package org.ikigaidigital.domain

import org.ikigaidigital.TimeDeposit

fun interface InterestPolicy {
    fun monthlyInterest(deposit: TimeDeposit): Double
}

object NoInterestPolicy : InterestPolicy {
    override fun monthlyInterest(deposit: TimeDeposit): Double = 0.0
}

class RuleBasedInterestPolicy(
    private val annualRate: Double,
    private val rules: List<EligibilityRule>
) : InterestPolicy {
    override fun monthlyInterest(deposit: TimeDeposit): Double {
        if (rules.all { it.isEligible(deposit) }) {
            return deposit.balance * annualRate / MONTHS_IN_YEAR
        }
        return 0.0
    }

    companion object {
        private const val MONTHS_IN_YEAR = 12
    }
}
