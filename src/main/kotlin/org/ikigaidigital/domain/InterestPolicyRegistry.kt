package org.ikigaidigital.domain

class InterestPolicyRegistry(
    private val policies: Map<String, InterestPolicy>
) {
    fun forPlan(planType: String): InterestPolicy = policies[planType] ?: NoInterestPolicy

    companion object {
        // Assumption: The "no interest for the first 30 days" rule applies to all existing plans.
        // Future plans can configure their own grace period via composable EligibilityRule instances.
        private val initialGracePeriodRule = MinAgeRule(minExclusiveDays = 30)

        fun default(): InterestPolicyRegistry = InterestPolicyRegistry(
            mapOf(
                "basic" to RuleBasedInterestPolicy(
                    annualRate = 0.01,
                    rules = listOf(initialGracePeriodRule)
                ),
                "student" to RuleBasedInterestPolicy(
                    annualRate = 0.03,
                    rules = listOf(initialGracePeriodRule, MaxAgeRule(maxInclusiveDays = 365))
                ),
                "premium" to RuleBasedInterestPolicy(
                    annualRate = 0.05,
                    rules = listOf(initialGracePeriodRule, MinAgeRule(minExclusiveDays = 45))
                )
            )
        )
    }
}
