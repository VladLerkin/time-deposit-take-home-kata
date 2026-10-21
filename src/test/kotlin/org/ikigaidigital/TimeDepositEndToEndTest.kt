package org.ikigaidigital

import org.assertj.core.api.Assertions.assertThat
import org.ikigaidigital.application.UpdateAllBalancesUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class TimeDepositEndToEndTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var updateAllBalancesUseCase: UpdateAllBalancesUseCase

    @BeforeEach
    fun setUpFixtures() {
        jdbcTemplate.execute("DELETE FROM withdrawals")
        jdbcTemplate.execute("DELETE FROM time_deposits")

        jdbcTemplate.execute(
            """
            INSERT INTO time_deposits (id, plan_type, days, balance) VALUES
                (1, 'basic', 45, 1200.00),
                (2, 'student', 180, 2400.00),
                (3, 'premium', 60, 6000.00),
                (4, 'basic', 30, 1000.00)
            """.trimIndent()
        )

        jdbcTemplate.execute(
            """
            INSERT INTO withdrawals (id, time_deposit_id, amount, date) VALUES
                (10, 1, 100.00, '2026-01-15'),
                (20, 2, 50.00, '2026-02-01'),
                (21, 2, 75.50, '2026-02-10')
            """.trimIndent()
        )
    }

    @Test
    fun `should update all balances via POST and return updated deposits with preserved withdrawals via GET`() {
        mockMvc.perform(post("/time-deposits/balance-updates"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/time-deposits").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(4))
            // Deposit 1: basic, 45 days, 1200.00 -> 1201.00, 1 withdrawal preserved
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].planType").value("basic"))
            .andExpect(jsonPath("$[0].days").value(45))
            .andExpect(jsonPath("$[0].balance").value(1201.0))
            .andExpect(jsonPath("$[0].withdrawals.length()").value(1))
            .andExpect(jsonPath("$[0].withdrawals[0].id").value(10))
            .andExpect(jsonPath("$[0].withdrawals[0].amount").value(100.0))
            .andExpect(jsonPath("$[0].withdrawals[0].date").value("2026-01-15"))
            // Deposit 2: student, 180 days, 2400.00 -> 2406.00, 2 withdrawals preserved
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].planType").value("student"))
            .andExpect(jsonPath("$[1].days").value(180))
            .andExpect(jsonPath("$[1].balance").value(2406.0))
            .andExpect(jsonPath("$[1].withdrawals.length()").value(2))
            .andExpect(jsonPath("$[1].withdrawals[0].id").value(20))
            .andExpect(jsonPath("$[1].withdrawals[0].amount").value(50.0))
            .andExpect(jsonPath("$[1].withdrawals[0].date").value("2026-02-01"))
            .andExpect(jsonPath("$[1].withdrawals[1].id").value(21))
            .andExpect(jsonPath("$[1].withdrawals[1].amount").value(75.5))
            .andExpect(jsonPath("$[1].withdrawals[1].date").value("2026-02-10"))
            // Deposit 3: premium, 60 days, 6000.00 -> 6025.00, 0 withdrawals
            .andExpect(jsonPath("$[2].id").value(3))
            .andExpect(jsonPath("$[2].planType").value("premium"))
            .andExpect(jsonPath("$[2].days").value(60))
            .andExpect(jsonPath("$[2].balance").value(6025.0))
            .andExpect(jsonPath("$[2].withdrawals").isEmpty)
            // Deposit 4: basic, 30 days (grace period), 1000.00 -> 1000.00
            .andExpect(jsonPath("$[3].id").value(4))
            .andExpect(jsonPath("$[3].planType").value("basic"))
            .andExpect(jsonPath("$[3].days").value(30))
            .andExpect(jsonPath("$[3].balance").value(1000.0))
            .andExpect(jsonPath("$[3].withdrawals").isEmpty)
    }

    @Test
    fun `concurrent balance updates should give the same result as sequential ones`() {
        val runs = 8
        val expected = listOf(
            TimeDeposit(id = 1, planType = "basic", balance = 1200.00, days = 45),
            TimeDeposit(id = 2, planType = "student", balance = 2400.00, days = 180),
            TimeDeposit(id = 3, planType = "premium", balance = 6000.00, days = 60),
            TimeDeposit(id = 4, planType = "basic", balance = 1000.00, days = 30)
        )
        // Sequential reference: each run is followed by rounding to NUMERIC(19, 2), as the persistence adapter does
        repeat(runs) {
            TimeDepositCalculator().updateBalance(expected)
            expected.forEach { it.balance = BigDecimal.valueOf(it.balance).setScale(2, RoundingMode.HALF_UP).toDouble() }
        }

        val startGate = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(runs)
        try {
            val futures = (1..runs).map {
                executor.submit {
                    startGate.await()
                    updateAllBalancesUseCase.execute()
                }
            }
            startGate.countDown()
            futures.forEach { it.get(30, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }

        val actual = jdbcTemplate.query("SELECT id, balance FROM time_deposits ORDER BY id") { rs, _ ->
            rs.getInt("id") to rs.getBigDecimal("balance")
        }
        assertThat(actual).containsExactlyElementsOf(
            expected.map { it.id to BigDecimal.valueOf(it.balance).setScale(2, RoundingMode.HALF_UP) }
        )
    }
}
