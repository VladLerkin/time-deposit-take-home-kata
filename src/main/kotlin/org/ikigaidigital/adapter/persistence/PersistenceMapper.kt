package org.ikigaidigital.adapter.persistence

import org.ikigaidigital.TimeDeposit
import org.ikigaidigital.domain.Withdrawal
import java.math.BigDecimal
import java.math.RoundingMode

object PersistenceMapper {

    fun toDomain(entity: TimeDepositEntity): TimeDeposit = TimeDeposit(
        id = entity.id,
        planType = entity.planType,
        balance = entity.balance.toDouble(),
        days = entity.days
    )

    fun toDomain(entity: WithdrawalEntity): Withdrawal = Withdrawal(
        id = entity.id,
        timeDepositId = entity.timeDepositId,
        amount = entity.amount,
        date = entity.date
    )

    // Convert Double -> BigDecimal via valueOf (not BigDecimal(double)) to eliminate IEEE 754 floating-point tails.
    fun toNumericBalance(balance: Double): BigDecimal =
        BigDecimal.valueOf(balance).setScale(2, RoundingMode.HALF_UP)
}
