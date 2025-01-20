package com.example.workout.routing

import com.example.common.respondRes
import com.example.workout.WorkoutDto
import com.example.workout.service.WorkoutService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.io.File
import java.util.*

fun Application.workoutRouting() {
    val workoutService = WorkoutService()

    routing {
        get("/exercises/{filename}") {
            val fileName = call.parameters["filename"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val file = File("avatars", fileName)

            if(!file.exists()) {
                call.respond(HttpStatusCode.NotFound, "File not found")
                return@get
            }

            call.respondFile(file)
        }

        authenticate {
            route("/exercises") {
                post {
                    val token = call.principal<String>()!!

                    val multipart = call.receiveMultipart()
                    var photo: PartData.FileItem? = null
                    var title: String? = null
                    var id: String? = null

                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FileItem -> photo = part

                            is PartData.FormItem -> when (part.name) {
                                "title" -> title = part.value
                                "id" -> id = part.value
                            }

                            is PartData.BinaryChannelItem,
                            is PartData.BinaryItem -> part.dispose()
                        }
                    }

                    val exercise = workoutService.createOrEditExercise(
                        id = id?.let(UUID::fromString),
                        photo = photo,
                        title = title,
                        token = token
                    )

                    call.respondRes(exercise)
                }

                get("/all") {
                    val token = call.principal<String>()!!
                    val exercises = workoutService.getExercises(token)
                    call.respond(exercises)
                }
            }

            route("/workouts") {
                get {
                    val token = call.principal<String>()!!
                    val workouts = workoutService.getWorkouts(token)
                    call.respond(workouts)
                }

                post {
                    try {
                        val token = call.principal<String>()!!
                        val workout = call.receive<WorkoutDto>()
                        val created = workoutService.createWorkout(token, workout)
                        call.respondRes(created)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                put {
                    val workout = call.receive<WorkoutDto>()
                    val updated = workoutService.editWorkout(workout)
                    call.respondRes(updated)
                }
            }

            route("/summary") {
                get {
                    val token = call.principal<String>()!!
                    val summary = workoutService.getSummary(token)
                    call.respond(summary)
                }
            }
        }
    }
}