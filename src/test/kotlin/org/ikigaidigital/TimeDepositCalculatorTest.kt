package org.ikigaidigital

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

class TimeDepositCalculatorTest {

    private val calculator = TimeDepositCalculator()

    @ParameterizedTest(name = "plan={0}, days={1} -> expectedBalance={2}")
    @CsvSource(
        // basic plan (1% annual -> +1.00/month on 1200.00 when days > 30)
        "basic, 0, 1200.00",
        "basic, 29, 1200.00",
        "basic, 30, 1200.00",
        "basic, 31, 1201.00",
        "basic, 45, 1201.00",
        "basic, 46, 1201.00",
        "basic, 365, 1201.00",
        "basic, 366, 1201.00",
        "basic, 1000, 1201.00",

        // student plan (3% annual -> +3.00/month on 1200.00 when 30 < days < 366)
        "student, 0, 1200.00",
        "student, 29, 1200.00",
        "student, 30, 1200.00",
        "student, 31, 1203.00",
        "student, 45, 1203.00",
        "student, 46, 1203.00",
        "student, 365, 1203.00",
        "student, 366, 1200.00",
        "student, 1000, 1200.00",

        // premium plan (5% annual -> +5.00/month on 1200.00 when days > 45)
        "premium, 0, 1200.00",
        "premium, 29, 1200.00",
        "premium, 30, 1200.00",
        "premium, 31, 1200.00",
        "premium, 45, 1200.00",
        "premium, 46, 1205.00",
        "premium, 365, 1205.00",
        "premium, 366, 1205.00",
        "premium, 1000, 1205.00"
    )
    fun `should calculate monthly interest across plan types and boundary days`(
        planType: String,
        days: Int,
        expectedBalance: Double
    ) {
        val deposit = TimeDeposit(id = 1, planType = planType, balance = 1200.00, days = days)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(expectedBalance)
    }

    @ParameterizedTest(name = "unknown or mismatched-case planType=''{0}'' accrues 0 interest")
    @ValueSource(strings = ["unknown", "gold", "", "Basic", "BASIC", "Student", "Premium"])
    fun `should apply zero interest for unknown or case-mismatched plan types`(planType: String) {
        val deposit = TimeDeposit(id = 1, planType = planType, balance = 1000.00, days = 100)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(1000.00)
    }

    @ParameterizedTest(name = "initial={0}, plan={1} -> expected={2}")
    @CsvSource(
        // 6.00 * 0.01 / 12 = 0.005 -> HALF_UP rounds to 0.01
        "6.00, basic, 6.01",
        // 4.00 * 0.01 / 12 = 0.00333... -> HALF_UP rounds to 0.00
        "4.00, basic, 4.00",
        // 1234567.00 * 0.01 / 12 = 1028.8058333... -> HALF_UP rounds to 1028.81
        "1234567.00, basic, 1235595.81",
        // 100.00 * 0.05 / 12 = 0.416666... -> HALF_UP rounds to 0.42
        "100.00, premium, 100.42"
    )
    fun `should round monthly interest to 2 decimal places using HALF_UP`(
        initialBalance: Double,
        planType: String,
        expectedBalance: Double
    ) {
        val deposit = TimeDeposit(id = 1, planType = planType, balance = initialBalance, days = 60)

        calculator.updateBalance(listOf(deposit))

        assertThat(deposit.balance).isEqualTo(expectedBalance)
    }

    @Test
    fun `should handle empty list as no-op`() {
        val emptyList = emptyList<TimeDeposit>()

        calculator.updateBalance(emptyList)

        assertThat(emptyList).isEmpty()
    }

    @Test
    fun `should mutate multiple deposits in-place within the same list`() {
        val first = TimeDeposit(id = 1, planType = "basic", balance = 1200.00, days = 45)
        val second = TimeDeposit(id = 2, planType = "student", balance = 2400.00, days = 100)
        val third = TimeDeposit(id = 3, planType = "premium", balance = 1200.00, days = 40)
        val fourth = TimeDeposit(id = 4, planType = "premium", balance = 1200.00, days = 60)
        val deposits = listOf(first, second, third, fourth)

        calculator.updateBalance(deposits)

        assertThat(deposits[0]).isSameAs(first)
        assertThat(first.balance).isEqualTo(1201.00)
        assertThat(second.balance).isEqualTo(2406.00)
        assertThat(third.balance).isEqualTo(1200.00)
        assertThat(fourth.balance).isEqualTo(1205.00)
    }
}
