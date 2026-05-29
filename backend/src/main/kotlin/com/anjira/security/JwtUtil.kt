package com.anjira.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import java.util.Date
import javax.crypto.spec.SecretKeySpec

class JwtUtil private constructor(
    private val algorithm: Algorithm,
    private val issuer: String = "taskplanner",
    private val audience: String = "taskplanner_users",
    private val expiryMinutes: Long = 60 * 24
) {
    companion object {
        @Volatile private var INSTANCE: JwtUtil? = null

        fun getInstance(): JwtUtil {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: JwtUtil(
                    algorithm = createAlgorithm(),
                    issuer = System.getenv("JWT_ISSUER") ?: "taskplanner",
                    audience = System.getenv("JWT_AUDIENCE") ?: "taskplanner_users",
                    expiryMinutes = System.getenv("JWT_EXPIRY_MINUTES")?.toLong() ?: 60 * 24
                ).also { INSTANCE = it }
            }
        }

        private fun createAlgorithm(): Algorithm {
            val secret = System.getenv("JWT_SECRET") ?: "default-secret-key-change-in-production"
            return Algorithm.HMAC256(secret)
        }
    }

    fun generateToken(userId: Int, username: String): String {
        val now = Date()
        val expiry = Date(now.time + expiryMinutes * 60 * 1000)

        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(userId.toString())
            .withClaim("username", username)
            .withIssuedAt(now)
            .withExpiresAt(expiry)
            .sign(algorithm)
    }

    fun getUserIdFromToken(token: String): Int {
        val verifier = JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
        val decoded: DecodedJWT = verifier.verify(token)
        return decoded.subject!!.toInt()
    }

    fun getUsernameFromToken(token: String): String {
        val verifier = JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
        val decoded: DecodedJWT = verifier.verify(token)
        return decoded.getClaim("username").asString()
    }
}
