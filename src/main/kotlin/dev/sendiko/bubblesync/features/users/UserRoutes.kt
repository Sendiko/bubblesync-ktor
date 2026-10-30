package dev.sendiko.bubblesync.features.users

import dev.sendiko.bubblesync.features.auth.UserAccount
import dev.sendiko.bubblesync.features.auth.UserService
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable

@Serializable
data class UserResponse(val id: String, val email: String, val name: String)

@Serializable
data class UpdateUserRequest(val name: String)

@Serializable
data class UserError(val error: String)

internal fun Route.userRoutes(userService: UserService) {
    authenticate {
        route("/api/users") {
            get("/me") {
                val userId = call.principal<JWTPrincipal>()!!.payload.subject!!
                val user = userService.findById(userId)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, UserError("User not found"))
                    return@get
                }
                call.respond(user.toResponse())
            }

            patch("/me") {
                val request = call.receive<UpdateUserRequest>()
                val name = request.name.trim()
                if (name.isBlank() || name.length > 100) {
                    call.respond(HttpStatusCode.BadRequest, UserError("Name must be between 1 and 100 characters"))
                    return@patch
                }

                val userId = call.principal<JWTPrincipal>()!!.payload.subject!!
                val user = userService.updateName(userId, name)
                if (user == null) {
                    call.respond(HttpStatusCode.NotFound, UserError("User not found"))
                    return@patch
                }
                call.respond(user.toResponse())
            }
        }
    }
}

private fun UserAccount.toResponse() = UserResponse(id = id, email = email, name = name)