package com.example.common

sealed interface Response<out T> {
    data class Success<out T>(val data: T) : Response<T>

    data class Error(val error: BaseError) : Response<Nothing> {
        constructor(code: Int, message: String) : this(BaseError(code, message))
    }
}

data class BaseError(val code: Int, val message: String)