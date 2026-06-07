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

class MeetingRoutesTest {

    private fun uniqueEmail(): String { counter++; return "mtg$counter@example.com" }

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
    fun `test meeting creation and retrieval`() = testApplication {
        DatabaseConfig.init()
        appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Meeting Group", "Group for meeting tests")
        val createResponse = client.post("/groups/$groupId/meetings") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"title":"Team Standup","description":"Daily sync","dateTime":"2025-06-07T10:00:00","location":"Room A","invitedUserIds":[]}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        assertTrue(createResponse.bodyAsText().contains("Team Standup"))
        val getResponse = client.get("/groups/$groupId/meetings") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertTrue(getResponse.bodyAsText().contains("Team Standup"))
    }

    @Test
    fun `test meeting update`() = testApplication {
        DatabaseConfig.init()
        appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Update Group", "Group for meeting update")
        val createResponse = client.post("/groups/$groupId/meetings") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"title":"Original Meeting","dateTime":"2025-06-07T10:00:00","invitedUserIds":[]}""")
        }
        val meetingId = createResponse.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
        val updateResponse = client.put("/meetings/$meetingId") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"title":"Updated Meeting","location":"Room B"}""")
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        assertTrue(updateResponse.bodyAsText().contains("Updated Meeting"))
    }

    @Test
    fun `test meeting deletion`() = testApplication {
        DatabaseConfig.init()
        appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Delete Group", "Group for meeting delete")
        val createResponse = client.post("/groups/$groupId/meetings") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"title":"Meeting to Delete","dateTime":"2025-06-07T10:00:00","invitedUserIds":[]}""")
        }
        val meetingId = createResponse.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
        val deleteResponse = client.delete("/meetings/$meetingId") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)
    }

    @Test
    fun `test rsvp flow`() = testApplication {
        DatabaseConfig.init()
        appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "RSVP Group", "Group for RSVP test")
        val meetResponse = client.post("/groups/$groupId/meetings") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"title":"RSVP Meeting","dateTime":"2025-06-07T10:00:00","invitedUserIds":[]}""")
        }
        val meetingId = meetResponse.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
        val uid = JWT.decode(token).subject.toInt()
        client.post("/meetings/$meetingId/participants") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"userId":$uid}""")
        }
        val rsvpResponse = client.put("/meetings/$meetingId/participants/$uid/rsvp") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"status":"going"}""")
        }
        assertEquals(HttpStatusCode.OK, rsvpResponse.status)
        assertTrue(rsvpResponse.bodyAsText().contains("going"))
        val participantsResponse = client.get("/meetings/$meetingId/participants") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, participantsResponse.status)
        assertTrue(participantsResponse.bodyAsText().contains("going"))
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
