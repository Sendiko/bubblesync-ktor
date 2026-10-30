package dev.sendiko.bubblesync.features.places

import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class LaundryPlaceRequest(
    val name: String,
    val location: String,
    val contactNumber: String? = null,
)

@Serializable
data class LaundryPlaceResponse(
    val id: String,
    val name: String,
    val location: String,
    val contactNumber: String?,
    val createdAt: String,
)

@Serializable
data class LaundryPlaceError(val error: String)

internal fun Route.laundryPlaceRoutes(service: LaundryPlaceService) {
    authenticate {
        route("/api/places") {
            get {
                call.respond(service.list().map { it.toResponse() })
            }

            get("/{id}") {
                val place = service.findById(call.parameters["id"]!!)
                if (place == null) {
                    call.respond(HttpStatusCode.NotFound, LaundryPlaceError("Laundry place not found"))
                    return@get
                }
                call.respond(place.toResponse())
            }

            post {
                val request = call.receive<LaundryPlaceRequest>()
                val values = request.validatedValues()
                if (values == null) {
                    call.respond(HttpStatusCode.BadRequest, LaundryPlaceError("Invalid laundry place details"))
                    return@post
                }
                val place = service.create(values.name, values.location, values.contactNumber)
                call.respond(HttpStatusCode.Created, place.toResponse())
            }

            put("/{id}") {
                val request = call.receive<LaundryPlaceRequest>()
                val values = request.validatedValues()
                if (values == null) {
                    call.respond(HttpStatusCode.BadRequest, LaundryPlaceError("Invalid laundry place details"))
                    return@put
                }
                val place = service.update(
                    id = call.parameters["id"]!!,
                    name = values.name,
                    location = values.location,
                    contactNumber = values.contactNumber,
                )
                if (place == null) {
                    call.respond(HttpStatusCode.NotFound, LaundryPlaceError("Laundry place not found"))
                    return@put
                }
                call.respond(place.toResponse())
            }

            delete("/{id}") {
                val deleted = service.delete(call.parameters["id"]!!)
                if (!deleted) {
                    call.respond(HttpStatusCode.NotFound, LaundryPlaceError("Laundry place not found"))
                    return@delete
                }
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

private data class ValidatedLaundryPlace(
    val name: String,
    val location: String,
    val contactNumber: String?,
)

private fun LaundryPlaceRequest.validatedValues(): ValidatedLaundryPlace? {
    val normalizedName = name.trim()
    val normalizedLocation = location.trim()
    val normalizedContactNumber = contactNumber?.trim()?.takeIf { it.isNotEmpty() }
    if (normalizedName.isBlank() || normalizedName.length > 255) return null
    if (normalizedLocation.isBlank() || normalizedLocation.length > 1000) return null
    if (normalizedContactNumber != null && normalizedContactNumber.length > 50) return null
    return ValidatedLaundryPlace(normalizedName, normalizedLocation, normalizedContactNumber)
}

private fun LaundryPlace.toResponse() = LaundryPlaceResponse(
    id = id,
    name = name,
    location = location,
    contactNumber = contactNumber,
    createdAt = createdAt.toString(),
)