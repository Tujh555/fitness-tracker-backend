package com.example.workout.database

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object WorkoutCrossRef : Table() {
    val workoutId = uuid("workout_id").references(Workouts.id, onDelete = ReferenceOption.CASCADE)
    val exerciseId = uuid("exercise_id").references(Exercises.id, onDelete = ReferenceOption.CASCADE)

    override val primaryKey: PrimaryKey = PrimaryKey(workoutId, exerciseId)
}