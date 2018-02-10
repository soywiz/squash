package org.jetbrains.squash.connection

interface SuspendAutoCloseable {
    suspend fun close(): Unit
}

suspend inline fun <T : SuspendAutoCloseable, R> T.use(callback: (T) -> R): R {
    try {
        return callback(this)
    } finally {
        this.close()
    }
}