package org.ikigaidigital.application

import org.ikigaidigital.domain.TimeDepositView

fun interface TimeDepositReadRepository {
    fun findAllWithWithdrawals(): List<TimeDepositView>
}
