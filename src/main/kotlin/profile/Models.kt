package com.example.profile

import kotlinx.serialization.Serializable

@Serializable
data class EditProfileRequest(
    val name: String,
    val login: String,
    val age: Int?,
    val target: String
)

@Serializable
data class UserDto(
    val id: Int,
    val name: String,
    val login: String,
    val age: Int?,
    val target: String,
    val avatar: String?
)