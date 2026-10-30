package dev.sendiko

import io.ktor.server.application.*
import io.ktor.server.routing.routing
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

suspend fun Application.configureExposed() {
    val config = environment.config
    val databaseHost = System.getenv("MYSQL_HOST") ?: config.property("database.host").getString()
    val databasePort = System.getenv("MYSQL_PORT") ?: config.property("database.port").getString()
    val databaseName = System.getenv("MYSQL_DATABASE") ?: config.property("database.name").getString()
    val databaseUser = System.getenv("MYSQL_USER") ?: config.property("database.user").getString()
    val databasePassword = System.getenv("MYSQL_PASSWORD") ?: config.property("database.password").getString()
    val database = R2dbcDatabase.connect(
        url = "r2dbc:mysql://$databaseHost:$databasePort/$databaseName",
        user = databaseUser,
        password = databasePassword,
    )
    val userService = UserService(database).also {
        it.createSchema()
    }
    routing {
        authRoutes(userService, jwtSettings())
    }
}
