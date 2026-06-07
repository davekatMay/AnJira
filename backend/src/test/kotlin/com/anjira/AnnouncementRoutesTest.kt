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

class AnnouncementRoutesTest {

    private fun uniqueEmail(): String { counter++; return "ann$counter@example.com" }

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
    fun `test announcement creation and retrieval`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Announce Group", "Group for announcement tests")
        val createResponse = client.post("/groups/$groupId/announcements") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"text":"Important announcement!","attachments":"[]"}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        assertTrue(createResponse.bodyAsText().contains("Important announcement!"))
        val getResponse = client.get("/groups/$groupId/announcements") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertTrue(getResponse.bodyAsText().contains("Important announcement!"))
    }

    @Test
    fun `test announcement update`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Update Ann Group", "Group for ann update")
        val createResponse = client.post("/groups/$groupId/announcements") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"text":"Original text","attachments":"[]"}""")
        }
        val annId = createResponse.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
        val updateResponse = client.put("/announcements/$annId") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"text":"Updated text"}""")
        }
        assertEquals(HttpStatusCode.OK, updateResponse.status)
        assertTrue(updateResponse.bodyAsText().contains("Updated text"))
    }

    @Test
    fun `test announcement deletion`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Delete Ann Group", "Group for ann delete")
        val createResponse = client.post("/groups/$groupId/announcements") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"text":"To be deleted","attachments":"[]"}""")
        }
        val annId = createResponse.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
        val deleteResponse = client.delete("/announcements/$annId") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)
    }

    @Test
    fun `test announcement pin toggle`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Pin Ann Group", "Group for ann pin test")
        val createResponse = client.post("/groups/$groupId/announcements") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"text":"Pinnable announcement","attachments":"[]"}""")
        }
        val annId = createResponse.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
        val pinResponse = client.put("/announcements/$annId/pin") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, pinResponse.status)
        assertTrue(pinResponse.bodyAsText().contains("Pin status toggled"))
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
