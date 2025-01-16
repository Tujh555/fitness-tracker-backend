package com.example.profile

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.util.*

fun Application.profileRouting() {
    val profileService = ProfileService()

    install(Authentication) {
        bearer {
            authenticate { credentials ->
                val user = profileService.getUserByToken(credentials.token)

                if (user != null) {
                    credentials.token
                } else {
                    null
                }
            }
        }
    }

    routing {
        get("/avatars/{filename}") {
            val fileName = call.parameters["filename"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            val file = File("avatars", fileName)

            if(!file.exists()) {
                call.respond(HttpStatusCode.NotFound, "File not found")
                return@get
            }

            call.respondFile(file)
        }

        authenticate {
            route("profile") {
                post("/avatar") {
                    val token = call.principal<String>()!!

                    val filePart = call
                        .receiveMultipart()
                        .asFlow()
                        .onEach { part ->
                            if (part !is PartData.FileItem) {
                                part.dispose()
                            }
                        }
                        .filterIsInstance<PartData.FileItem>()
                        .first()

                    val fileName = filePart.originalFileName ?: UUID.randomUUID().toString()
                    val response = profileService.uploadAvatar(token, filePart.provider(), fileName)
                    call.respond(response)
                }

                post("/edit") {
                    val token = call.principal<String>()!!
                    val request = call.receive<EditProfileRequest>()
                    val response = profileService.editProfile(token, request)
                    call.respond(response)
                }
            }
        }
    }
}

