package com.example.workout.database

import com.example.auth.database.Users
import org.jetbrains.exposed.sql.Table

object Workouts : Table() {
    val id = uuid("id").autoGenerate()
    val title = text("title")
    val date = long("date")
    val userId = integer("user_id").references(Users.id)

    override val primaryKey = PrimaryKey(id)
}
