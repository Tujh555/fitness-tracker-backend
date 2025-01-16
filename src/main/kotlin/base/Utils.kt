package com.example.base

import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend inline fun <T> query(crossinline block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }