package org.ikigaidigital.domain

import org.ikigaidigital.TimeDeposit

data class TimeDepositView(
    val deposit: TimeDeposit,
    val withdrawals: List<Withdrawal>
)
