package org.ikigaidigital.adapter.persistence

import org.ikigaidigital.TimeDeposit
import org.ikigaidigital.application.TimeDepositReadRepository
import org.ikigaidigital.application.TimeDepositRepository
import org.ikigaidigital.domain.TimeDepositView
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class TimeDepositPersistenceAdapter(
    private val depositJpaRepository: SpringDataTimeDepositRepository,
    private val withdrawalJpaRepository: SpringDataWithdrawalRepository
) : TimeDepositRepository, TimeDepositReadRepository {

    @Transactional(readOnly = true)
    override fun findAll(): List<TimeDeposit> =
        depositJpaRepository.findAllByOrderByIdAsc().map(PersistenceMapper::toDomain)

    @Transactional
    override fun updateBalances(deposits: List<TimeDeposit>) {
        for (deposit in deposits) {
            depositJpaRepository.updateBalanceById(
                id = deposit.id,
                balance = PersistenceMapper.toNumericBalance(deposit.balance)
            )
        }
    }

    @Transactional(readOnly = true)
    override fun findAllWithWithdrawals(): List<TimeDepositView> {
        val deposits = depositJpaRepository.findAllByOrderByIdAsc()
        val withdrawalsByDepositId = withdrawalJpaRepository.findAllByOrderByIdAsc()
            .map(PersistenceMapper::toDomain)
            .groupBy { it.timeDepositId }

        return deposits.map { entity ->
            TimeDepositView(
                deposit = PersistenceMapper.toDomain(entity),
                withdrawals = withdrawalsByDepositId[entity.id].orEmpty()
            )
        }
    }
}
