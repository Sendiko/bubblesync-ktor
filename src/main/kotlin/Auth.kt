package dev.sendiko

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.Date

@Serializable
data class RegisterRequest(val email: String, val password: String, val name: String)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class UserResponse(val id: String, val email: String, val name: String)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserResponse,
)

@Serializable
data class AuthError(val error: String)

internal fun Route.authRoutes(userService: UserService, jwtSettings: JwtSettings) {
    route("/api/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val email = request.email.trim().lowercase()
            val name = request.name.trim()
            if (!EMAIL_PATTERN.matches(email) || email.length > 254) {
                call.respond(HttpStatusCode.BadRequest, AuthError("A valid email is required"))
                return@post
            }
            if (name.isBlank() || name.length > 100) {
                call.respond(HttpStatusCode.BadRequest, AuthError("Name must be between 1 and 100 characters"))
                return@post
            }
            if (request.password.length !in 8..128) {
                call.respond(HttpStatusCode.BadRequest, AuthError("Password must be between 8 and 128 characters"))
                return@post
            }
            if (userService.findByEmail(email) != null) {
                call.respond(HttpStatusCode.Conflict, AuthError("An account with this email already exists"))
                return@post
            }

            val account = userService.create(email, PasswordHasher.hash(request.password), name)
            call.respond(HttpStatusCode.Created, createAuthResponse(account, jwtSettings))
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            val email = request.email.trim().lowercase()
            if (request.password.length !in 8..128) {
                call.respond(HttpStatusCode.Unauthorized, AuthError("Invalid email or password"))
                return@post
            }
            val account = userService.findByEmail(email)
            if (account == null || !PasswordHasher.verify(request.password, account.passwordHash)) {
                call.respond(HttpStatusCode.Unauthorized, AuthError("Invalid email or password"))
                return@post
            }
            call.respond(HttpStatusCode.OK, createAuthResponse(account, jwtSettings))
        }

        authenticate {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()!!
                call.respond(
                    UserResponse(
                        id = principal.payload.subject!!,
                        email = principal.payload.getClaim("email").asString(),
                        name = principal.payload.getClaim("name").asString(),
                    )
                )
            }
        }
    }
}

private fun createAuthResponse(account: UserAccount, settings: JwtSettings): AuthResponse {
    val expiresAt = Instant.now().plusSeconds(settings.expiresInSeconds)
    val token = JWT.create()
        .withIssuer(settings.issuer)
        .withAudience(settings.audience)
        .withSubject(account.id)
        .withClaim("email", account.email)
        .withClaim("name", account.name)
        .withExpiresAt(Date.from(expiresAt))
        .sign(Algorithm.HMAC256(settings.secret))
    return AuthResponse(
        accessToken = token,
        tokenType = "Bearer",
        expiresIn = settings.expiresInSeconds,
        user = UserResponse(account.id, account.email, account.name),
    )
}

private val EMAIL_PATTERN = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")