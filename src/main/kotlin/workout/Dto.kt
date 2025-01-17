package com.example.workout

import com.google.gson.annotations.SerializedName

data class WorkoutDto(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String,
    @SerializedName("date") val date: String,
    @SerializedName("exercises") val exercises: List<ExerciseDto>
)

data class ExerciseDto(
    @SerializedName("id") val id: String?,
    @SerializedName("title") val title: String,
    @SerializedName("describing_photo") val describingPhoto: String?,
    @SerializedName("approaches") val approaches: List<ApproachDto>
)

data class ApproachDto(
    @SerializedName("id") val id: String?,
    @SerializedName("workout_id") val workoutId: String,
    @SerializedName("exercise_id") val exerciseId: String,
    @SerializedName("repetitions") val repetitions: Int,
    @SerializedName("weight") val weight: Int
)

data class SummaryDto(
    @SerializedName("date") val date: String,
    @SerializedName("tonnage") val tonnage: Float
)