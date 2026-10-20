package org.ikigaidigital.domain

import org.assertj.core.api.Assertions.assertThat
import org.ikigaidigital.TimeDeposit
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class TimeDepositViewTest {

    @Test
    fun `should hold deposit and associated withdrawals`() {
        val deposit = TimeDeposit(id = 1, planType = "basic", balance = 1000.0, days = 45)
        val withdrawal = Withdrawal(
            id = 10,
            timeDepositId = 1,
            amount = BigDecimal("250.00"),
            date = LocalDate.of(2026, 1, 15)
        )

        val view = TimeDepositView(deposit = deposit, withdrawals = listOf(withdrawal))

        assertThat(view.deposit).isEqualTo(deposit)
        assertThat(view.withdrawals).containsExactly(withdrawal)
    }
}
