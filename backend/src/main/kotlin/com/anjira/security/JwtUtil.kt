package com.anjira.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.Date

class JwtUtil private constructor(private val secret: String) {

    private val algorithm = Algorithm.HMAC256(secret)
    private val issuer = "taskplanner"
    private val audience = "taskplanner_users"
    private val accessExpiryMs = 60 * 60 * 1000L       // 1 hour
    private val refreshExpiryMs = 30 * 24 * 60 * 60 * 1000L // 30 days

    fun generateAccessToken(userId: Int, username: String): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId.toString())
            .withClaim("username", username)
            .withClaim("type", "access")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + accessExpiryMs))
            .sign(algorithm)
    }

    fun generateRefreshToken(userId: Int): String {
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId.toString())
            .withClaim("type", "refresh")
            .withIssuedAt(Date())
            .withExpiresAt(Date(System.currentTimeMillis() + refreshExpiryMs))
            .sign(algorithm)
    }

    fun verifyToken(token: String): DecodedJWT? {
        return try {
            JWT.require(algorithm)
                .withIssuer(issuer)
                .withAudience(audience)
                .build()
                .verify(token)
        } catch (e: Exception) {
            null
        }
    }

    fun getUserIdFromToken(token: String): Int? {
        return verifyToken(token)?.subject?.toIntOrNull()
    }

    companion object {
        @Volatile
        private var instance: JwtUtil? = null

        fun getInstance(): JwtUtil {
            return instance ?: synchronized(this) {
                instance ?: JwtUtil(System.getenv("JWT_SECRET") ?: "default-secret-key-change-in-production").also { instance = it }
            }
        }
    }
}
