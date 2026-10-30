package dev.sendiko.bubblesync.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import dev.sendiko.bubblesync.core.config.jwtSettings
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

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