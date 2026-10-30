package dev.sendiko.bubblesync.features.records

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Serializable
data class CreateLaundryRecordRequest(
    val placeId: String,
    val dropOffDateTime: String,
    val amountPaid: String,
    val bulkPhotoUrl: String? = null,
    val items: List<CreateLaundryItemRequest> = emptyList(),
)

@Serializable
data class CreateLaundryItemRequest(
    val name: String,
    val quantity: Int = 1,
    val itemPhotoUrl: String? = null,
)

@Serializable
data class UpdateLaundryRecordStatusRequest(val status: String)

@Serializable
data class LaundryRecordResponse(
    val id: String,
    val userId: String,
    val placeId: String,
    val dropOffDateTime: String,
    val amountPaid: String,
    val status: String,
    val bulkPhotoUrl: String?,
    val items: List<LaundryRecordItemResponse>,
    val createdAt: String,
)

@Serializable
data class LaundryRecordItemResponse(
    val id: String,
    val name: String,
    val quantity: Int,
    val itemPhotoUrl: String?,
)

@Serializable
data class LaundryRecordError(val error: String)

private val ALLOWED_STATUSES = setOf("DROPPED_OFF", "WASHING", "READY", "PICKED_UP")

internal fun Route.laundryRecordRoutes(service: LaundryRecordService) {
    authenticate {
        route("/api/records") {
            get {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject!!
                call.respond(service.listForUser(userId).map { it.toResponse() })
            }

            get("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject!!
                val record = service.findById(userId, call.parameters["id"]!!)
                if (record == null) {
                    call.respond(HttpStatusCode.NotFound, LaundryRecordError("Laundry record not found"))
                    return@get
                }
                call.respond(record.toResponse())
            }

            post {
                val request = call.receive<CreateLaundryRecordRequest>()
                val input = request.toNewLaundryRecordOrNull()
                if (input == null) {
                    call.respond(HttpStatusCode.BadRequest, LaundryRecordError("Invalid laundry record details"))
                    return@post
                }
                val userId = call.principal<JWTPrincipal>()!!.payload.subject!!
                val record = service.create(userId, input)
                if (record == null) {
                    call.respond(HttpStatusCode.NotFound, LaundryRecordError("Laundry place not found"))
                    return@post
                }
                call.respond(HttpStatusCode.Created, record.toResponse())
            }

            patch("/{id}/status") {
                val request = call.receive<UpdateLaundryRecordStatusRequest>()
                if (request.status !in ALLOWED_STATUSES) {
                    call.respond(HttpStatusCode.BadRequest, LaundryRecordError("Invalid laundry record status"))
                    return@patch
                }
                val userId = call.principal<JWTPrincipal>()!!.payload.subject!!
                val record = service.updateStatus(userId, call.parameters["id"]!!, request.status)
                if (record == null) {
                    call.respond(HttpStatusCode.NotFound, LaundryRecordError("Laundry record not found"))
                    return@patch
                }
                call.respond(record.toResponse())
            }

            delete("/{id}") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject!!
                val deleted = service.delete(userId, call.parameters["id"]!!)
                if (!deleted) {
                    call.respond(HttpStatusCode.NotFound, LaundryRecordError("Laundry record not found"))
                    return@delete
                }
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private fun CreateLaundryRecordRequest.toNewLaundryRecordOrNull(): NewLaundryRecord? {
    val normalizedPlaceId = placeId.trim()
    val dropOff = runCatching { LocalDateTime.parse(dropOffDateTime) }.getOrNull() ?: return null
    val amount = runCatching {
        BigDecimal(amountPaid.trim()).setScale(2, RoundingMode.UNNECESSARY)
    }.getOrNull() ?: return null
    if (normalizedPlaceId.isEmpty() || amount.signum() < 0 || amount.precision() > 10) return null
    if (items.size > 100) return null

    val normalizedBulkPhotoUrl = bulkPhotoUrl?.trim()?.takeIf { it.isNotEmpty() }
    if (normalizedBulkPhotoUrl != null && normalizedBulkPhotoUrl.length > 2048) return null

    val normalizedItems = items.map { item ->
        val name = item.name.trim()
        val itemPhotoUrl = item.itemPhotoUrl?.trim()?.takeIf { it.isNotEmpty() }
        if (name.isBlank() || name.length > 255 || item.quantity !in 1..1000) return null
        if (itemPhotoUrl != null && itemPhotoUrl.length > 2048) return null
        NewLaundryRecordItem(name, item.quantity, itemPhotoUrl)
    }

    return NewLaundryRecord(
        placeId = normalizedPlaceId,
        dropOffDateTime = dropOff,
        amountPaid = amount,
        bulkPhotoUrl = normalizedBulkPhotoUrl,
        items = normalizedItems,
    )
}

private fun LaundryRecord.toResponse() = LaundryRecordResponse(
    id = id,
    userId = userId,
    placeId = placeId,
    dropOffDateTime = dropOffDateTime.toString(),
    amountPaid = amountPaid.toPlainString(),
    status = status,
    bulkPhotoUrl = bulkPhotoUrl,
    items = items.map { it.toResponse() },
    createdAt = createdAt.toString(),
)

private fun LaundryRecordItem.toResponse() = LaundryRecordItemResponse(
    id = id,
    name = name,
    quantity = quantity,
    itemPhotoUrl = itemPhotoUrl,
)