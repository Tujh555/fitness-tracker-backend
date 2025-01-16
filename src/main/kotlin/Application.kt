package com.example

import com.example.auth.authRouting
import com.example.profile.profileRouting
import io.ktor.serialization.gson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) {
        gson {}
    }
    configureDatabases()
    authRouting()
    profileRouting()
}
