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

class TaskRoutesTest {

    @Test
    fun `test task creation and retrieval`() = testApplication {
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
        val token = registerAndGetToken(client, "tasktest@example.com", "password")
        val groupId = createGroup(client, token, "Test Group", "Test group for testing")

        val createResponse = client.post("/groups/$groupId/tasks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"title":"Test Task","description":"Test task description"}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        assertTrue(createResponse.bodyAsText().contains("Test Task"))

        val getResponse = client.get("/groups/$groupId/tasks") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertTrue(getResponse.bodyAsText().contains("Test Task"))
    }

    @Test
    fun `test task update`() = testApplication {
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
        val token = registerAndGetToken(client, "taskupdate@example.com", "password")
        val groupId = createGroup(client, token, "Update Group", "Group for update test")

        val createResponse = client.post("/groups/$groupId/tasks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"title":"Original Task","description":"Original description"}""")
        }
        val createText = createResponse.bodyAsText()
        val taskId = createText.substringAfter(""""id":""").substringBefore(",").toInt()

        val updateResponse = client.put("/tasks/$taskId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"title":"Updated Task","status":"in_progress"}""")
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        assertTrue(updateResponse.bodyAsText().contains("Updated Task"))
        assertTrue(updateResponse.bodyAsText().contains("in_progress"))
    }

    @Test
    fun `test task deletion`() = testApplication {
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
        val token = registerAndGetToken(client, "taskdelete@example.com", "password")
        val groupId = createGroup(client, token, "Delete Group", "Group for delete test")

        val createResponse = client.post("/groups/$groupId/tasks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"title":"Task to Delete"}""")
        }
        val createText = createResponse.bodyAsText()
        val taskId = createText.substringAfter(""""id":""").substringBefore(",").toInt()

        val deleteResponse = client.delete("/tasks/$taskId") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)
    }

    @Test
    fun `test subtask creation and completion`() = testApplication {
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
        val token = registerAndGetToken(client, "subtasktest@example.com", "password")
        val groupId = createGroup(client, token, "Subtask Group", "Group for subtask test")

        val createResponse = client.post("/groups/$groupId/tasks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"title":"Parent Task"}""")
        }
        val createText = createResponse.bodyAsText()
        val taskId = createText.substringAfter(""""id":""").substringBefore(",").toInt()

        val subtaskResponse = client.post("/tasks/$taskId/subtasks") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"title":"Test Subtask"}""")
        }
        assertEquals(HttpStatusCode.Created, subtaskResponse.status)
        val subtaskText = subtaskResponse.bodyAsText()
        assertTrue(subtaskText.contains("Test Subtask"))
        assertTrue(subtaskText.contains("\"isCompleted\":false"))

        val subtaskId = subtaskText.substringAfter(""""id":""").substringBefore(",").toInt()
        val updateResponse = client.put("/subtasks/$subtaskId?isCompleted=true") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        assertTrue(updateResponse.bodyAsText().contains("\"isCompleted\":true"))
    }

    private suspend fun registerAndGetToken(client: HttpClient, email: String, password: String): String {
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        return response.bodyAsText().substringAfter(""""accessToken":"""").substringBefore("""","""")
    }

    private suspend fun createGroup(client: HttpClient, token: String, name: String, description: String?): Int {
        val body = buildString {
            append("""{"name":"$name"""")
            if (description != null) append(""","description":"$description"""")
            append("""}""")
        }
        val response = client.post("/groups") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(body)
        }
        assertEquals(HttpStatusCode.Created, response.status)
        return response.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun initEnv() {
            System.setProperty("JWT_SECRET", "test-secret-key-for-tests")
        }
    }
}
