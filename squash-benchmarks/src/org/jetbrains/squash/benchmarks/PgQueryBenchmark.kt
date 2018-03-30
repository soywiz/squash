package org.jetbrains.squash.benchmarks

import com.opentable.db.postgres.embedded.*
import kotlinx.coroutines.experimental.*
import org.jetbrains.squash.dialects.postgres.*
import org.openjdk.jmh.annotations.*

open class PgQueryBenchmark : QueryBenchmark() {
    lateinit var pg : EmbeddedPostgres

    @Setup
    fun startPostgres() {
        pg = EmbeddedPostgres.start()
    }

    override fun createTransaction() = runBlocking { PgConnection.create("localhost:${pg.port}/", "postgres").createTransaction() }
}