package org.jetbrains.squash.benchmarks

import kotlinx.coroutines.experimental.*
import org.jetbrains.squash.dialects.h2.*

open class H2QueryBenchmark : QueryBenchmark() {
    override fun createTransaction() = runBlocking { H2Connection.createMemoryConnection().createTransaction() }
}