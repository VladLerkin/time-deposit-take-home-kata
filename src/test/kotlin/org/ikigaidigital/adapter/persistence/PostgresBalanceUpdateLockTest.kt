package org.ikigaidigital.adapter.persistence

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.transaction.IllegalTransactionStateException
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresBalanceUpdateLock::class)
@Testcontainers
// Each thread needs its own transaction, so the test method itself must not run in one
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PostgresBalanceUpdateLockTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:16-alpine")
    }

    @Autowired
    private lateinit var lock: PostgresBalanceUpdateLock

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Test
    fun `second transaction should wait until the first one releases the lock`() {
        val transactionTemplate = TransactionTemplate(transactionManager)
        val firstAcquired = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)

        val first = CompletableFuture.runAsync {
            transactionTemplate.executeWithoutResult {
                lock.acquire()
                firstAcquired.countDown()
                releaseFirst.await(10, TimeUnit.SECONDS)
            }
        }
        assertThat(firstAcquired.await(10, TimeUnit.SECONDS)).isTrue()

        val second = CompletableFuture.runAsync {
            transactionTemplate.executeWithoutResult { lock.acquire() }
        }

        Thread.sleep(500)
        assertThat(second).isNotDone()

        releaseFirst.countDown()
        first.get(10, TimeUnit.SECONDS)
        second.get(10, TimeUnit.SECONDS)
    }

    @Test
    fun `should refuse to acquire the lock outside a transaction`() {
        assertThatThrownBy { lock.acquire() }
            .isInstanceOf(IllegalTransactionStateException::class.java)
    }
}
