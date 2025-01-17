package com.example.auth

import com.example.auth.models.LogoutRequest
import com.example.auth.models.RegisterRequest
import com.example.auth.service.AuthService
import com.example.profile.getUserByToken
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.authRouting() {
    val authService = AuthService()

    install(Authentication) {
        bearer {
            authenticate { credentials ->
                val user = getUserByToken(credentials.token)

                if (user != null) {
                    credentials.token
                } else {
                    null
                }
            }
        }
    }

    routing {
        route("auth") {
            post("/register") {
                val request = call.receive<RegisterRequest>()
                val response = authService.register(request)
                call.respond(response)
            }

            post("/login") {
                val request = call.receive<RegisterRequest>()
                val response = authService.login(request)
                call.respond(response)
            }

            post("/logout") {
                try {
                    val request = call.receive<LogoutRequest>()
                    authService.logout(request)
                    call.respond("")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, e.message ?: "Unknown error")
                }
            }
        }
    }
}