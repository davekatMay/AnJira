package com.anjira

import com.anjira.config.DatabaseConfig
import com.anjira.routes.AuthRoute
import com.anjira.routes.GroupRoute
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import io.ktor.serialization.gson.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.*

class AuthRoutesTest {

    @Test
    fun `test registration and login flow`() = testApplication {
        DatabaseConfig.init()
        application {
            install(ContentNegotiation) { gson {} }
            install(Authentication) {
                jwt("jwt") {
                    val secret = System.getProperty("JWT_SECRET") ?: "test-secret-key-for-tests"
                    val algorithm = Algorithm.HMAC256(secret)
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
                authenticate("jwt") { GroupRoute() }
            }
        }
        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"test@example.com","password":"testpassword"}""")
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)

        val authResponse = registerResponse.bodyAsText()
        assertTrue(authResponse.contains("accessToken"))
        assertTrue(authResponse.contains("refreshToken"))
        assertTrue(authResponse.contains("userId"))
        assertTrue(authResponse.contains("test"))

        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"test@example.com","password":"testpassword"}""")
        }
        assertEquals(HttpStatusCode.OK, loginResponse.status)
        assertTrue(loginResponse.bodyAsText().contains("accessToken"))
    }

    @Test
    fun `test duplicate registration fails`() = testApplication {
        DatabaseConfig.init()
        application {
            install(ContentNegotiation) { gson {} }
            install(Authentication) {
                jwt("jwt") {
                    val secret = System.getProperty("JWT_SECRET") ?: "test-secret-key-for-tests"
                    val algorithm = Algorithm.HMAC256(secret)
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
                authenticate("jwt") { GroupRoute() }
            }
        }
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"dup@example.com","password":"password"}""")
        }

        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"dup@example.com","password":"password"}""")
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `test login with invalid credentials fails`() = testApplication {
        DatabaseConfig.init()
        application {
            install(ContentNegotiation) { gson {} }
            install(Authentication) {
                jwt("jwt") {
                    val secret = System.getProperty("JWT_SECRET") ?: "test-secret-key-for-tests"
                    val algorithm = Algorithm.HMAC256(secret)
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
                authenticate("jwt") { GroupRoute() }
            }
        }
        client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@example.com","password":"correctpassword"}""")
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"user@example.com","password":"wrongpassword"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun initEnv() {
            System.setProperty("JWT_SECRET", "test-secret-key-for-tests")
        }
    }
}
