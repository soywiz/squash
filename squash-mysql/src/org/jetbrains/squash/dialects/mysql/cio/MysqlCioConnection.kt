package org.jetbrains.squash.dialects.mysql.cio

import com.soywiz.io.ktor.client.mysql.*
import com.soywiz.io.ktor.client.util.*
import kotlinx.coroutines.experimental.*
import org.jetbrains.squash.connection.*
import org.jetbrains.squash.definition.*
import org.jetbrains.squash.dialect.*
import org.jetbrains.squash.dialects.mysql.*
import org.jetbrains.squash.drivers.*
import org.jetbrains.squash.results.*
import org.jetbrains.squash.schema.*
import org.jetbrains.squash.statements.*
import java.util.*
import kotlin.reflect.*

class MysqlCioConnection(val mysql: Mysql) : DatabaseConnection {
    override val dialect: SQLDialect = MySqlDialect
    override val monitor: DatabaseConnectionMonitor = JDBCDatabaseConnectionMonitor()
    override fun createTransaction(): Transaction = MysqlCioTransaction(this)
    override fun close(): Unit = run { launch { mysql.close() }.start() }
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

class MysqlCioResultRow(val row: MysqlRow) : ResultRow {
    override fun columnValue(type: KClass<*>, columnName: String, tableName: String?): Any? {
        return when (type) {
            Int::class -> row.int(columnName)
            Long::class -> row.int(columnName)
            String::class -> row.string(columnName)
            ByteArray::class -> row.byteArray(columnName)
            Date::class -> row.date(columnName)
            else -> TODO()
        }
    }

    override fun columnValue(type: KClass<*>, index: Int): Any? {
        return when (type) {
            Int::class -> row.int(index)
            Long::class -> row.int(index)
            String::class -> row.string(index)
            ByteArray::class -> row.byteArray(index)
            Date::class -> row.date(index)
            else -> TODO()
        }
    }
}

class MysqlCioTransaction(override val connection: MysqlCioConnection) : Transaction {
    val mysql = connection.mysql

    override suspend fun executeStatement(statement: SQLStatement): Response {
        // @TODO: handle statement arguments
        val result = mysql.query(statement.sql).toList()
        return Response(result.map { MysqlCioResultRow(it) })
    }

    override suspend fun <T> executeStatement(statement: Statement<T>): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override suspend fun commit() {
        println("WARNING: MysqlCioTransaction.commit not implemented!")
    }

    override suspend fun rollback() {
        println("WARNING: MysqlCioTransaction.rollback not implemented!")
    }

    override suspend fun databaseSchema(): DatabaseSchema = MysqlCioDatabaseSchema()

    override fun createBlob(bytes: ByteArray): BinaryObject = JDBCBinaryObject(bytes)

    override fun close() {
        launch { rollback() }.start()
    }
}

class MysqlCioDatabaseSchema : DatabaseSchema {
    suspend override fun tables(): Sequence<DatabaseSchema.SchemaTable> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun create(tables: List<TableDefinition>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun createStatements(tables: List<TableDefinition>): List<SQLStatement> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    suspend override fun validate(tables: List<Table>): List<DatabaseSchema.DatabaseSchemaValidationItem> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
