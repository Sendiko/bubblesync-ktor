package dev.sendiko.bubblesync.plugins

import dev.sendiko.bubblesync.core.config.jwtSettings
import dev.sendiko.bubblesync.features.auth.UserService
import dev.sendiko.bubblesync.features.auth.authRoutes
import dev.sendiko.bubblesync.features.places.LaundryPlaceService
import dev.sendiko.bubblesync.features.places.laundryPlaceRoutes
import dev.sendiko.bubblesync.features.records.LaundryRecordService
import dev.sendiko.bubblesync.features.records.laundryRecordRoutes
import dev.sendiko.bubblesync.features.users.userRoutes
import io.ktor.server.application.*
import io.ktor.server.routing.routing
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase

suspend fun Application.configureDatabases() {
    val config = environment.config
    val databaseHost = environmentVariableOrConfig("MYSQL_HOST", config.property("database.host").getString())
    val databasePort = environmentVariableOrConfig("MYSQL_PORT", config.property("database.port").getString())
    val databaseName = environmentVariableOrConfig("MYSQL_DATABASE", config.property("database.name").getString())
    val databaseUser = environmentVariableOrConfig("MYSQL_USER", config.property("database.user").getString())
    val databasePassword = environmentVariableOrConfig("MYSQL_PASSWORD", config.property("database.password").getString())
    val database = R2dbcDatabase.connect(
        url = "r2dbc:mysql://$databaseHost:$databasePort/$databaseName",
        user = databaseUser,
        password = databasePassword,
    )
    val userService = UserService(database).also {
        it.createSchema()
    }
    val laundryPlaceService = LaundryPlaceService(database)
    val laundryRecordService = LaundryRecordService(database)
    routing {
        authRoutes(userService, jwtSettings())
        userRoutes(userService)
        laundryPlaceRoutes(laundryPlaceService)
        laundryRecordRoutes(laundryRecordService)
    }
}

private fun environmentVariableOrConfig(name: String, fallback: String): String =
    System.getenv(name)?.takeIf { it.isNotBlank() } ?: fallback