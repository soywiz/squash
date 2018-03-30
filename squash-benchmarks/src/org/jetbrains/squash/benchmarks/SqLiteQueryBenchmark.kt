package org.jetbrains.squash.benchmarks

import kotlinx.coroutines.experimental.*
import org.jetbrains.squash.dialects.sqlite.*

open class SqLiteQueryBenchmark : QueryBenchmark() {
    override fun createTransaction() = runBlocking { SqLiteConnection.createMemoryConnection().createTransaction() }
}