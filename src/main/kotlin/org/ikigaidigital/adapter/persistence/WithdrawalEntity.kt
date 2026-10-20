package org.ikigaidigital.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate

@Entity
@Table(name = "withdrawals")
class WithdrawalEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: Int,

    @Column(name = "time_deposit_id", nullable = false)
    var timeDepositId: Int,

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    var amount: BigDecimal,

    @Column(name = "date", nullable = false)
    var date: LocalDate
)
