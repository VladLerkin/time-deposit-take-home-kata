package org.ikigaidigital.application

import org.assertj.core.api.Assertions.assertThat
import org.ikigaidigital.TimeDeposit
import org.ikigaidigital.TimeDepositCalculator
import org.ikigaidigital.domain.Withdrawal
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class UpdateAllBalancesServiceTest {

    @Test
    fun `should update balances of all time deposits without altering withdrawals`() {
        val initialDeposits = listOf(
            TimeDeposit(id = 1, planType = "basic", balance = 1200.00, days = 45),
            TimeDeposit(id = 2, planType = "student", balance = 2400.00, days = 100),
            TimeDeposit(id = 3, planType = "premium", balance = 1200.00, days = 30)
        )
        val initialWithdrawals = listOf(
            Withdrawal(id = 10, timeDepositId = 1, amount = BigDecimal("100.00"), date = LocalDate.of(2026, 1, 10)),
            Withdrawal(id = 11, timeDepositId = 2, amount = BigDecimal("50.00"), date = LocalDate.of(2026, 2, 15))
        )
        val fakeStore = InMemoryFakeTimeDepositStore(initialDeposits, initialWithdrawals)
        val useCase: UpdateAllBalancesUseCase = UpdateAllBalancesService(
            repository = fakeStore,
            lock = {},
            calculator = TimeDepositCalculator()
        )

        useCase.execute()

        val updated = fakeStore.findAll()
        assertThat(updated).containsExactly(
            TimeDeposit(id = 1, planType = "basic", balance = 1201.00, days = 45),
            TimeDeposit(id = 2, planType = "student", balance = 2406.00, days = 100),
            TimeDeposit(id = 3, planType = "premium", balance = 1200.00, days = 30)
        )
        assertThat(fakeStore.withdrawals()).containsExactlyElementsOf(initialWithdrawals)
    }

    @Test
    fun `should acquire the balance update lock before reading deposits`() {
        val events = mutableListOf<String>()
        val repository = object : TimeDepositRepository {
            override fun findAll(): List<TimeDeposit> {
                events.add("findAll")
                return emptyList()
            }

            override fun updateBalances(deposits: List<TimeDeposit>) {
                events.add("updateBalances")
            }
        }
        val useCase = UpdateAllBalancesService(
            repository = repository,
            lock = { events.add("lock") }
        )

        useCase.execute()

        assertThat(events).containsExactly("lock", "findAll", "updateBalances")
    }

    private class InMemoryFakeTimeDepositStore(
        deposits: List<TimeDeposit>,
        withdrawals: List<Withdrawal>
    ) : TimeDepositRepository {
        private val depositMap = deposits.associate { it.id to it.copy() }.toMutableMap()
        private val withdrawalList = withdrawals.toMutableList()

        override fun findAll(): List<TimeDeposit> =
            depositMap.values.map { it.copy() }

        override fun updateBalances(deposits: List<TimeDeposit>) {
            for (deposit in deposits) {
                val existing = depositMap[deposit.id] ?: continue
                depositMap[deposit.id] = existing.copy(balance = deposit.balance)
            }
        }

        fun withdrawals(): List<Withdrawal> = withdrawalList.toList()
    }
}
