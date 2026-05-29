package com.anjira.routes

import com.anjira.db.UserTable
import com.anjira.security.JwtUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

private val logger = LoggerFactory.getLogger("AuthRoutes")

data class RegisterRequest(val username: String, val email: String, val password: String)
data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val token: String, val userId: Int, val username: String)

fun hashPassword(password: String): String = java.security.MessageDigest.getInstance("SHA-256")
    .digest(password.toByteArray())
    .joinToString("") { "%02x".format(it) }

fun Route.AuthRoute() {
    route("/auth") {
        post("/register") {
            val registerRequest = call.receive<RegisterRequest>()
            logger.info("Registration attempt for email: ${registerRequest.email}")

            val existingUser = transaction {
                UserTable.select { UserTable.email eq registerRequest.email }.firstOrNull()
            }
            if (existingUser != null) {
                call.respond(HttpStatusCode.Conflict, mapOf("error" to "User with this email already exists"))
                return@post
            }

            val hashedPassword = hashPassword(registerRequest.password)
            val newUserId = transaction {
                UserTable.insertAndGetId {
                    it[username] = registerRequest.username
                    it[email] = registerRequest.email
                    it[passwordHash] = hashedPassword
                    it[createdAt] = LocalDateTime.now().toString()
                    it[updatedAt] = LocalDateTime.now().toString()
                }
            }

            val jwtUtil = JwtUtil.getInstance()
            val token = jwtUtil.generateToken(newUserId.value, registerRequest.username)

            logger.info("User registered successfully: ${newUserId.value}")
            call.respond(AuthResponse(token, newUserId.value, registerRequest.username))
        }

        post("/login") {
            val loginRequest = call.receive<LoginRequest>()
            logger.info("Login attempt for email: ${loginRequest.email}")

            val user = transaction {
                UserTable.select { UserTable.email eq loginRequest.email }.firstOrNull()
            }

            if (user == null || hashPassword(loginRequest.password) != user[UserTable.passwordHash]) {
                logger.warn("Invalid credentials for email: ${loginRequest.email}")
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid email or password"))
                return@post
            }

            val userId = user[UserTable.id].value
            val username = user[UserTable.username]
            val jwtUtil = JwtUtil.getInstance()
            val token = jwtUtil.generateToken(userId, username)

            logger.info("User logged in successfully: $userId")
            call.respond(AuthResponse(token, userId, username))
        }
    }
}
