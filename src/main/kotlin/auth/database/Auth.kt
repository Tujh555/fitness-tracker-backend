package com.example.auth.database

import com.example.profile.UserDto
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val id = integer("id").autoIncrement()
    val login = text("login").uniqueIndex()
    val password = text("password")
    val name = text("name").nullable()
    val age = integer("age").nullable()
    val target = text("target").nullable()
    val avatarUrl = text("avatar_url").nullable()

    override val primaryKey = PrimaryKey(id)
}

object Tokens : Table() {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val token = text("token").uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}

fun ResultRow.asUserDto() = UserDto(
    id = get(Users.id),
    name = get(Users.name).orEmpty(),
    login = get(Users.login),
    age = get(Users.age),
    target = get(Users.target).orEmpty(),
    avatar = get(Users.avatarUrl)
)