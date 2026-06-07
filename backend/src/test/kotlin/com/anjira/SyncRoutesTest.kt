package com.anjira

import com.anjira.config.DatabaseConfig
import com.anjira.routes.*
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

class SyncRoutesTest {

    private fun uniqueEmail(): String { counter++; return "sync$counter@example.com" }

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
            routing { AuthRoute(); authenticate("jwt") { GroupRoute(); SyncRoutes() } }
        }
    }

    @Test
    fun `test sync returns data after changes`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Sync Group", "Group for sync test")
        val taskResponse = client.post("/groups/$groupId/tasks") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"title":"Sync Task","description":"Test sync"}""")
        }
        assertEquals(HttpStatusCode.Created, taskResponse.status)
        val syncResponse = client.get("/sync") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, syncResponse.status)
        val syncText = syncResponse.bodyAsText()
        assertTrue(syncText.contains("Sync Group"))
        assertTrue(syncText.contains("Sync Task"))
        assertTrue(syncText.contains("serverTime"))
    }

    @Test
    fun `test sync with since parameter filters results`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        createGroup(client, token, "Filter Group", "Group for sync filter test")
        val syncFuture = client.get("/sync?since=2030-01-01T00:00:00") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, syncFuture.status)
        assertFalse(syncFuture.bodyAsText().contains("Filter Group"), "Group created before since should not appear")
    }

    @Test
    fun `test sync with invalid since returns 400`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val response = client.get("/sync?since=invalid-date") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `test sync without auth returns 401`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val response = client.get("/sync")
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
