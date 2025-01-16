package com.example.profile

import com.example.auth.database.Tokens
import com.example.auth.database.Users
import com.example.base.Response
import com.example.base.query
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.coobird.thumbnailator.Thumbnails
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

class ProfileService {
    suspend fun getUserByToken(token: String): UserDto? {
        val user = query {
            val tokenEntity = Tokens
                .selectAll()
                .where { Tokens.token eq token }
                .firstOrNull()
                ?: return@query null

            val userEntity = Users
                .selectAll()
                .where { Users.id eq tokenEntity[Tokens.userId] }
                .firstOrNull()
                ?: return@query null

            UserDto(
                id = userEntity[Users.id],
                name = userEntity[Users.name].orEmpty(),
                login = userEntity[Users.login],
                age = userEntity[Users.age],
                target = userEntity[Users.target].orEmpty(),
                avatar = userEntity[Users.avatarUrl]
            )
        }

        return user
    }

    suspend fun editProfile(token: String, request: EditProfileRequest): Response<UserDto> {
        val user = getUserByToken(token) ?: return Response.Error(404, "Пользователь не найден")

        val updatedUser = query {
            Users.update(
                where = { Users.id eq user.id },
                body = {
                    it[name] = request.name
                    it[login] = request.login
                    it[age] = request.age
                    it[target] = request.target
                }
            )

            user.copy(
                name = request.name,
                login = request.login,
                age = request.age,
                target = request.target
            )
        }

        return Response.Success(updatedUser)
    }

    suspend fun uploadAvatar(token: String, channel: ByteReadChannel, name: String): Response<UserDto> {
        val user = getUserByToken(token) ?: return Response.Error(404, "Пользователь не найден")
        val fileExtension = name.substringAfterLast(".", "").ifEmpty { "jpg" }
        val avatarFileName = "${UUID.randomUUID()}.$fileExtension"
        val avatarsDir = "avatars"
        val avatarPath = Paths.get(avatarsDir, avatarFileName).toString()

        val pathToSave = File(avatarPath)

        withContext(Dispatchers.IO) {
            val writeChannel = pathToSave.writeChannel()
            channel.copyAndClose(writeChannel)
            writeChannel.flushAndClose()
        }

        val thumbnailPath = "${avatarsDir}/${UUID.randomUUID()}_thumbnail.$fileExtension"
        Thumbnails.of(pathToSave).size(500, 500).toFile(File(thumbnailPath))

        val imageUrl = "http://localhost:8080/$thumbnailPath" // TODO: Change for the server
        val updated = query {
            Users.update({ Users.id eq user.id }) {
                it[avatarUrl] = imageUrl
            }
            user.copy(avatar = imageUrl)
        }

        return Response.Success(updated)
    }
}