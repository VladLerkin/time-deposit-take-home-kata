package org.ikigaidigital.domain

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.ikigaidigital.TimeDeposit
import org.junit.jupiter.api.Test

class InterestPolicyTest {

    @Test
    fun `MinAgeRule should only allow deposits strictly older than minExclusiveDays`() {
        val rule = MinAgeRule(minExclusiveDays = 30)

        assertThat(rule.isEligible(TimeDeposit(1, "basic", 1000.0, 29))).isFalse()
        assertThat(rule.isEligible(TimeDeposit(1, "basic", 1000.0, 30))).isFalse()
        assertThat(rule.isEligible(TimeDeposit(1, "basic", 1000.0, 31))).isTrue()
    }

    @Test
    fun `MaxAgeRule should only allow deposits up to maxInclusiveDays`() {
        val rule = MaxAgeRule(maxInclusiveDays = 365)

        assertThat(rule.isEligible(TimeDeposit(1, "student", 1000.0, 365))).isTrue()
        assertThat(rule.isEligible(TimeDeposit(1, "student", 1000.0, 366))).isFalse()
    }

    @Test
    fun `RuleBasedInterestPolicy should apply annual rate divided by 12 only when all rules pass`() {
        val policy = RuleBasedInterestPolicy(
            annualRate = 0.03,
            rules = listOf(MinAgeRule(30), MaxAgeRule(365))
        )

        assertThat(policy.monthlyInterest(TimeDeposit(1, "student", 1200.0, 30))).isEqualTo(0.0)
        assertThat(policy.monthlyInterest(TimeDeposit(1, "student", 1200.0, 31))).isCloseTo(3.0, within(1e-9))
        assertThat(policy.monthlyInterest(TimeDeposit(1, "student", 1200.0, 365))).isCloseTo(3.0, within(1e-9))
        assertThat(policy.monthlyInterest(TimeDeposit(1, "student", 1200.0, 366))).isEqualTo(0.0)
    }

    @Test
    fun `InterestPolicyRegistry default should provide basic, student, premium policies and fallback to NoInterest`() {
        val registry = InterestPolicyRegistry.default()

        val basic = registry.forPlan("basic")
        assertThat(basic.monthlyInterest(TimeDeposit(1, "basic", 1200.0, 30))).isEqualTo(0.0)
        assertThat(basic.monthlyInterest(TimeDeposit(1, "basic", 1200.0, 31))).isCloseTo(1.0, within(1e-9))

        val student = registry.forPlan("student")
        assertThat(student.monthlyInterest(TimeDeposit(1, "student", 1200.0, 30))).isEqualTo(0.0)
        assertThat(student.monthlyInterest(TimeDeposit(1, "student", 1200.0, 31))).isCloseTo(3.0, within(1e-9))
        assertThat(student.monthlyInterest(TimeDeposit(1, "student", 1200.0, 366))).isEqualTo(0.0)

        val premium = registry.forPlan("premium")
        assertThat(premium.monthlyInterest(TimeDeposit(1, "premium", 1200.0, 45))).isEqualTo(0.0)
        assertThat(premium.monthlyInterest(TimeDeposit(1, "premium", 1200.0, 46))).isCloseTo(5.0, within(1e-9))

        val unknown = registry.forPlan("unknown")
        assertThat(unknown).isSameAs(NoInterestPolicy)
        assertThat(unknown.monthlyInterest(TimeDeposit(1, "unknown", 1200.0, 100))).isEqualTo(0.0)
    }
}
