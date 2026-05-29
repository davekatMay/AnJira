package com.example

import com.example.dao.TaskEntity
import com.example.dao.UserEntity
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

private val logger = LoggerFactory.getLogger("TaskRoutesTest")

class TaskRoutesTest {

    private lateinit var testApplication: TestApplication
    private val shutdownLatch = CountDownLatch(1)
    private var authToken: String? = null
    private var userId: Int? = null
    private var groupId: Int? = null

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
        
        // Create a test user and group for testing
        createTestUserAndGroup()
    }

    @AfterEach
    fun tearDown() {
        testApplication.stop()
        shutdownLatch.countDown()
    }

    private fun createTestUserAndGroup() {
        // Register a user
        val registerResponse = testApplication.client.postForm(
            "/auth/register",
            paramsMapOf(
                "username" to "testuser",
                "email" to "test@example.com",
                "password" to "testpassword"
            )
        )
        
        assertEquals(io.ktor.http.HttpStatusCode.Created, registerResponse.status)
        val authResponse = registerResponse.body<AuthResponse>()
        authToken = authResponse!!.token
        userId = authResponse.userId

        // Create a group
        val groupResponse = testApplication.client.post(
            "/groups",
            headers = appendHeader(io.ktor.http.HttpHeaders.Authorization, "Bearer $authToken"),
            body = """
                {
                    "name": "Test Group",
                    "description": "Test group for task testing"
                }
            """.trimIndent(),
            contentType = io.ktor.http.ContentType.Application.Json
        )
        
        assertEquals(io.ktor.http.HttpStatusCode.OK, groupResponse.status)
        val group = groupResponse.body<GroupResponse>()
        groupId = group!!.id
    }

    @Test
    fun `test task creation and retrieval`() {
        // Create a task
        val createResponse = testApplication.client.post(
            "/groups/$groupId/tasks",
            headers = appendHeader(io.ktor.http.HttpHeaders.Authorization, "Bearer $authToken"),
            body = """
                {
                    "title": "Test Task",
                    "description": "Test task description",
                    "assignedTo": $userId
                }
            """.trimIndent(),
            contentType = io.ktor.http.ContentType.Application.Json
        )
        
        assertEquals(io.ktor.http.HttpStatusCode.OK, createResponse.status)
        val createdTask = createResponse.body<TaskResponse>()
        assertNotNull(createdTask)
        assertEquals("Test Task", createdTask.title)
        assertEquals("Test task description", createdTask.description)
        assertEquals("todo", createdTask.status)
        assertEquals(userId, createdTask.assignedTo)

        // Get tasks for the group
        val getResponse = testApplication.client.get(
            "/groups/$groupId/tasks",
            headers = appendHeader(io.ktor.http.HttpHeaders.Authorization, "Bearer $authToken")
        )
        
        assertEquals(io.ktor.http.HttpStatusCode.OK, getResponse.status)
        val tasks = getResponse.body<List<TaskResponse>>()
        assertNotNull(tasks)
        assertEquals(1, tasks.size)
        assertEquals("Test Task", tasks[0].title)
    }

    @Test
    fun `test task update`() {
        // Create a task
        val createResponse = testApplication.client.post(
            "/groups/$groupId/tasks",
            headers = appendHeader(io.ktor.http.HttpHeaders.Authorization, "Bearer $authToken"),
            body = """
                {
                    "title": "Test Task",
                    "description": "Test task description"
                }
            """.trimIndent(),
            contentType = io.ktor.http.ContentType.Application.Json
        )
        
        val createdTask = createResponse.body<TaskResponse>()
        val taskId = createdTask!!.id

        // Update the task
        val updateResponse = testApplication.client.put(
            "/tasks/$taskId",
            headers = appendHeader(io.ktor.http.HttpHeaders.Authorization, "Bearer $authToken"),
            body = """
                {
                    "title": "Updated Task",
                    "status": "in_progress"
                }
            """.trimIndent(),
            contentType = io.ktor.http.ContentType.Application.Json
        )
        
        assertEquals(io.ktor.http.HttpStatusCode.OK, updateResponse.status)
        val updatedTask = updateResponse.body<TaskResponse>()
        assertNotNull(updatedTask)
        assertEquals("Updated Task", updatedTask.title)
        assertEquals("in_progress", updatedTask.status)

        // Verify the update
        val getResponse = testApplication.client.get(
            "/groups/$groupId/tasks",
            headers = appendHeader(io.ktor.http.HttpHeaders.Authorization, "Bearer $authToken")
        )
        
        val tasks = getResponse.body<List<TaskResponse>>()
        assertEquals(1, tasks.size)
        assertEquals("Updated Task", tasks[0].title)
        assertEquals("in_progress", tasks[0].status)
    }

    @Test
    fun `test task deletion`() {
        // Create a task
        val createResponse = testApplication.client.post(
            "/groups/$groupId/tasks",
            headers = appendHeader(io.ktor.http.HttpHeaders.Authorization, "Bearer $authToken"),
            body = """
                {
                    "title": "Test Task",
                    "description": "Test task description"
                }
            """.trimIndent(),
            contentType = io.ktor.http.ContentType.Application.Json
        )
        
        val createdTask = createResponse.body<TaskResponse>()
        val taskId = createdTask!!.id

        // Delete the task
        val deleteResponse = testApplication.client.delete(
            "/tasks/$taskId",
            headers = appendHeader(io.ktor.http.HttpHeaders.Authorization, "Bearer $authToken")
        )
        
        assertEquals(io.ktor.http.HttpStatusCode.NoContent, deleteResponse.status)

        // Verify the task is deleted
        val getResponse = testApplication.client.get(
            "/groups/$groupId/tasks",
            headers = appendHeader(io.ktor.http.HttpHeaders.Authorization, "Bearer $authToken")
        )
        
        val tasks = getResponse.body<List<TaskResponse>>()
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun `test subtask creation and completion`() {
        // Create a task
        val createResponse = testApplication.client.post(
            "/groups/$groupId/tasks",
            headers = appendHeader(io.ktor.http.HttpHeaders.Authorization, "Bearer $authToken"),
            body = """
                {
                    "title": "Test Task",
                    "description": "Test task description"
                }
            """.trimIndent(),
            contentType = io.ktor.http.ContentType.Application.Json
        )
        
        val createdTask = createResponse.body<TaskResponse>()
        val taskId = createdTask!!.id

        // Create a subtask
        val subtaskCreateResponse = testApplication.client.post(
            "/tasks/$taskId/subtasks",
            headers = appendHeader(io.ktor.http.HttpHeaders.Authorization, "Bearer $authToken"),
            body = """
                {
                    "title": "Test Subtask"
                }
            """.trimIndent(),
            contentType = io.ktor.http.ContentType.Application.Json
        )
        
        assertEquals(io.ktor.http.HttpStatusCode.OK, subtaskCreateResponse.status)
        val createdSubtask = subtaskCreateResponse.body<SubtaskResponse>()
        assertNotNull(createdSubtask)
        assertEquals("Test Subtask", createdSubtask.title)
        assertFalse(createdSubtask.isCompleted)

        // Update subtask completion
        val subtaskUpdateResponse = testApplication.client.put(
            "/subtasks/${createdSubtask.id}",
            headers = appendHeader(io.ktor.http.HttpHeaders.Authorization, "Bearer $authToken"),
            queryParameters = mapOf("isCompleted" to "true")
        )
        
        assertEquals(io.ktor.http.HttpStatusCode.OK, subtaskUpdateResponse.status)
        val updatedSubtask = subtaskUpdateResponse.body<SubtaskResponse>()
        assertNotNull(updatedSubtask)
        assertTrue(updatedSubtask.isCompleted)

        // Get subtasks for the task
        val subtasksResponse = testApplication.client.get(
            "/tasks/$taskId/subtasks",
            headers = appendHeader(io.ktor.http.HttpHeaders.Authorization, "Bearer $authToken")
        )
        
        val subtasks = subtasksResponse.body<List<SubtaskResponse>>()
        assertEquals(1, subtasks.size)
        assertTrue(subtasks[0].isCompleted)
    }

    // Data classes for authentication response
    data class AuthResponse(val token: String, val userId: Int, val username: String)
    
    // Data classes for task response
    data class TaskResponse(
        val id: Int,
        val title: String,
        val description: String?,
        val status: String,
        val createdBy: Int,
        val assignedTo: Int?,
        val createdAt: String,
        val updatedAt: String
    )
    
    // Data classes for group response
    data class GroupResponse(
        val id: Int,
        val name: String,
        val description: String?,
        val createdBy: Int,
        val createdAt: String,
        val updatedAt: String
    )
    
    // Data classes for subtask response
    data class SubtaskResponse(
        val id: Int,
        val title: String,
        val isCompleted: Boolean,
        val createdAt: String,
        val updatedAt: String
    )
}