package com.example

import com.example.dao.UserEntity
import com.example.security.JwtUtil
import io.ktor.application.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import io.ktor.util.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertFalse
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertTrue
import org.junit.jupiter.api.Assertions.*
import org.slf4j.LoggerFactory
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val logger = LoggerFactory.getLogger("AuthRoutesTest")

class AuthRoutesTest {

    private lateinit var testApplication: TestApplication
    private val shutdownLatch = CountDownLatch(1)

    @BeforeEach
    fun setUp() {
        testApplication = TestApplication {
            // Initialize database (uses test configuration if env vars are set)
            com.example.config.DatabaseConfig.init()

            // Content negotiation
            install(io.ktor.server.contentNegotiation.ContentNegotiation) {
                json()
            }

            // Authentication
            authentication {
                jwt("jwt") {
                    verifier(com.example.security.JwtUtil.getInstance().verifier())
                    validate { 
                        // If token is valid, the principal (UserId) is available here
                        it 
                    }
                }
            }

            // Routes
            io.ktor.server.routing.routing {
                com.example.routes.AuthRoutes()
                com.example.routes.GroupRoutes()
            }
        }
    }

    @AfterEach
    fun tearDown() {
        testApplication.stop()
        shutdownLatch.countDown()
    }

    @Test
    fun `test registration and login flow`() {
        // Test registration
        val registerResponse = testApplication.client.postForm(
            "/auth/register",
            paramsMapOf(
                "username" to "testuser",
                "email" to "test@example.com",
                "password" to "testpassword"
            )
        )

        assertEquals(HttpStatusCode.Created, registerResponse.status)
        val authResponse = registerResponse.body<AuthResponse>()
        assertNotNull(authResponse)
        assertNotNull(authResponse.token)
        assertEquals(1, authResponse.userId) // First user gets ID 1
        assertEquals("testuser", authResponse.username)

        // Test login with same credentials
        val loginResponse = testApplication.client.postForm(
            "/auth/login",
            paramsMapOf(
                "email" to "test@example.com",
                "password" to "testpassword"
            )
        )

        assertEquals(HttpStatusCode.OK, loginResponse.status)
        val loginAuthResponse = loginResponse.body<AuthResponse>()
        assertNotNull(loginAuthResponse)
        assertNotNull(loginAuthResponse.token)
        assertEquals(authResponse.userId, loginAuthResponse.userId)
        assertEquals(authResponse.username, loginAuthResponse.username)

        // Verify token is valid by accessing a protected endpoint
        val groupsResponse = testApplication.client.get(
            "/groups",
            headers = appendHeader(HttpHeaders.Authorization, "Bearer ${loginAuthResponse.token}")
        )

        assertEquals(HttpStatusCode.OK, groupsResponse.status)
        // Should be empty list since no groups created yet
        val groups = groupsResponse.body<List<*>>()
        assertTrue(groups.isEmpty())
    }

    @Test
    fun `test duplicate registration fails`() {
        // First registration
        testApplication.client.postForm(
            "/auth/register",
            paramsMapOf(
                "username" to "user2",
                "email" to "user2@example.com",
                "password" to "password"
            )
        )

        // Attempt duplicate registration
        val response = testApplication.client.postForm(
            "/auth/register",
            paramsMapOf(
                "username" to "user2duplicate",
                "email" to "user2@example.com", // Same email
                "password" to "password"
            )
        )

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `test login with invalid credentials fails`() {
        // Register a user
        testApplication.client.postForm(
            "/auth/register",
            paramsMapOf(
                "username" to "user3",
                "email" to "user3@example.com",
                "password" to "correctpassword"
            )
        )

        // Try to login with wrong password
        val response = testApplication.client.postForm(
            "/auth/login",
            paramsMapOf(
                "email" to "user3@example.com",
                "password" to "wrongpassword"
            )
        )

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    // Data class for authentication response
    data class AuthResponse(val token: String, val userId: Int, val username: String)
}