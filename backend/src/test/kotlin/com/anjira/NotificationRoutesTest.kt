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

class NotificationRoutesTest {

    private fun uniqueEmail(): String { counter++; return "not$counter@example.com" }

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
            routing { AuthRoute(); authenticate("jwt") { GroupRoute(); NotificationRoutes() } }
        }
    }

    @Test
    fun `test notification preferences get with defaults`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Prefs Group", "Group for prefs test")
        val getResponse = client.get("/notifications/preferences/$groupId") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, getResponse.status)
        val text = getResponse.bodyAsText()
        assertTrue(text.contains("taskAssigned"))
        assertTrue(text.contains("true"))
    }

    @Test
    fun `test notification preferences update`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Prefs Upd Group", "Group for prefs update")
        client.put("/notifications/preferences/$groupId") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"taskAssigned":false,"taskStatusChanged":false,"meetingReminderMinutes":15}""")
        }.also { assertEquals(HttpStatusCode.OK, it.status) }
        val getResponse = client.get("/notifications/preferences/$groupId") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, getResponse.status)
        val text = getResponse.bodyAsText()
        assertTrue(text.contains(""""taskAssigned":false"""))
        assertTrue(text.contains(""""meetingReminderMinutes":15"""))
    }

    @Test
    fun `test fcm register and unregister`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val registerResponse = client.post("/notifications/fcm/register") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"token":"test-fcm-token-123","deviceName":"Test Device"}""")
        }
        assertEquals(HttpStatusCode.Created, registerResponse.status)
        assertTrue(registerResponse.bodyAsText().contains("Token registered"))
        val unregisterResponse = client.post("/notifications/fcm/unregister") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"token":"test-fcm-token-123"}""")
        }
        assertEquals(HttpStatusCode.NoContent, unregisterResponse.status)
    }

    @Test
    fun `test notification list`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val notifsResponse = client.get("/notifications") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, notifsResponse.status)
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
