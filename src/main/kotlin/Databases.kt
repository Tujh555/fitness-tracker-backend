package com.example

import com.example.auth.database.Tokens
import com.example.auth.database.Users
import com.example.workout.database.Approaches
import com.example.workout.database.Exercises
import com.example.workout.database.Workouts
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases(): Database {
    val database = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;",
        driver = "org.h2.Driver"
    )
    transaction(database) {
        SchemaUtils.create(Users, Tokens, Workouts, Exercises, Approaches)
    }

    return database
}