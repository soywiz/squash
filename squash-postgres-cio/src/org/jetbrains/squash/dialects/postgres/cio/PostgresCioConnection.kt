package org.jetbrains.squash.dialects.postgres.cio

import com.soywiz.io.ktor.client.postgre.*
import com.soywiz.io.ktor.client.util.*
import org.jetbrains.squash.connection.*
import org.jetbrains.squash.definition.*
import org.jetbrains.squash.dialect.*
import org.jetbrains.squash.results.*
import org.jetbrains.squash.schema.*
import org.jetbrains.squash.statements.*
import kotlin.reflect.*

class PostgresCioConnection(val db: PostgreClient) : DatabaseConnection {
    override val dialect: PgDialect =
        PgDialect
    override val monitor: PostgreCioConnectionMonitor = PostgreCioConnectionMonitor()
    override suspend fun createTransaction(): Transaction = PostgreCioTransaction(this).start()
    override suspend fun close(): Unit = run { db.close() }
}

// @TODO: Unify with JDBC
class PostgreCioConnectionMonitor : DatabaseConnectionMonitor {
    private val beforeCallbacks = mutableListOf<Transaction.(SQLStatement) -> Unit>()
    private val afterCallbacks = mutableListOf<Transaction.(SQLStatement, result: Any?) -> Unit>()

    override fun before(callback: Transaction.(statement: SQLStatement) -> Unit) {
        beforeCallbacks += callback
    }

    override fun after(callback: Transaction.(statement: SQLStatement, result: Any?) -> Unit) {
        afterCallbacks += callback
    }

    fun beforeStatement(transaction: PostgreCioTransaction, statement: SQLStatement) {
        for (callback in beforeCallbacks) callback(transaction, statement)
    }

    fun afterStatement(transaction: PostgreCioTransaction, statement: SQLStatement, result: Any?) {
        for (callback in afterCallbacks) callback(transaction, statement, result)
    }
}

class PostgreCioResultRow(val row: PostgreRow) : ResultRow {
    override fun columnValue(type: KClass<*>, columnName: String, tableName: String?): Any? {
        return when (type) {
            Int::class -> row.int(columnName)
            Long::class -> row.int(columnName)
            String::class -> row.string(columnName)
            ByteArray::class -> row.bytes(columnName)
        //Date::class -> row.date(columnName)
            else -> TODO()
        }
    }

    override fun columnValue(type: KClass<*>, index: Int): Any? {
        return when (type) {
            Int::class -> row.int(index)
            Long::class -> row.int(index)
            String::class -> row.string(index)
            ByteArray::class -> row.bytes(index)
        //Date::class -> row.date(index)
            else -> TODO()
        }
    }
}

class PostgreCioTransaction(override val connection: PostgresCioConnection) : Transaction {
    val db = connection.db

    override suspend fun executeStatement(statement: SQLStatement): Response {
        // @TODO: handle statement arguments
        // @TODO: proper preparing the query instead of quoting arguments
        var index = 0
        val sql = statement.sql.replace(Regex("\\?")) {
            val arg = statement.arguments[index++].value
            when (arg) {
                null -> "NULL"
                else -> arg.toString().postgreQuote()
            }
        }
        //println(sql)
        val result = db.query(sql).toList()
        return Response(result.map { PostgreCioResultRow(it) })
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
        db.query("START TRANSACTION;")
        //println("WARNING: PostgreCioTransaction.start not implemented!")
    }

    override suspend fun commit() {
        db.query("COMMIT;")
        //println("WARNING: PostgreCioTransaction.commit not implemented!")
    }

    override suspend fun rollback() {
        //println("WARNING: PostgreCioTransaction.rollback not implemented!")
        db.query("ROLLBACK;")
    }

    override suspend fun databaseSchema(): DatabaseSchema = PostgreCioDatabaseSchema(this)

    override fun createBlob(bytes: ByteArray): BinaryObject = PostgreBinaryObject(bytes)

    override suspend fun close() {
        commit()
    }
}

class PostgreBinaryObject(override val bytes: ByteArray) : BinaryObject {
    override fun toString(): String = "BLOB(${bytes.size}"
}

open class PostgreCioDatabaseSchema(final override val transaction: PostgreCioTransaction) :
    DatabaseSchemaBase(transaction) {
    val db get() = transaction.db
    override suspend fun tables(): Sequence<DatabaseSchema.SchemaTable> {
        return db.query("SHOW TABLES;").toList().map { PostgreSchemaTable(it.string(0) ?: "", this) }.asSequence()
    }

    class PostgreSchemaTable(override val name: String, private val schema: PostgreCioDatabaseSchema) :
        DatabaseSchema.SchemaTable {
        val db get() = schema.db

        override suspend fun columns(): Sequence<DatabaseSchema.SchemaColumn> {
            //show columns from mytable;
            return db.query("SHOW COLUMNS FROM ${name.postgreTableQuote()};").toList().map {
                PostgreSchemaColumn(it.string("Field")!!, it.string("Null") == "YES")
            }.asSequence()
        }

        class PostgreSchemaColumn(override val name: String, override val nullable: Boolean) :
            DatabaseSchema.SchemaColumn {
            override fun toString(): String = "[Postgre] Column: $name"
        }

        override fun toString(): String = "[Postgre] Table: $name"
    }
}

object PgDialect : BaseSQLDialect("Postgres") {
    override val definition: DefinitionSQLDialect = object : BaseDefinitionSQLDialect(this) {
        override fun columnTypeSQL(builder: SQLStatementBuilder, column: Column<*>) {
            if (column.hasProperty<AutoIncrementProperty>()) {
                require(!column.hasProperty<NullableProperty>()) { "Column ${column.name} cannot be both AUTOINCREMENT and NULL" }
                val type = column.type
                val autoincrement = when (type) {
                    is IntColumnType -> "SERIAL"
                    is LongColumnType -> "BIGSERIAL"
                    else -> error("AutoIncrement column for '$type' is not supported by $this")
                }
                builder.append(autoincrement)
            } else super.columnTypeSQL(builder, column)
        }

        override fun columnAutoIncrementProperty(builder: SQLStatementBuilder, property: AutoIncrementProperty?) {
            // do nothing, we already handled AutoIncrementProperty as SERIAL
        }

        override fun columnPropertiesSQL(builder: SQLStatementBuilder, column: Column<*>) {
            super.columnPropertiesSQL(builder, column)
        }

        override fun columnTypeSQL(builder: SQLStatementBuilder, type: ColumnType) {
            when (type) {
                is UUIDColumnType -> builder.append("UUID")
                is BlobColumnType -> builder.append("BYTEA")
                is BinaryColumnType -> builder.append("BYTEA")
                is DateTimeColumnType -> builder.append("TIMESTAMP")
                else -> super.columnTypeSQL(builder, type)
            }
        }
    }
}


fun String.postgreEscape(): String {
    var out = ""
    for (c in this) {
        when (c) {
            '\u0000' -> out += "\\0"
            '\'' -> out += "\\'"
            '\"' -> out += "\\\""
            '\b' -> out += "\\b"
            '\n' -> out += "\\n"
            '\r' -> out += "\\r"
            '\t' -> out += "\\t"
            '\u0026' -> out += "\\Z"
            '\\' -> out += "\\\\"
            '%' -> out += "\\%"
            '_' -> out += "\\_"
            '`' -> out += "\\`"
            else -> out += c
        }
    }
    return out
}

fun String.postgreQuote(): String = "'${this.postgreEscape()}'"
fun String.postgreTableQuote(): String = "`${this.postgreEscape()}`"
