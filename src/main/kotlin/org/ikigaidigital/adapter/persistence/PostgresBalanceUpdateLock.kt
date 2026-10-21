package org.ikigaidigital.adapter.persistence

import jakarta.persistence.EntityManager
import org.ikigaidigital.application.BalanceUpdateLock
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

// A single transaction-scoped advisory lock instead of SELECT ... FOR UPDATE on every row:
// one lock regardless of table size, released automatically on commit or rollback.
@Component
class PostgresBalanceUpdateLock(
    private val entityManager: EntityManager
) : BalanceUpdateLock {

    @Transactional(propagation = Propagation.MANDATORY)
    override fun acquire() {
        // Wrapped in count(*) because pg_advisory_xact_lock returns void, which Hibernate cannot map
        entityManager.createNativeQuery("select count(*) from (select pg_advisory_xact_lock(:key)) as l")
            .setParameter("key", LOCK_KEY)
            .singleResult
    }

    companion object {
        const val LOCK_KEY = 7_281_001L
    }
}
