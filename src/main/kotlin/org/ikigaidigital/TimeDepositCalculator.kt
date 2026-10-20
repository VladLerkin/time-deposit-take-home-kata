package org.ikigaidigital

import org.ikigaidigital.domain.InterestPolicyRegistry
import java.math.BigDecimal
import java.math.RoundingMode

class TimeDepositCalculator(
    private val policies: InterestPolicyRegistry = InterestPolicyRegistry.default()
) {
    fun updateBalance(xs: List<TimeDeposit>) {
        for (deposit in xs) {
            val interest = policies.forPlan(deposit.planType).monthlyInterest(deposit)
            deposit.balance += roundInterest(interest)
        }
    }

    private fun roundInterest(interest: Double): Double =
        BigDecimal(interest).setScale(2, RoundingMode.HALF_UP).toDouble()
}
