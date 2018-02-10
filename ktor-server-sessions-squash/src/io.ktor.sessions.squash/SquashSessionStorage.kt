package io.ktor.sessions.squash

import com.soywiz.io.ktor.client.util.*
import io.ktor.sessions.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.io.*
import org.jetbrains.squash.connection.*
import org.jetbrains.squash.dialect.*
import java.io.*
import java.time.*
import java.util.*

class SquashSessionStorage(
    val connection: DatabaseConnection,
    val tableName: String = "sessions",
    val ttl: Duration = Duration.ofDays(7L)
) : SimplifiedSessionStorage() {
    private val GC_STEPS = 10000
    private var totalCount = 0
    private var initialized = false

    suspend fun initOnce(): Unit {
        if (initialized) return
        initialized = true
        // CREATE TABLE sessions (id VARCHAR PRIMRAY KEY, expires DATETIME)
        // @TODO: Add additional index for expires column (except for big data like cassandra) where we would have
        // @TODO: to do this by scanning since secondary indices doesn't scale well.
        TODO()
    }

    override suspend fun read(id: String): ByteArray? {
        initOnce()
        // SELECT
        TODO()
    }

    override suspend fun write(id: String, data: ByteArray?) {
        initOnce()
        if (data == null) {
            // DELETE
            TODO()
        } else {
            // INSERT/UPDATE
            TODO()
        }

        if ((totalCount % GC_STEPS) == 0) {
            // Not required to wait for this
            launch {
                prune()
            }.start()
        }
    }

    suspend fun prune() {
        connection.transaction {
            Calendar.getInstance().apply { add(Calendar.SECOND, ttl.seconds.toInt()) }.time
            executeStatement(SQLStatement("DELETE FROM ? WHERE expires >= NOW();", listOf(SQLArgument(0, tableName))))
        }
    }
}

// @TODO: Ask: Could this be the default interface since Sessions are going to be small
// @TODO: Ask: and most of the time (de)serialized in-memory
abstract class SimplifiedSessionStorage : SessionStorage {
    abstract suspend fun read(id: String): ByteArray?
    abstract suspend fun write(id: String, data: ByteArray?): Unit

    override suspend fun invalidate(id: String) {
        write(id, null)
    }

    override suspend fun <R> read(id: String, consumer: suspend (ByteReadChannel) -> R): R {
        val data = read(id) ?: throw NoSuchElementException("Session $id not found")
        return consumer(ByteReadChannel(data))
    }

    override suspend fun write(id: String, provider: suspend (ByteWriteChannel) -> Unit) {
        return provider(reader(getCoroutineContext(), autoFlush = true) {
            val data = ByteArrayOutputStream()
            val temp = ByteArray(1024)
            while (!channel.isClosedForRead) {
                val read = channel.readAvailable(temp)
                if (read <= 0) break
                data.write(temp, 0, read)
            }
            write(id, data.toByteArray())
        }.channel)
    }
}
