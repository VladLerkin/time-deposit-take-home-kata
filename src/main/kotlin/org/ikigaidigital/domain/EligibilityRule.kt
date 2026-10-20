package org.ikigaidigital.domain

import org.ikigaidigital.TimeDeposit

fun interface EligibilityRule {
    fun isEligible(deposit: TimeDeposit): Boolean
}

class MinAgeRule(private val minExclusiveDays: Int) : EligibilityRule {
    override fun isEligible(deposit: TimeDeposit): Boolean = deposit.days > minExclusiveDays
}

class MaxAgeRule(private val maxInclusiveDays: Int) : EligibilityRule {
    override fun isEligible(deposit: TimeDeposit): Boolean = deposit.days <= maxInclusiveDays
}
