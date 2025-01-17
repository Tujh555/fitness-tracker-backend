package com.example.auth.service

import com.example.auth.database.Tokens
import com.example.auth.database.Users
import com.example.auth.database.asUserDto
import com.example.auth.models.AuthResponse
import com.example.auth.models.LogoutRequest
import com.example.auth.models.RegisterRequest
import com.example.common.Response
import com.example.common.query
import com.example.profile.UserDto
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.mindrot.jbcrypt.BCrypt
import java.util.*

class AuthService {
    suspend fun register(request: RegisterRequest): Response<AuthResponse> = query {
        val existingUser = Users
            .selectAll()
            .where { Users.login eq request.login }
            .firstOrNull()

        if (existingUser != null) {
            return@query Response.Error(401, "Пользователь с таким именем уже существует")
        }

        val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())
        val userInsert = Users.insert {
            it[login] = request.login
            it[password] = hashedPassword
        }
        val newToken = UUID.randomUUID().toString()

        Tokens.insert {
            it[userId] = userInsert[Users.id]
            it[token] = newToken
        }
        val userDto = UserDto(
            id = userInsert[Users.id],
            name = userInsert[Users.name].orEmpty(),
            login = userInsert[Users.login],
            age = userInsert[Users.age],
            target = userInsert[Users.target].orEmpty(),
            avatar = userInsert[Users.avatarUrl]
        )
        Response.Success(AuthResponse(userDto, newToken))
    }


    suspend fun login(request: RegisterRequest): Response<AuthResponse> = query {
        val user = Users
            .selectAll()
            .where { Users.login eq request.login }
            .firstOrNull()
            ?: return@query Response.Error(402, "Пользователя с таким именем не существует")

        if (BCrypt.checkpw(request.password, user[Users.password]).not()) {
            return@query Response.Error(403, "Неверный пароль")
        }
        val newToken = UUID.randomUUID().toString()
        Tokens.insert {
            it[userId] = user[Users.id]
            it[token] = newToken
        }
        val userDto = user.asUserDto()
        Response.Success(AuthResponse(userDto, newToken))
    }

    suspend fun logout(request: LogoutRequest) {
        query {
            Tokens.deleteWhere { userId eq request.id }
        }
    }
}