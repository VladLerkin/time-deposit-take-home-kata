package org.ikigaidigital.application

import org.ikigaidigital.domain.TimeDepositView

fun interface GetAllTimeDepositsUseCase {
    fun execute(): List<TimeDepositView>
}

class GetAllTimeDepositsService(
    private val readRepository: TimeDepositReadRepository
) : GetAllTimeDepositsUseCase {

    override fun execute(): List<TimeDepositView> =
        readRepository.findAllWithWithdrawals()
}
