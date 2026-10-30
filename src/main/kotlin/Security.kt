package dev.sendiko

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

internal data class JwtSettings(
    val secret: String,
    val issuer: String,
    val audience: String,
    val realm: String,
    val expiresInSeconds: Long,
)

internal fun Application.jwtSettings(): JwtSettings {
    val config = environment.config
    val secret = System.getenv("JWT_SECRET") ?: config.property("jwt.secret").getString()
    require(secret.length >= 32) { "JWT_SECRET must be at least 32 characters" }
    return JwtSettings(
        secret = secret,
        issuer = System.getenv("JWT_ISSUER") ?: config.property("jwt.issuer").getString(),
        audience = System.getenv("JWT_AUDIENCE") ?: config.property("jwt.audience").getString(),
        realm = config.property("jwt.realm").getString(),
        expiresInSeconds = config.property("jwt.expiresInSeconds").getString().toLong(),
    )
}

fun Application.configureSecurity() {
    val settings = jwtSettings()
    install(Authentication) {
        jwt {
            realm = settings.realm
            verifier(
                JWT.require(Algorithm.HMAC256(settings.secret))
                    .withAudience(settings.audience)
                    .withIssuer(settings.issuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.subject != null && settings.audience in credential.payload.audience) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}