package org.ikigaidigital.application

import org.ikigaidigital.TimeDepositCalculator

fun interface UpdateAllBalancesUseCase {
    fun execute()
}

class UpdateAllBalancesService(
    private val repository: TimeDepositRepository,
    private val lock: BalanceUpdateLock,
    private val calculator: TimeDepositCalculator = TimeDepositCalculator()
) : UpdateAllBalancesUseCase {

    override fun execute() {
        lock.acquire()
        val deposits = repository.findAll()
        calculator.updateBalance(deposits)
        repository.updateBalances(deposits)
    }
}
