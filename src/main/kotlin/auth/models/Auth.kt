package com.example.auth.models

import com.example.profile.UserDto

data class RegisterRequest(val login: String, val password: String, val age: Int?, val name: String?)

data class LogoutRequest(val id: Int)

data class AuthResponse(val user: UserDto, val token: String)