package org.ikigaidigital.adapter.rest

import org.ikigaidigital.TimeDeposit
import org.ikigaidigital.application.GetAllTimeDepositsUseCase
import org.ikigaidigital.domain.TimeDepositView
import org.ikigaidigital.domain.Withdrawal
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDate

@WebMvcTest(TimeDepositController::class)
class TimeDepositControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var getAllTimeDepositsUseCase: GetAllTimeDepositsUseCase

    @Test
    fun `GET time-deposits should return 200 OK with time deposits and nested withdrawals`() {
        `when`(getAllTimeDepositsUseCase.execute()).thenReturn(
            listOf(
                TimeDepositView(
                    deposit = TimeDeposit(id = 1, planType = "basic", balance = 1201.00, days = 45),
                    withdrawals = listOf(
                        Withdrawal(
                            id = 10,
                            timeDepositId = 1,
                            amount = BigDecimal("100.00"),
                            date = LocalDate.of(2026, 1, 15)
                        )
                    )
                ),
                TimeDepositView(
                    deposit = TimeDeposit(id = 2, planType = "premium", balance = 6000.00, days = 60),
                    withdrawals = emptyList()
                )
            )
        )

        mockMvc.perform(get("/time-deposits").accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").value(1))
            .andExpect(jsonPath("$[0].planType").value("basic"))
            .andExpect(jsonPath("$[0].balance").value(1201.0))
            .andExpect(jsonPath("$[0].days").value(45))
            .andExpect(jsonPath("$[0].withdrawals.length()").value(1))
            .andExpect(jsonPath("$[0].withdrawals[0].id").value(10))
            .andExpect(jsonPath("$[0].withdrawals[0].amount").value(100.0))
            .andExpect(jsonPath("$[0].withdrawals[0].date").value("2026-01-15"))
            .andExpect(jsonPath("$[0].withdrawals[0].timeDepositId").doesNotExist())
            .andExpect(jsonPath("$[1].id").value(2))
            .andExpect(jsonPath("$[1].planType").value("premium"))
            .andExpect(jsonPath("$[1].balance").value(6000.0))
            .andExpect(jsonPath("$[1].days").value(60))
            .andExpect(jsonPath("$[1].withdrawals").isEmpty)
    }
}
