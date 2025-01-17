package com.example.workout.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Approaches : Table() {
    val id = uuid("id").autoGenerate()
    val workoutId = uuid("workout_id").references(Workouts.id, onDelete = ReferenceOption.CASCADE)
    val exerciseId = uuid("exercise_id").references(Exercises.id, onDelete = ReferenceOption.CASCADE)
    val weight = integer("weight").default(10)
    val repetitions = integer("repetitions").default(1)
}