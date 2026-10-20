package org.ikigaidigital.application

import org.assertj.core.api.Assertions.assertThat
import org.ikigaidigital.TimeDeposit
import org.ikigaidigital.domain.TimeDepositView
import org.ikigaidigital.domain.Withdrawal
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class GetAllTimeDepositsServiceTest {

    @Test
    fun `should return all time deposits with their withdrawals`() {
        val expectedViews = listOf(
            TimeDepositView(
                deposit = TimeDeposit(id = 1, planType = "basic", balance = 1200.0, days = 45),
                withdrawals = listOf(
                    Withdrawal(id = 10, timeDepositId = 1, amount = BigDecimal("100.00"), date = LocalDate.of(2026, 1, 10))
                )
            ),
            TimeDepositView(
                deposit = TimeDeposit(id = 2, planType = "premium", balance = 5000.0, days = 60),
                withdrawals = emptyList()
            )
        )
        val useCase: GetAllTimeDepositsUseCase = GetAllTimeDepositsService(
            readRepository = TimeDepositReadRepository { expectedViews }
        )

        val result = useCase.execute()

        assertThat(result).containsExactlyElementsOf(expectedViews)
    }
}
