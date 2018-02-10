import com.soywiz.io.ktor.client.mysql.*
import kotlinx.coroutines.experimental.*
import org.jetbrains.squash.connection.*
import org.jetbrains.squash.definition.*
import org.jetbrains.squash.dialects.mysql.cio.*
import org.jetbrains.squash.multi.*
import org.jetbrains.squash.query.*
import org.jetbrains.squash.results.*
import org.jetbrains.squash.schema.*
import org.jetbrains.squash.statements.*

object MultiDatabaseConnectionSpike {
    object Cities : TableDefinition() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", 50)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val connection = MultiDatabaseConnection(maxClients = 8) { MysqlCioConnection(Mysql(database = "mytest")) }
            val jobs = arrayListOf<Job>()
            for (n in 0 until 8) {
                val job = launch {
                    connection.transaction {
                        databaseSchema().create(Cities)

                        insertInto(Cities).values {
                            it[name] = "Valencia"
                        }.execute()

                        println("Wait 1 second... for job transaction $n")

                        delay(1000)

                        for (row in from(Cities).select(Cities.name).execute()) {
                            println(row[Cities.name])
                        }

                        //for (row in executeStatement("SELECT NOW();")) {
                        //    println(row.get<Date>("NOW()"))
                        //}
                    }
                }
                job.start()
                jobs += job
            }
            for (job in jobs) {
                job.join()
            }
        }
    }
}