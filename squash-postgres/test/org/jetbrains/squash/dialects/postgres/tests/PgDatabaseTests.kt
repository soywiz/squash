package org.jetbrains.squash.dialects.postgres.tests

import org.jetbrains.squash.definition.*
import org.jetbrains.squash.dialects.postgres.*
import org.jetbrains.squash.tests.*
import ru.yandex.qatools.embed.postgresql.*
import java.nio.file.*
import kotlin.test.*

val pgUrl = EmbeddedPostgres().start(EmbeddedPostgres.cachedRuntimeConfig(Paths.get("target/pg_embedded")))

class PgDatabaseTests : DatabaseTests {
    override val quote = "\""
    override val blobType = "BYTEA"
    override fun getIdColumnType(columnType: ColumnType): String = when (columnType) {
        is IntColumnType -> "SERIAL NOT NULL"
        is LongColumnType -> "BIGSERIAL NOT NULL"
        else -> fail("Unsupported column type $columnType")
    }

    override fun primaryKey(name: String, vararg column: String): String = ", CONSTRAINT PK_$name PRIMARY KEY (${column.joinToString()})"
    override fun autoPrimaryKey(table: String, column: String): String = primaryKey(table, column)

    override fun createConnection() = PgConnection.create(pgUrl)
    override suspend fun createTransaction() = createConnection().createTransaction().apply {
        executeStatement("SET search_path TO pg_temp")
    }
}