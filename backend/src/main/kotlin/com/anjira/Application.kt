package com.anjira

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.anjira.config.DatabaseConfig
import com.anjira.routes.AuthRoute
import com.anjira.routes.GroupRoute
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.gson.*
import io.ktor.server.plugins.contentnegotiation.*
import org.slf4j.LoggerFactory

fun main() {
    val logger = LoggerFactory.getLogger("Application")

    val jwtSecret = System.getenv("JWT_SECRET")
    require(!jwtSecret.isNullOrBlank()) { "JWT_SECRET environment variable is required" }

    logger.info("Starting server...")

    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        DatabaseConfig.init()

        install(ContentNegotiation) {
            gson {}
        }

        install(Authentication) {
            jwt("jwt") {
                val algorithm = Algorithm.HMAC256(jwtSecret)
                verifier(JWT.require(algorithm)
                    .withIssuer("taskplanner")
                    .withAudience("taskplanner_users")
                    .build())
                validate { credential ->
                    if (credential.payload.audience.contains("taskplanner_users")) {
                        UserIdPrincipal(credential.payload.subject ?: "")
                    } else null
                }
            }
        }

        routing {
            AuthRoute()
            authenticate("jwt") {
                GroupRoute()
            }
        }

    }.start(wait = true)
}
