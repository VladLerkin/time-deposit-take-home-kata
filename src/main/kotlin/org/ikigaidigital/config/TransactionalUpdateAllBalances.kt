package org.ikigaidigital.config

import org.ikigaidigital.application.UpdateAllBalancesUseCase
import org.springframework.transaction.support.TransactionOperations

class TransactionalUpdateAllBalances(
    private val delegate: UpdateAllBalancesUseCase,
    private val transactionOperations: TransactionOperations
) : UpdateAllBalancesUseCase {

    override fun execute() {
        transactionOperations.executeWithoutResult {
            delegate.execute()
        }
    }
}
