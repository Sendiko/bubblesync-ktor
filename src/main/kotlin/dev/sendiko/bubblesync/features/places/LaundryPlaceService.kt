package dev.sendiko.bubblesync.features.places

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import java.time.LocalDateTime
import java.util.UUID

data class LaundryPlace(
    val id: String,
    val name: String,
    val location: String,
    val contactNumber: String?,
    val createdAt: LocalDateTime,
)

class LaundryPlaceService(private val database: R2dbcDatabase) {
    suspend fun list(): List<LaundryPlace> = suspendTransaction(database) {
        LaundryPlaces.selectAll()
            .map { it.toLaundryPlace() }
            .toList()
    }

    suspend fun findById(id: String): LaundryPlace? = suspendTransaction(database) {
        LaundryPlaces.selectAll()
            .where { LaundryPlaces.id eq id }
            .map { it.toLaundryPlace() }
            .singleOrNull()
    }

    suspend fun create(name: String, location: String, contactNumber: String?): LaundryPlace = suspendTransaction(database) {
        val place = LaundryPlace(
            id = UUID.randomUUID().toString(),
            name = name,
            location = location,
            contactNumber = contactNumber,
            createdAt = LocalDateTime.now(),
        )
        LaundryPlaces.insert {
            it[id] = place.id
            it[LaundryPlaces.name] = place.name
            it[LaundryPlaces.location] = place.location
            it[LaundryPlaces.contactNumber] = place.contactNumber
            it[LaundryPlaces.createdAt] = place.createdAt
        }
        place
    }

    suspend fun update(
        id: String,
        name: String,
        location: String,
        contactNumber: String?,
    ): LaundryPlace? = suspendTransaction(database) {
        val updatedRows = LaundryPlaces.update({ LaundryPlaces.id eq id }) {
            it[LaundryPlaces.name] = name
            it[LaundryPlaces.location] = location
            it[LaundryPlaces.contactNumber] = contactNumber
        }
        if (updatedRows == 0) {
            null
        } else {
            LaundryPlaces.selectAll()
                .where { LaundryPlaces.id eq id }
                .map { it.toLaundryPlace() }
                .singleOrNull()
        }
    }

    suspend fun delete(id: String): Boolean = suspendTransaction(database) {
        LaundryPlaces.deleteWhere { LaundryPlaces.id eq id } > 0
    }

    private fun ResultRow.toLaundryPlace() = LaundryPlace(
        id = this[LaundryPlaces.id],
        name = this[LaundryPlaces.name],
        location = this[LaundryPlaces.location],
        contactNumber = this[LaundryPlaces.contactNumber],
        createdAt = this[LaundryPlaces.createdAt],
    )
}