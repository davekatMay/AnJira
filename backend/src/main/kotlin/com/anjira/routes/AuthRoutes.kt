package com.anjira.routes

import com.anjira.db.RefreshTokenTable
import com.anjira.db.UserTable
import com.anjira.security.JwtUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

private val logger = LoggerFactory.getLogger("AuthRoutes")

data class RegisterRequest(val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val accessToken: String, val refreshToken: String, val userId: Int, val username: String)
data class RefreshRequest(val refreshToken: String)

fun hashPassword(password: String): String = java.security.MessageDigest.getInstance("SHA-256")
    .digest(password.toByteArray())
    .joinToString("") { "%02x".format(it) }

fun Route.AuthRoute() {
    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            logger.info("Registration attempt for email: ${request.email}")

            val existingUser = transaction {
                UserTable.select { UserTable.email eq request.email }.firstOrNull()
            }
            if (existingUser != null) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "User with this email already exists"))
                return@post
            }

            val username = request.email.substringBefore("@")
            val hashedPassword = hashPassword(request.password)
            val newUserId = transaction {
                UserTable.insertAndGetId {
                    it[UserTable.username] = username
                    it[UserTable.email] = request.email
                    it[UserTable.passwordHash] = hashedPassword
                    it[UserTable.createdAt] = LocalDateTime.now().toString()
                    it[UserTable.updatedAt] = LocalDateTime.now().toString()
                }
            }

            val jwtUtil = JwtUtil.getInstance()
            val accessToken = jwtUtil.generateAccessToken(newUserId.value, username)
            val refreshToken = jwtUtil.generateRefreshToken(newUserId.value)

            saveRefreshToken(newUserId.value, refreshToken)

            logger.info("User registered successfully: ${newUserId.value}")
            call.respond(AuthResponse(accessToken, refreshToken, newUserId.value, username))
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            logger.info("Login attempt for email: ${request.email}")

            val user = transaction {
                UserTable.select { UserTable.email eq request.email }.firstOrNull()
            }

            if (user == null || hashPassword(request.password) != user[UserTable.passwordHash]) {
                logger.warn("Invalid credentials for email: ${request.email}")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid email or password"))
                return@post
            }

            val userId = user[UserTable.id].value
            val username = user[UserTable.username]
            val jwtUtil = JwtUtil.getInstance()
            val accessToken = jwtUtil.generateAccessToken(userId, username)
            val refreshToken = jwtUtil.generateRefreshToken(userId)

            saveRefreshToken(userId, refreshToken)

            logger.info("User logged in successfully: $userId")
            call.respond(AuthResponse(accessToken, refreshToken, userId, username))
        }

        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            val jwtUtil = JwtUtil.getInstance()

            val decoded = jwtUtil.verifyToken(request.refreshToken)
            if (decoded == null || decoded.getClaim("type").asString() != "refresh") {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid refresh token"))
                return@post
            }

            val userId = decoded.subject.toIntOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid refresh token"))
                return@post
            }

            val stored = transaction {
                RefreshTokenTable.select { RefreshTokenTable.userId eq userId }
                    .adjustWhere { RefreshTokenTable.token eq request.refreshToken }
                    .firstOrNull()
            }

            if (stored == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Refresh token revoked"))
                return@post
            }

            val user = transaction {
                UserTable.select { UserTable.id eq userId }.firstOrNull()
            } ?: run {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "User not found"))
                return@post
            }

            val username = user[UserTable.username]
            val newAccessToken = jwtUtil.generateAccessToken(userId, username)
            val newRefreshToken = jwtUtil.generateRefreshToken(userId)

            transaction {
                RefreshTokenTable.deleteWhere { RefreshTokenTable.token eq request.refreshToken }
            }
            saveRefreshToken(userId, newRefreshToken)

            call.respond(AuthResponse(newAccessToken, newRefreshToken, userId, username))
        }
    }
}

private fun saveRefreshToken(userId: Int, token: String) {
    transaction {
        RefreshTokenTable.insert { r ->
            r[RefreshTokenTable.userId] = userId
            r[RefreshTokenTable.token] = token
            r[RefreshTokenTable.expiresAt] = LocalDateTime.now().plusDays(30).toString()
            r[RefreshTokenTable.createdAt] = LocalDateTime.now().toString()
        }
    }
}
