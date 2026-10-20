package org.ikigaidigital.adapter.rest

import org.ikigaidigital.adapter.rest.dto.TimeDepositDto
import org.ikigaidigital.adapter.rest.dto.WithdrawalDto
import org.ikigaidigital.domain.TimeDepositView
import org.ikigaidigital.domain.Withdrawal
import java.math.BigDecimal
import java.math.RoundingMode

object TimeDepositRestMapper {

    fun toDto(view: TimeDepositView): TimeDepositDto = TimeDepositDto(
        id = view.deposit.id,
        planType = view.deposit.planType,
        balance = BigDecimal.valueOf(view.deposit.balance).setScale(2, RoundingMode.HALF_UP).toDouble(),
        days = view.deposit.days,
        withdrawals = view.withdrawals.map(::toDto)
    )

    // Assumption: timeDepositId is omitted in WithdrawalDto because the parent-child relationship is expressed by nesting.
    fun toDto(withdrawal: Withdrawal): WithdrawalDto = WithdrawalDto(
        id = withdrawal.id,
        amount = withdrawal.amount,
        date = withdrawal.date
    )
}
