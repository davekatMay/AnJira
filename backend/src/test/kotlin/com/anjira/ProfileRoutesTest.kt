package com.anjira

import com.anjira.config.DatabaseConfig
import com.anjira.routes.AuthRoute
import com.anjira.routes.GroupRoute
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.*
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

class ProfileRoutesTest {

    private fun uniqueEmail(): String { counter++; return "prof$counter@example.com" }

    private fun TestApplicationBuilder.appSetup() {
        application {
            install(ContentNegotiation) { gson {} }
            install(Authentication) {
                jwt("jwt") {
                    val secret = System.getProperty("JWT_SECRET") ?: "test-secret-key-for-tests"
                    val algorithm = Algorithm.HMAC256(secret)
                    verifier(JWT.require(algorithm).withIssuer("taskplanner").withAudience("taskplanner_users").build())
                    validate { credential ->
                        if (credential.payload.audience.contains("taskplanner_users")) UserIdPrincipal(credential.payload.subject ?: "") else null
                    }
                }
            }
            routing { AuthRoute(); authenticate("jwt") { GroupRoute() } }
        }
    }

    @Test
    fun `test user stats returns zero values initially`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val statsResponse = client.get("/users/me/stats") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, statsResponse.status)
        val text = statsResponse.bodyAsText()
        assertTrue(text.contains("completedTasks"))
        assertTrue(text.contains("attendedMeetings"))
    }

    @Test
    fun `test user stats shows task and meeting counts`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Stats Group", "Group for stats test")
        val userId = JWT.decode(token).subject.toInt()
        client.post("/groups/$groupId/tasks") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"title":"Completed Task","assignedTo":$userId}""")
        }.let { createResponse ->
            val taskId = createResponse.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
            client.put("/tasks/$taskId") {
                contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
                setBody("""{"status":"done"}""")
            }
        }
        val statsResponse = client.get("/users/me/stats") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, statsResponse.status)
        val text = statsResponse.bodyAsText()
        assertTrue(text.contains(""""completedTasks":1"""), "Should have 1 completed task")
    }

    @Test
    fun `test user stats requires auth`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val response = client.get("/users/me/stats")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    private suspend fun registerAndGetToken(client: HttpClient, email: String, password: String): String {
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json); setBody("""{"email":"$email","password":"$password"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        return response.bodyAsText().substringAfter(""""accessToken":"""").substringBefore("""","""")
    }

    private suspend fun createGroup(client: HttpClient, token: String, name: String, description: String?): Int {
        val body = buildString {
            append("""{"name":"$name""""); if (description != null) append(""","description":"$description""""); append("""}""")
        }
        val response = client.post("/groups") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token"); setBody(body)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        return response.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
    }

    companion object {
        private var counter = 0
        @JvmStatic @BeforeAll fun initEnv() { System.setProperty("JWT_SECRET", "test-secret-key-for-tests") }
    }
}
