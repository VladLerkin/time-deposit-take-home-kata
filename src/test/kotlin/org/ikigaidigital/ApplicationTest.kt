package org.ikigaidigital

import org.assertj.core.api.Assertions.assertThat
import org.ikigaidigital.application.GetAllTimeDepositsUseCase
import org.ikigaidigital.application.UpdateAllBalancesUseCase
import org.ikigaidigital.config.TransactionalUpdateAllBalances
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Testcontainers
class ApplicationTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired
    private lateinit var calculator: TimeDepositCalculator

    @Autowired
    private lateinit var updateAllBalancesUseCase: UpdateAllBalancesUseCase

    @Autowired
    private lateinit var getAllTimeDepositsUseCase: GetAllTimeDepositsUseCase

    @Test
    fun contextLoads() {
        assertThat(calculator).isNotNull()
        assertThat(updateAllBalancesUseCase).isInstanceOf(TransactionalUpdateAllBalances::class.java)
        assertThat(getAllTimeDepositsUseCase).isNotNull()
    }
}
