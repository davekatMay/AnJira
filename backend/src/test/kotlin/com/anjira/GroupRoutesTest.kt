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

class GroupRoutesTest {

    private fun uniqueEmail(): String { counter++; return "grp$counter@example.com" }

    @Test
    fun `test group creation`() = testApplication {
        DatabaseConfig.init()
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
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val response = client.post("/groups") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"name":"New Group","description":"A test group","avatar":"https://example.com/avatar.png"}""")
        }
        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("New Group"))
    }

    @Test
    fun `test get user groups`() = testApplication {
        DatabaseConfig.init()
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
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        createGroup(client, token, "Group A", null)
        createGroup(client, token, "Group B", null)
        val response = client.get("/groups") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, response.status)
        val text = response.bodyAsText()
        assertTrue(text.contains("Group A"))
        assertTrue(text.contains("Group B"))
    }

    @Test
    fun `test get group detail`() = testApplication {
        DatabaseConfig.init()
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
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Detail Group", "Group for detail test")
        val response = client.get("/groups/$groupId") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Detail Group"))
    }

    @Test
    fun `test group update`() = testApplication {
        DatabaseConfig.init()
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
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Original Name", null)
        val response = client.put("/groups/$groupId") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"name":"Updated Name","description":"New description"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("Updated Name"))
    }

    @Test
    fun `test group deletion`() = testApplication {
        DatabaseConfig.init()
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
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token, "Delete Me", null)
        val response = client.delete("/groups/$groupId") { header(HttpHeaders.Authorization, "Bearer $token") }
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `test join group by invite code`() = testApplication {
        DatabaseConfig.init()
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
        val token1 = registerAndGetToken(client, uniqueEmail(), "password")
        val groupId = createGroup(client, token1, "Invite Group", null)
        val groupResponse = client.get("/groups/$groupId") { header(HttpHeaders.Authorization, "Bearer $token1") }
        val inviteCode = groupResponse.bodyAsText().substringAfter(""""inviteCode":"""").substringBefore("""","""")
        val token2 = registerAndGetToken(client, uniqueEmail(), "password")
        val joinResponse = client.post("/groups/join") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token2")
            setBody("""{"inviteCode":"$inviteCode"}""")
        }
        assertEquals(HttpStatusCode.OK, joinResponse.status)
        assertTrue(joinResponse.bodyAsText().contains("Invite Group"))
    }

    @Test
    fun `test join with invalid invite code`() = testApplication {
        DatabaseConfig.init()
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
        val token = registerAndGetToken(client, uniqueEmail(), "password")
        val response = client.post("/groups/join") {
            contentType(ContentType.Application.Json); header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"inviteCode":"INVALID"}""")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
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
        @JvmStatic @BeforeAll
        fun initEnv() { System.setProperty("JWT_SECRET", "test-secret-key-for-tests") }
    }
}
