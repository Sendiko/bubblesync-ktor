package dev.sendiko.bubblesync

import dev.sendiko.bubblesync.features.auth.AuthResponse
import dev.sendiko.bubblesync.features.places.LaundryPlaceResponse
import dev.sendiko.bubblesync.features.records.LaundryRecordResponse
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

        val createPlace = client.post("/api/places") {
            header(HttpHeaders.Authorization, "Bearer ${registeredAccount.accessToken}")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Spin Cycle","location":"42 Main Street","contactNumber":"555-0100"}""")
        }
        assertEquals(HttpStatusCode.Created, createPlace.status)
        val place = Json.decodeFromString<LaundryPlaceResponse>(createPlace.bodyAsText())

        val getPlace = client.get("/api/places/${place.id}") {
            header(HttpHeaders.Authorization, "Bearer ${registeredAccount.accessToken}")
        }
        assertEquals(HttpStatusCode.OK, getPlace.status)

        val updatePlace = client.put("/api/places/${place.id}") {
            header(HttpHeaders.Authorization, "Bearer ${registeredAccount.accessToken}")
            contentType(ContentType.Application.Json)
            setBody("""{"name":"Spin Cycle Plus","location":"44 Main Street"}""")
        }
        assertEquals(HttpStatusCode.OK, updatePlace.status)
        assertEquals("Spin Cycle Plus", Json.decodeFromString<LaundryPlaceResponse>(updatePlace.bodyAsText()).name)

        val createRecord = client.post("/api/records") {
            header(HttpHeaders.Authorization, "Bearer ${registeredAccount.accessToken}")
            contentType(ContentType.Application.Json)
            setBody(
                """{"placeId":"${place.id}","dropOffDateTime":"2026-10-30T12:15:00","amountPaid":"25.50","items":[{"name":"Shirt","quantity":2},{"name":"Towel"}]}"""
            )
        }
        assertEquals(HttpStatusCode.Created, createRecord.status)
        val record = Json.decodeFromString<LaundryRecordResponse>(createRecord.bodyAsText())
        assertEquals("DROPPED_OFF", record.status)
        assertEquals(2, record.items.size)
        assertEquals("25.50", record.amountPaid)

        val getRecord = client.get("/api/records/${record.id}") {
            header(HttpHeaders.Authorization, "Bearer ${registeredAccount.accessToken}")
        }
        assertEquals(HttpStatusCode.OK, getRecord.status)
        assertEquals(2, Json.decodeFromString<LaundryRecordResponse>(getRecord.bodyAsText()).items.size)

        val records = client.get("/api/records") {
            header(HttpHeaders.Authorization, "Bearer ${registeredAccount.accessToken}")
        }
        assertEquals(1, Json.decodeFromString<List<LaundryRecordResponse>>(records.bodyAsText()).size)

        val invalidStatus = client.patch("/api/records/${record.id}/status") {
            header(HttpHeaders.Authorization, "Bearer ${registeredAccount.accessToken}")
            contentType(ContentType.Application.Json)
            setBody("""{"status":"UNKNOWN"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, invalidStatus.status)

        val updateRecord = client.patch("/api/records/${record.id}/status") {
            header(HttpHeaders.Authorization, "Bearer ${registeredAccount.accessToken}")
            contentType(ContentType.Application.Json)
            setBody("""{"status":"WASHING"}""")
        }
        assertEquals(HttpStatusCode.OK, updateRecord.status)
        assertEquals("WASHING", Json.decodeFromString<LaundryRecordResponse>(updateRecord.bodyAsText()).status)

        val deleteRecord = client.delete("/api/records/${record.id}") {
            header(HttpHeaders.Authorization, "Bearer ${registeredAccount.accessToken}")
        }
        assertEquals(HttpStatusCode.NoContent, deleteRecord.status)

        val deletePlace = client.delete("/api/places/${place.id}") {
            header(HttpHeaders.Authorization, "Bearer ${registeredAccount.accessToken}")
        }
        assertEquals(HttpStatusCode.NoContent, deletePlace.status)
    }
}