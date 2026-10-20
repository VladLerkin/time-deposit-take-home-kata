package org.ikigaidigital

import org.assertj.core.api.Assertions.assertThat
import org.ikigaidigital.application.GetAllTimeDepositsUseCase
import org.ikigaidigital.application.UpdateAllBalancesUseCase
import org.ikigaidigital.config.TransactionalUpdateAllBalances
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@AutoConfigureMockMvc
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

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun contextLoads() {
        assertThat(calculator).isNotNull()
        assertThat(updateAllBalancesUseCase).isInstanceOf(TransactionalUpdateAllBalances::class.java)
        assertThat(getAllTimeDepositsUseCase).isNotNull()
    }

    @Test
    fun `should serve static OpenAPI spec and Swagger UI pointing to it`() {
        mockMvc.perform(get("/openapi.yaml"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/swagger-ui/index.html"))
            .andExpect(status().isOk)

        mockMvc.perform(get("/v3/api-docs/swagger-config"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.url").value("/openapi.yaml"))
    }
}
