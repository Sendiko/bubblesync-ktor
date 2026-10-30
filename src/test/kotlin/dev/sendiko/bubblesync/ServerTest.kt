package dev.sendiko.bubblesync

import dev.sendiko.bubblesync.features.auth.AuthResponse
import dev.sendiko.bubblesync.features.users.UserResponse
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.*
import java.util.UUID

class ServerTest {

    @Test
    fun `test root endpoint`() = testApplication {
        configure()
        assertEquals(HttpStatusCode.OK, client.get("/").status)
    }

    @Test
    fun `register login and access protected profile`() = testApplication {
        configure()
        val email = "${UUID.randomUUID()}@example.com"
        val registerBody = """{"email":"$email","password":"correct-horse-battery","name":"Test User"}"""

        val registration = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(registerBody)
        }
        assertEquals(HttpStatusCode.Created, registration.status)
        val registeredAccount = Json.decodeFromString<AuthResponse>(registration.bodyAsText())

        val duplicate = client.post("/api/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(registerBody)
        }
        assertEquals(HttpStatusCode.Conflict, duplicate.status)

        val invalidLogin = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"wrong-password"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, invalidLogin.status)

        val login = client.post("/api/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"correct-horse-battery"}""")
        }
        assertEquals(HttpStatusCode.OK, login.status)

        val profile = client.get("/api/users/me") {
            header(HttpHeaders.Authorization, "Bearer ${registeredAccount.accessToken}")
        }
        assertEquals(HttpStatusCode.OK, profile.status)

        val invalidUpdate = client.patch("/api/users/me") {
            header(HttpHeaders.Authorization, "Bearer ${registeredAccount.accessToken}")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"  "}""")
        }
        assertEquals(HttpStatusCode.BadRequest, invalidUpdate.status)

        val update = client.patch("/api/users/me") {
            header(HttpHeaders.Authorization, "Bearer ${registeredAccount.accessToken}")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Updated User"}""")
        }
        assertEquals(HttpStatusCode.OK, update.status)
        assertEquals("Updated User", Json.decodeFromString<UserResponse>(update.bodyAsText()).name)
    }
}