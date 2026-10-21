package org.ikigaidigital.adapter.persistence

import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.SessionFactory
import org.ikigaidigital.TimeDeposit
import org.ikigaidigital.domain.Withdrawal
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.math.BigDecimal
import java.time.LocalDate

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TimeDepositPersistenceAdapter::class)
@Testcontainers
class TimeDepositPersistenceAdapterTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired
    private lateinit var adapter: TimeDepositPersistenceAdapter

    @Autowired
    private lateinit var depositJpaRepository: SpringDataTimeDepositRepository

    @Autowired
    private lateinit var withdrawalJpaRepository: SpringDataWithdrawalRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        withdrawalJpaRepository.deleteAll()
        depositJpaRepository.deleteAll()
    }

    @Test
    fun `updateBalances should update balances in same transaction without touching withdrawals`() {
        depositJpaRepository.saveAll(
            listOf(
                TimeDepositEntity(id = 1, planType = "basic", days = 45, balance = BigDecimal("1200.00")),
                TimeDepositEntity(id = 2, planType = "student", days = 100, balance = BigDecimal("2400.00"))
            )
        )
        withdrawalJpaRepository.saveAll(
            listOf(
                WithdrawalEntity(id = 10, timeDepositId = 1, amount = BigDecimal("150.00"), date = LocalDate.of(2026, 1, 10)),
                WithdrawalEntity(id = 11, timeDepositId = 1, amount = BigDecimal("50.00"), date = LocalDate.of(2026, 2, 12))
            )
        )

        adapter.updateBalances(
            listOf(
                TimeDeposit(id = 1, planType = "basic", balance = 1201.00, days = 45),
                TimeDeposit(id = 2, planType = "student", balance = 2406.00, days = 100)
            )
        )

        // Re-read within the same @DataJpaTest transaction to verify 1st-level cache was cleared and flushed
        val views = adapter.findAllWithWithdrawals()
        assertThat(views).hasSize(2)
        assertThat(views[0].deposit).isEqualTo(TimeDeposit(id = 1, planType = "basic", balance = 1201.00, days = 45))
        assertThat(views[0].withdrawals).containsExactly(
            Withdrawal(id = 10, timeDepositId = 1, amount = BigDecimal("150.00"), date = LocalDate.of(2026, 1, 10)),
            Withdrawal(id = 11, timeDepositId = 1, amount = BigDecimal("50.00"), date = LocalDate.of(2026, 2, 12))
        )
        assertThat(views[1].deposit).isEqualTo(TimeDeposit(id = 2, planType = "student", balance = 2406.00, days = 100))
        assertThat(views[1].withdrawals).isEmpty()
    }

    @Test
    fun `findAllWithWithdrawals should use two queries regardless of deposit count (no N+1)`() {
        depositJpaRepository.saveAll(
            (1..5).map { id ->
                TimeDepositEntity(id = id, planType = "basic", days = 60, balance = BigDecimal("1000.00"))
            }
        )
        withdrawalJpaRepository.saveAll(
            (1..5).map { id ->
                WithdrawalEntity(id = id, timeDepositId = id, amount = BigDecimal("10.00"), date = LocalDate.of(2026, 1, id))
            }
        )
        entityManager.flush()
        entityManager.clear()

        val statistics = entityManager.entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.isStatisticsEnabled = true
        statistics.clear()

        val views = adapter.findAllWithWithdrawals()

        assertThat(views).hasSize(5)
        assertThat(views).allSatisfy { assertThat(it.withdrawals).hasSize(1) }
        assertThat(statistics.prepareStatementCount).isEqualTo(2)
    }

    @Test
    fun `should map IEEE 754 floating-point tail 1000_1000000000001 to 1000_10 in database`() {
        val numeric = PersistenceMapper.toNumericBalance(1000.1000000000001)
        assertThat(numeric).isEqualByComparingTo(BigDecimal("1000.10"))

        depositJpaRepository.save(
            TimeDepositEntity(id = 1, planType = "basic", days = 60, balance = BigDecimal("1000.00"))
        )

        adapter.updateBalances(
            listOf(TimeDeposit(id = 1, planType = "basic", balance = 1000.1000000000001, days = 60))
        )

        val persistedEntity = depositJpaRepository.findById(1).orElseThrow()
        assertThat(persistedEntity.balance).isEqualTo(BigDecimal("1000.10"))
    }
}
