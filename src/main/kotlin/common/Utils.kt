package com.example.common

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> query(block: suspend Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }

suspend inline fun <reified T : Any> RoutingCall.respondRes(response: Response<T>) {
    when (val res = response) {
        is Response.Error -> respond(HttpStatusCode(res.error.code, res.error.message), res.error.message)
        is Response.Success<T> -> respond(res.data)
    }
}