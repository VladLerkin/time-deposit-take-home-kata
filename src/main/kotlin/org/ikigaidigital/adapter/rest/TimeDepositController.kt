package org.ikigaidigital.adapter.rest

import org.ikigaidigital.adapter.rest.api.TimeDepositsApi
import org.ikigaidigital.adapter.rest.dto.TimeDepositDto
import org.ikigaidigital.application.GetAllTimeDepositsUseCase
import org.ikigaidigital.application.UpdateAllBalancesUseCase
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController

@RestController
class TimeDepositController(
    private val getAllTimeDepositsUseCase: GetAllTimeDepositsUseCase,
    private val updateAllBalancesUseCase: UpdateAllBalancesUseCase
) : TimeDepositsApi {

    override fun getAllTimeDeposits(): ResponseEntity<List<TimeDepositDto>> {
        val dtos = getAllTimeDepositsUseCase.execute().map(TimeDepositRestMapper::toDto)
        return ResponseEntity.ok(dtos)
    }

    override fun updateAllBalances(): ResponseEntity<Unit> {
        updateAllBalancesUseCase.execute()
        return ResponseEntity.noContent().build()
    }
}
