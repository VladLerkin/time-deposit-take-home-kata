package org.ikigaidigital.application

// Serializes concurrent balance update runs. Without it, two runs could read the same balances
// and both write back their result, so the outcome would depend on timing.
// Must be acquired inside the transaction that performs the update and is released when it ends.
fun interface BalanceUpdateLock {
    fun acquire()
}
