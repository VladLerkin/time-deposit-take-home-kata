package org.ikigaidigital.adapter.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

// Assumption: IDs are not auto-generated because the API only reads and updates existing deposits.
// No @OneToMany or cascade on withdrawals to guarantee balance updates can never orphan or delete withdrawals.
@Entity
@Table(name = "time_deposits")
class TimeDepositEntity(
    @Id
    @Column(name = "id", nullable = false)
    var id: Int,

    @Column(name = "plan_type", nullable = false, length = 50)
    var planType: String,

    @Column(name = "days", nullable = false)
    var days: Int,

    @Column(name = "balance", nullable = false, precision = 19, scale = 2)
    var balance: BigDecimal
)
