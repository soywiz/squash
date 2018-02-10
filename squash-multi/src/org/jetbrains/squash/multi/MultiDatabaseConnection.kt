package org.jetbrains.squash.multi

import com.soywiz.io.ktor.client.util.*
import org.jetbrains.squash.connection.*
import org.jetbrains.squash.dialect.*

/**
 * [DatabaseConnection] that uses a pool of [DatabaseConnection] to handle several transactions concurrently.
 */
class MultiDatabaseConnection(val maxClients: Int = 64, val factory: () -> DatabaseConnection) : DatabaseConnection {
    private val defaultConnection = factory()
    private val pool = AsyncPool(maxClients) { if (it == 0) defaultConnection else factory() }
    private var closed = false

    override val dialect: SQLDialect get() = defaultConnection.dialect
    override val monitor: DatabaseConnectionMonitor get() = defaultConnection.monitor // @TODO: Probably we want a merged monitor

    override suspend fun createTransaction(): Transaction {
        if (closed) {
            throw IllegalStateException("DatabaseConnection already closed")
        }
        val connection = pool.alloc()
        //println("connection: $connection")
        val transaction = connection.createTransaction()
        var done = false
        return object : Transaction by transaction {
            override suspend fun commit() {
                transaction.commit()
                releaseOnce()
            }

            override suspend fun rollback() {
                transaction.rollback()
                releaseOnce()
            }

            private suspend fun releaseOnce() {
                if (done) return
                done = true
                if (closed) {
                    connection.close()
                } else {
                    pool.free(connection)
                }
            }
        }
    }

    override suspend fun close() {
        closed = true
        for (n in 0 until pool.availableFreed) {
            pool.alloc().close()
        }
    }
}

