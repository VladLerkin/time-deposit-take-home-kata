package org.ikigaidigital.adapter.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal

interface SpringDataTimeDepositRepository : JpaRepository<TimeDepositEntity, Int> {

    fun findAllByOrderByIdAsc(): List<TimeDepositEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update TimeDepositEntity e set e.balance = :balance where e.id = :id")
    fun updateBalanceById(
        @Param("id") id: Int,
        @Param("balance") balance: BigDecimal
    ): Int
}

interface SpringDataWithdrawalRepository : JpaRepository<WithdrawalEntity, Int> {
    fun findAllByOrderByIdAsc(): List<WithdrawalEntity>
}
