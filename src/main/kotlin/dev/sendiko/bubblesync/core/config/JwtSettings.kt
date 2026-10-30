package dev.sendiko.bubblesync.core.config

import io.ktor.server.application.*

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