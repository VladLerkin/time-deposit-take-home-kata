package org.ikigaidigital.config

import org.assertj.core.api.Assertions.assertThat
import org.ikigaidigital.application.UpdateAllBalancesUseCase
import org.junit.jupiter.api.Test
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionCallback
import org.springframework.transaction.support.TransactionOperations

class TransactionalUpdateAllBalancesTest {

    @Test
    fun `should execute delegate inside transaction`() {
        val events = mutableListOf<String>()
        val recordingTransactionOperations = object : TransactionOperations {
            override fun <T : Any?> execute(action: TransactionCallback<T>): T? {
                events.add("tx-begin")
                val result = action.doInTransaction(SimpleTransactionStatus())
                events.add("tx-commit")
                return result
            }
        }
        val delegate = UpdateAllBalancesUseCase { events.add("delegate-executed") }
        val decorator = TransactionalUpdateAllBalances(delegate, recordingTransactionOperations)

        decorator.execute()

        assertThat(events).containsExactly("tx-begin", "delegate-executed", "tx-commit")
    }
}
