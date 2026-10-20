package org.ikigaidigital.application

import org.ikigaidigital.TimeDeposit

interface TimeDepositRepository {
    fun findAll(): List<TimeDeposit>
    fun updateBalances(deposits: List<TimeDeposit>)
}
