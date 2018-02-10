package org.jetbrains.squash.dialects.mysql.cio

import org.jetbrains.squash.connection.*
import org.jetbrains.squash.dialect.*
import org.jetbrains.squash.dialects.mysql.*
import org.jetbrains.squash.drivers.*
import org.jetbrains.squash.results.*
import org.jetbrains.squash.schema.*
import org.jetbrains.squash.statements.*

class MysqlCioConnection : DatabaseConnection {
    override val dialect: SQLDialect = MySqlDialect
    override val monitor: DatabaseConnectionMonitor = JDBCDatabaseConnectionMonitor()

    override fun createTransaction(): Transaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}

class MysqlCioConnectionMonitor : DatabaseConnectionMonitor {
    private val beforeCallbacks = mutableListOf<Transaction.(SQLStatement) -> Unit>()
    private val afterCallbacks = mutableListOf<Transaction.(SQLStatement, result: Any?) -> Unit>()

    override fun before(callback: Transaction.(statement: SQLStatement) -> Unit) {
        beforeCallbacks += callback
    }

    override fun after(callback: Transaction.(statement: SQLStatement, result: Any?) -> Unit) {
        afterCallbacks += callback
    }
}

/*
class MysqlCioTransaction : Transaction {
    override val connection: DatabaseConnection
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    suspend override fun executeStatement(sql: String): Response {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun executeStatement(statement: SQLStatement): Response {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun commit() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun rollback() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun <T> executeStatement(statement: Statement<T>): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun databaseSchema(): DatabaseSchema {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createBlob(bytes: ByteArray): BinaryObject {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
*/
