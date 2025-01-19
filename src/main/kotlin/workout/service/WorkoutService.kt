package com.example.workout.service

import com.example.common.Response
import com.example.common.query
import com.example.profile.getUserByToken
import com.example.workout.ApproachDto
import com.example.workout.ExerciseDto
import com.example.workout.SummaryDto
import com.example.workout.WorkoutDto
import com.example.workout.database.Approaches
import com.example.workout.database.Exercises
import com.example.workout.database.WorkoutCrossRef
import com.example.workout.database.Workouts
import io.ktor.http.content.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import net.coobird.thumbnailator.Thumbnails
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import java.io.File
import java.nio.file.Paths
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.math.E

class WorkoutService {
    suspend fun getWorkouts(token: String): List<WorkoutDto> {
        val user = getUserByToken(token) ?: return emptyList()

        return query {
            (Workouts innerJoin WorkoutCrossRef innerJoin Exercises innerJoin Approaches)
                .select(
                    Workouts.id,
                    Workouts.title,
                    Workouts.date,
                    Exercises.id,
                    Exercises.title,
                    Exercises.describingPhoto,
                    Approaches.id,
                    Approaches.weight,
                    Approaches.repetitions,
                    Approaches.workoutId,
                    Approaches.exerciseId
                )
                .where { Workouts.userId eq user.id }
                .map { row ->
                    val workoutId = row[Workouts.id].toString()
                    val workoutTitle = row[Workouts.title]
                    val workoutDate = row[Workouts.date]
                    val exerciseId = row[Exercises.id].toString()
                    val exerciseTitle = row[Exercises.title]
                    val exerciseDescribingPhoto = row[Exercises.describingPhoto]
                    val approachId = row[Approaches.id].toString()
                    val approachWeight = row[Approaches.weight]
                    val approachRepetitions = row[Approaches.repetitions]
                    val approachWorkoutId = row[Approaches.workoutId].toString()
                    val approachExerciseId = row[Approaches.exerciseId].toString()
                    WorkoutData(
                        workoutId = workoutId,
                        workoutTitle = workoutTitle,
                        workoutDate = workoutDate,
                        exerciseId = exerciseId,
                        exerciseTitle = exerciseTitle,
                        exerciseDescribingPhoto = exerciseDescribingPhoto,
                        approachId = approachId,
                        approachWeight = approachWeight,
                        approachRepetitions = approachRepetitions,
                        approachWorkoutId = approachWorkoutId,
                        approachExerciseId = approachExerciseId
                    )
                }
                .groupBy { it.workoutId }
                .mapValues { (_, workoutData) ->
                    val firstWorkoutData = workoutData.first()
                    WorkoutDto(
                        id = firstWorkoutData.workoutId,
                        title = firstWorkoutData.workoutTitle,
                        date = firstWorkoutData.workoutDate.toString(),
                        exercises = workoutData
                            .groupBy { it.exerciseId }
                            .mapValues { (_, exerciseData) ->
                                val firstExerciseData = exerciseData.first()
                                ExerciseDto(
                                    id = firstExerciseData.exerciseId,
                                    title = firstExerciseData.exerciseTitle,
                                    describingPhoto = firstExerciseData.exerciseDescribingPhoto,
                                    approaches = exerciseData.map { approachData ->
                                        ApproachDto(
                                            id = approachData.approachId,
                                            workoutId = approachData.approachWorkoutId,
                                            exerciseId = approachData.approachExerciseId,
                                            repetitions = approachData.approachRepetitions,
                                            weight = approachData.approachWeight
                                        )
                                    }
                                )
                            }.values.toList()
                    )
                }.values.toList()
        }
    }

    suspend fun getSummary(token: String): List<SummaryDto> {
        val user = getUserByToken(token) ?: return emptyList()

        return query {
            (Workouts innerJoin Approaches)
                .select(Workouts.date, Approaches.weight, Approaches.repetitions, Workouts.id)
                .where { Workouts.userId eq user.id }
                .map { row ->
                    SummaryData(
                        workoutId = row[Workouts.id],
                        workoutDate = row[Workouts.date],
                        weight = row[Approaches.weight],
                        repetitions = row[Approaches.repetitions]
                    )
                }
                .groupBy {
                    val workoutDateTime = LocalDateTime.ofInstant(it.workoutDate, ZoneOffset.UTC)
                    workoutDateTime.year to workoutDateTime.monthValue
                }
                .mapValues { (_, workoutData) ->
                    workoutData.sumOf { workout ->
                        workout.weight * workout.repetitions
                    }.toFloat()
                }
                .map { (key, tonnage) ->
                    SummaryDto(
                        date = "${key.first}-${key.second}",
                        tonnage = tonnage
                    )
                }
        }
    }

    suspend fun editWorkout(workoutDto: WorkoutDto): Response<WorkoutDto> {
        val workoutId = workoutDto.id?.let(UUID::fromString) ?: UUID.randomUUID()
        val existing = Workouts
            .selectAll()
            .where { Workouts.id eq workoutId }
            .firstOrNull()

        if (existing == null) {
            return Response.Error(404, "Not found")
        }

        val dto = query {
            Workouts.update({ Workouts.id eq workoutId }) {
                it[title] = workoutDto.title
                it[date] = Instant.parse(workoutDto.date)
            }

            workoutDto.exercises.forEach { exerciseDto ->
                val exerciseId = UUID.fromString(exerciseDto.id)
                Exercises.update({ Exercises.id eq exerciseId }) {
                    it[title] = workoutDto.title
                    if (exerciseDto.describingPhoto != null) {
                        it[describingPhoto] = exerciseDto.describingPhoto
                    }
                }

                exerciseDto.approaches.forEach { approachDto ->
                    Approaches.deleteWhere { Approaches.workoutId eq workoutId and (Approaches.exerciseId eq exerciseId) }

                    Approaches.insert {
                        it[Approaches.workoutId] = workoutId
                        it[Approaches.exerciseId] = exerciseId
                        it[weight] = approachDto.weight
                        it[repetitions] = approachDto.repetitions
                    }
                }
            }

            workoutDto.copy(
                exercises = workoutDto.exercises.map { exercise ->
                    exercise.copy(
                        approaches = Approaches
                            .selectAll()
                            .where { Approaches.workoutId eq workoutId and (Approaches.exerciseId eq UUID.fromString(exercise.id)) }
                            .map {
                                ApproachDto(
                                    id = it[Approaches.id].toString(),
                                    workoutId = it[Approaches.workoutId].toString(),
                                    exerciseId = it[Approaches.exerciseId].toString(),
                                    repetitions = it[Approaches.repetitions],
                                    weight = it[Approaches.repetitions]
                                )
                            }
                    )
                }
            )
        }

        return Response.Success(dto)
    }

    suspend fun createWorkout(token: String, workoutDto: WorkoutDto): Response<WorkoutDto> {
        val user = getUserByToken(token) ?: return Response.Error(401, "Not authorized")

        val dto = query {
            val workout = Workouts.insert {
                it[userId] = user.id
                it[title] = workoutDto.title
                it[date] = Instant.parse(workoutDto.date)
            }

            val exercises = workoutDto.exercises.map { exerciseDto ->
                val ex = Exercises.insert {
                    it[title] = exerciseDto.title
                    it[describingPhoto] = exerciseDto.describingPhoto
                    it[userId] = user.id
                }
                WorkoutCrossRef.insert {
                    it[workoutId] = workout[Workouts.id]
                    it[exerciseId] = ex[Exercises.id]
                }
                ex
            }

            val approaches = workoutDto.exercises
                .mapIndexed { index, exerciseDto ->
                    exerciseDto to exercises[index]
                }
                .associate { (dto, statement) ->
                    val id = statement[Exercises.id].toString()

                    val approaches = dto.approaches.map { approachDto ->
                        Approaches.insert {
                            it[workoutId] = workout[Workouts.id]
                            it[exerciseId] = statement[Exercises.id]
                            it[weight] = approachDto.weight
                            it[repetitions] = approachDto.repetitions
                        }
                    }

                    id to approaches
                }

            WorkoutDto(
                id = workout[Workouts.id].toString(),
                title = workout[Workouts.title],
                date = workout[Workouts.date].toString(),
                exercises = exercises.map { exercise ->
                    val id = exercise[Exercises.id].toString()
                    ExerciseDto(
                        id = id,
                        title = exercise[Exercises.title],
                        describingPhoto = exercise[Exercises.describingPhoto],
                        approaches = approaches
                            .getOrDefault(id, emptyList())
                            .map { approach ->
                                ApproachDto(
                                    id = approach[Approaches.id].toString(),
                                    workoutId = approach[Approaches.workoutId].toString(),
                                    exerciseId = approach[Approaches.exerciseId].toString(),
                                    repetitions = approach[Approaches.repetitions],
                                    weight = approach[Approaches.weight]
                                )
                            }
                    )
                }
            )
        }

        return Response.Success(dto)
    }

    suspend fun getExercises(token: String): List<ExerciseDto> = query {
        val user = getUserByToken(token) ?: return@query emptyList()
        Exercises.selectAll().where { Exercises.userId eq user.id }.map { row ->
            ExerciseDto(
                id = row[Exercises.id].toString(),
                title = row[Exercises.title],
                describingPhoto = row[Exercises.describingPhoto],
                approaches = emptyList()
            )
        }
    }

    suspend fun createOrEditExercise(
        id: UUID?,
        photo: PartData.FileItem?,
        title: String,
        token: String
    ): Response<ExerciseDto> = query {
        val user = getUserByToken(token) ?: return@query Response.Error(401, "Вы не авторизованы")
        val existing = id?.let { Exercises.selectAll().where { Exercises.id eq it }.firstOrNull() }
        val savedPhoto = photo?.let { savePhoto(it) }

        if (existing == null) {
            val statement = Exercises.insert {
                it[Exercises.title] = title
                it[describingPhoto] = savedPhoto
                it[userId] = user.id
            }

            return@query Response.Success(
                data = ExerciseDto(
                    id = statement[Exercises.id].toString(),
                    title = statement[Exercises.title],
                    describingPhoto = statement[Exercises.describingPhoto],
                    approaches = emptyList()
                )
            )
        }

        Exercises.update({ Exercises.id eq id }) {
            it[Exercises.title] = title
            if (savedPhoto != null) {
                it[describingPhoto] = savedPhoto
            }
        }
        val exercise = Exercises
            .selectAll()
            .where { Exercises.id eq id }
            .first()

        Response.Success(
            data = ExerciseDto(
                id = exercise[Exercises.id].toString(),
                title = exercise[Exercises.title],
                describingPhoto = exercise[Exercises.describingPhoto],
                approaches = emptyList()
            )
        )
    }

    private suspend fun savePhoto(photo: PartData.FileItem): String {
        val fileExtension = photo.originalFileName
            .orEmpty()
            .substringAfterLast(".", "")
            .ifEmpty { "jpg" }
        val uniqueName = "${UUID.randomUUID()}.$fileExtension"
        val exercisesDir = "exercises"
        val pathToSave = Paths.get(exercisesDir, uniqueName).toFile()
        val writeChannel = pathToSave.writeChannel()
        photo.provider().copyAndClose(writeChannel)
        writeChannel.flushAndClose()
        val thumbnailPath = "${exercisesDir}/${UUID.randomUUID()}_thumbnail.$fileExtension"
        Thumbnails.of(pathToSave).size(300, 300).toFile(File(thumbnailPath))
        return "http://0.0.0.0:8080/$thumbnailPath"
    }

    private data class SummaryData(
        val workoutId: UUID,
        val workoutDate: Instant,
        val weight: Int,
        val repetitions: Int
    )

    private data class WorkoutData(
        val workoutId: String,
        val workoutTitle: String,
        val workoutDate: Instant,
        val exerciseId: String,
        val exerciseTitle: String,
        val exerciseDescribingPhoto: String?,
        val approachId: String,
        val approachWeight: Int,
        val approachRepetitions: Int,
        val approachWorkoutId: String,
        val approachExerciseId: String
    )
}