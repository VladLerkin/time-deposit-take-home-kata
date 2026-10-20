package org.ikigaidigital.config

import org.ikigaidigital.TimeDepositCalculator
import org.ikigaidigital.application.GetAllTimeDepositsService
import org.ikigaidigital.application.GetAllTimeDepositsUseCase
import org.ikigaidigital.application.TimeDepositReadRepository
import org.ikigaidigital.application.TimeDepositRepository
import org.ikigaidigital.application.UpdateAllBalancesService
import org.ikigaidigital.application.UpdateAllBalancesUseCase
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.transaction.support.TransactionOperations

@Configuration
class UseCaseConfig {

    @Bean
    fun timeDepositCalculator(): TimeDepositCalculator = TimeDepositCalculator()

    @Bean
    fun updateAllBalancesUseCase(
        repository: TimeDepositRepository,
        calculator: TimeDepositCalculator,
        transactionOperations: TransactionOperations
    ): UpdateAllBalancesUseCase = TransactionalUpdateAllBalances(
        delegate = UpdateAllBalancesService(repository, calculator),
        transactionOperations = transactionOperations
    )

    @Bean
    fun getAllTimeDepositsUseCase(
        readRepository: TimeDepositReadRepository
    ): GetAllTimeDepositsUseCase = GetAllTimeDepositsService(readRepository)
}
