package org.jetbrains.squash.dialects.mysql.cio

import com.soywiz.io.ktor.client.mysql.*
import com.soywiz.io.ktor.client.util.*
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
    override val dialect: MySqlDialect = MySqlDialect
    override val monitor: MysqlCioConnectionMonitor = MysqlCioConnectionMonitor()
    override suspend fun createTransaction(): Transaction = MysqlCioTransaction(this).start()
    override suspend fun close(): Unit = run { mysql.close() }
}

// @TODO: Unify with JDBC
class MysqlCioConnectionMonitor : DatabaseConnectionMonitor {
    private val beforeCallbacks = mutableListOf<Transaction.(SQLStatement) -> Unit>()
    private val afterCallbacks = mutableListOf<Transaction.(SQLStatement, result: Any?) -> Unit>()

    override fun before(callback: Transaction.(statement: SQLStatement) -> Unit) {
        beforeCallbacks += callback
    }

    override fun after(callback: Transaction.(statement: SQLStatement, result: Any?) -> Unit) {
        afterCallbacks += callback
    }

    fun beforeStatement(transaction: MysqlCioTransaction, statement: SQLStatement) {
        for (callback in beforeCallbacks) callback(transaction, statement)
    }

    fun afterStatement(transaction: MysqlCioTransaction, statement: SQLStatement, result: Any?) {
        for (callback in afterCallbacks) callback(transaction, statement, result)
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
        // @TODO: proper preparing the query instead of quoting arguments
        var index = 0
        val sql = statement.sql.replace(Regex("\\?")) {
            val arg = statement.arguments[index++].value
            when (arg) {
                null -> "NULL"
                else -> arg.toString().mysqlQuote()
            }
        }
        //println(sql)
        val result = mysql.query(sql).toList()
        return Response(result.map { MysqlCioResultRow(it) })
    }

    override suspend fun <T> executeStatement(statement: Statement<T>): T {
        val statementSQL = connection.dialect.statementSQL(statement)
        val returnColumn: Column<*>? =
            if (statement is InsertValuesStatement<*, *>) statement.generatedKeyColumn else null
        connection.monitor.beforeStatement(this, statementSQL)
        val untypedResult = executeStatement(statementSQL)
        val result = untypedResult as T
        //val result = null
        //val preparedStatement = jdbcTransaction.prepareStatement(statementSQL, returnColumn)
        //preparedStatement.execute()
        //val result = resultFor(preparedStatement, statement)
        connection.monitor.afterStatement(this, statementSQL, result)
        return result
    }

    suspend fun start() = this.apply {
        mysql.query("START TRANSACTION;")
        //println("WARNING: MysqlCioTransaction.start not implemented!")
    }

    override suspend fun commit() {
        mysql.query("COMMIT;")
        //println("WARNING: MysqlCioTransaction.commit not implemented!")
    }

    override suspend fun rollback() {
        //println("WARNING: MysqlCioTransaction.rollback not implemented!")
        mysql.query("ROLLBACK;")
    }

    override suspend fun databaseSchema(): DatabaseSchema = MysqlCioDatabaseSchema(this)

    override fun createBlob(bytes: ByteArray): BinaryObject = JDBCBinaryObject(bytes)

    override suspend fun close() {
        commit()
    }
}

open class MysqlCioDatabaseSchema(final override val transaction: MysqlCioTransaction) :
    DatabaseSchemaBase(transaction) {
    val mysql get() = transaction.mysql
    override suspend fun tables(): Sequence<DatabaseSchema.SchemaTable> {
        return mysql.query("SHOW TABLES;").toList().map { MysqlSchemaTable(it.string(0) ?: "", this) }.asSequence()
    }

    class MysqlSchemaTable(override val name: String, private val schema: MysqlCioDatabaseSchema) :
        DatabaseSchema.SchemaTable {
        val mysql get() = schema.mysql

        override suspend fun columns(): Sequence<DatabaseSchema.SchemaColumn> {
            //show columns from mytable;
            return mysql.query("SHOW COLUMNS FROM ${name.mysqlTableQuote()};").toList().map {
                MysqlSchemaColumn(it.string("Field")!!, it.string("Null") == "YES")
            }.asSequence()
        }

        class MysqlSchemaColumn(override val name: String, override val nullable: Boolean) :
            DatabaseSchema.SchemaColumn {
            override fun toString(): String = "[MYSQL] Column: $name"
        }

        override fun toString(): String = "[MYSQL] Table: $name"
    }
}
