package com.example.common

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import java.time.Instant

class InstantColumnType : ColumnType<Instant>() {
    override fun sqlType(): String = "BIGINT"

    override fun valueFromDB(value: Any): Instant? = when (value) {
        is Long -> Instant.ofEpochMilli(value)
        else -> null
    }

    override fun notNullValueToDB(value: Instant): Any = value.toEpochMilli()

    override fun valueToString(value: Instant?): String = value?.toString() ?: super.valueToString(value)
}

fun Table.instant(name: String): Column<Instant> = registerColumn(name, InstantColumnType())