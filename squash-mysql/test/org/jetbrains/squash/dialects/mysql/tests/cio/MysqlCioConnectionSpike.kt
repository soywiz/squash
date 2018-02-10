package org.jetbrains.squash.dialects.mysql.tests.cio

import com.soywiz.io.ktor.client.mysql.*
import kotlinx.coroutines.experimental.*
import org.jetbrains.squash.dialects.mysql.cio.*
import org.jetbrains.squash.results.*

object MysqlCioConnectionTestsSpike {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val connection = MysqlCioConnection(Mysql())
            val transaction = connection.createTransaction()
            transaction.apply {
                for (row in executeStatement("SELECT 1;")) {
                    println(row.get<Int>(0))
                }
                commit()
            }
        }
    }
}