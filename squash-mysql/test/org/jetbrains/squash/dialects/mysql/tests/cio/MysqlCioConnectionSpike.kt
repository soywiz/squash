package org.jetbrains.squash.dialects.mysql.tests.cio

import com.soywiz.io.ktor.client.mysql.*
import kotlinx.coroutines.experimental.*
import org.jetbrains.squash.connection.*
import org.jetbrains.squash.definition.*
import org.jetbrains.squash.dialects.mysql.cio.*
import org.jetbrains.squash.statements.*

object MysqlCioConnectionTestsSpike {
    object Cities : TableDefinition() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", 50)
    }


    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val connection = MysqlCioConnection(Mysql())
            connection.transaction {
                insertInto(Cities).values {
                    it[name] = "Valencia"
                }.execute()

                //for (row in executeStatement("SELECT NOW();")) {
                //    println(row.get<Date>("NOW()"))
                //}
            }
        }
    }
}