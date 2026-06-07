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

class PlaylistRoutesTest {

    private fun uniqueEmail(): String { counter++; return "pl$counter@example.com" }

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
    fun `test playlist creation and retrieval`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Playlist Group", "Group for playlist tests")
        val createResponse = client.post("/groups/$groupId/playlists") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"name":"Chill Vibes","type":"group"}""")
        }
        assertEquals(HttpStatusCode.Created, createResponse.status)
        assertTrue(createResponse.bodyAsText().contains("Chill Vibes"))
        val getResponse = client.get("/groups/$groupId/playlists") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, getResponse.status)
        assertTrue(getResponse.bodyAsText().contains("Chill Vibes"))
    }

    @Test
    fun `test playlist rename`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Rename Group", "Group for rename test")
        val createResponse = client.post("/groups/$groupId/playlists") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"name":"Old Name","type":"group"}""")
        }
        val plId = createResponse.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
        val renameResponse = client.put("/playlists/$plId") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"name":"New Name"}""")
        }
        assertEquals(HttpStatusCode.OK, renameResponse.status)
        assertTrue(renameResponse.bodyAsText().contains("Playlist updated"))
    }

    @Test
    fun `test playlist deletion`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Del Pl Group", "Group for pl delete")
        val createResponse = client.post("/groups/$groupId/playlists") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"name":"Delete Me","type":"group"}""")
        }
        val plId = createResponse.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
        val deleteResponse = client.delete("/playlists/$plId") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)
    }

    @Test
    fun `test track addition and retrieval`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Track Group", "Group for track test")
        val plResponse = client.post("/groups/$groupId/playlists") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"name":"Test Playlist","type":"group"}""")
        }
        val plId = plResponse.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
        val trackResponse = client.post("/playlists/$plId/tracks") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"trackId":"12345","trackName":"Test Song","artistName":"Test Artist","trackViewUrl":"https://example.com/track"}""")
        }
        assertEquals(HttpStatusCode.Created, trackResponse.status)
        assertTrue(trackResponse.bodyAsText().contains("Test Song"))
        val getTracksResponse = client.get("/playlists/$plId/tracks") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, getTracksResponse.status)
        assertTrue(getTracksResponse.bodyAsText().contains("Test Song"))
    }

    @Test
    fun `test track reorder`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Reorder Group", "Group for reorder test")
        val plResponse = client.post("/groups/$groupId/playlists") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"name":"Reorder Playlist","type":"group"}""")
        }
        val plId = plResponse.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
        val tr1 = client.post("/playlists/$plId/tracks") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"trackId":"1","trackName":"Song A","artistName":"Artist","trackViewUrl":"https://example.com/1"}""")
        }.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
        val tr2 = client.post("/playlists/$plId/tracks") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"trackId":"2","trackName":"Song B","artistName":"Artist","trackViewUrl":"https://example.com/2"}""")
        }.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
        val reorderResponse = client.put("/playlists/$plId/tracks/reorder") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"trackIds":[$tr2,$tr1]}""")
        }
        assertEquals(HttpStatusCode.OK, reorderResponse.status)
        assertTrue(reorderResponse.bodyAsText().contains("Tracks reordered"))
    }

    @Test
    fun `test track deletion`() = testApplication {
        DatabaseConfig.init(); appSetup()
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Trk Del Group", "Group for track delete")
        val plResponse = client.post("/groups/$groupId/playlists") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"name":"Track Del","type":"group"}""")
        }
        val plId = plResponse.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
        val trResponse = client.post("/playlists/$plId/tracks") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"trackId":"99","trackName":"Delete Me","artistName":"Artist","trackViewUrl":"https://example.com/99"}""")
        }
        val trId = trResponse.bodyAsText().substringAfter(""""id":""").substringBefore(",").toInt()
        val deleteResponse = client.delete("/playlists/$plId/tracks/$trId") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)
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
