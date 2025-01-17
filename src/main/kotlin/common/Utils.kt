package com.example.common

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> query(block: suspend Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }