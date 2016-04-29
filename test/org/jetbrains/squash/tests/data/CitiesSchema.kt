package org.jetbrains.squash.tests.data

import org.jetbrains.squash.*

object CitiesSchema {
    object Cities : Table() {
        val id = integer("id").autoIncrement() // PKColumn<Int>
        val name = varchar("name", 50) // Column<String>
    }

    object Citizens : Table() {
        val id = varchar("id", 10).primaryKey() // PKColumn<String>
        val name = varchar("name", length = 50) // Column<String>
        val cityId = reference(Cities.id, "city_id").nullable() // Column<Int?>
    }

    object UserData : Table() {
        val user_id = reference(Citizens.id)
        val comment = varchar("comment", 30)
        val value = integer("value")
    }

    enum class E {
        ONE,
        TWO,
        THREE
    }

    object Misc : Table() {
        val n = integer("n")
        val nn = integer("nn").nullable()

        val d = date("d")
        val dn = date("dn").nullable()

        val t = datetime("t")
        val tn = datetime("tn").nullable()

        val e = enumeration<E>("e")
        val en = enumeration<E>("en").nullable()

        val s = varchar("s", 100)
        val sn = varchar("sn", 100).nullable()

        val dc = decimal("dc", 12, 2)
        val dcn = decimal("dcn", 12, 2).nullable()

    }
}
