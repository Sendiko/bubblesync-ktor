package dev.sendiko.bubblesync.features.records

import dev.sendiko.bubblesync.features.places.LaundryPlaces
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

data class LaundryRecord(
    val id: String,
    val userId: String,
    val placeId: String,
    val dropOffDateTime: LocalDateTime,
    val amountPaid: BigDecimal,
    val status: String,
    val bulkPhotoUrl: String?,
    val items: List<LaundryRecordItem>,
    val createdAt: LocalDateTime,
)

data class LaundryRecordItem(
    val id: String,
    val name: String,
    val quantity: Int,
    val itemPhotoUrl: String?,
)

data class NewLaundryRecord(
    val placeId: String,
    val dropOffDateTime: LocalDateTime,
    val amountPaid: BigDecimal,
    val bulkPhotoUrl: String?,
    val items: List<NewLaundryRecordItem>,
)

data class NewLaundryRecordItem(
    val name: String,
    val quantity: Int,
    val itemPhotoUrl: String?,
)

class LaundryRecordService(private val database: R2dbcDatabase) {
    suspend fun listForUser(userId: String): List<LaundryRecord> = suspendTransaction(database) {
        val recordRows = LaundryRecords.selectAll()
            .where { LaundryRecords.userId eq userId }
            .toList()
        val records = mutableListOf<LaundryRecord>()
        for (row in recordRows) {
            val itemRows = LaundryItems.selectAll()
                .where { LaundryItems.recordId eq row[LaundryRecords.id] }
                .toList()
            records += row.toLaundryRecord(itemRows)
        }
        records
    }

    suspend fun findById(userId: String, recordId: String): LaundryRecord? = suspendTransaction(database) {
        val row = LaundryRecords.selectAll()
            .where { (LaundryRecords.id eq recordId) and (LaundryRecords.userId eq userId) }
            .singleOrNull() ?: return@suspendTransaction null
        val itemRows = LaundryItems.selectAll()
            .where { LaundryItems.recordId eq recordId }
            .toList()
        row.toLaundryRecord(itemRows)
    }

    suspend fun create(userId: String, input: NewLaundryRecord): LaundryRecord? = suspendTransaction(database) {
        val placeExists = LaundryPlaces.selectAll()
            .where { LaundryPlaces.id eq input.placeId }
            .singleOrNull() != null
        if (!placeExists) return@suspendTransaction null

        val record = LaundryRecord(
            id = UUID.randomUUID().toString(),
            userId = userId,
            placeId = input.placeId,
            dropOffDateTime = input.dropOffDateTime,
            amountPaid = input.amountPaid,
            status = "DROPPED_OFF",
            bulkPhotoUrl = input.bulkPhotoUrl,
            items = input.items.map {
                LaundryRecordItem(
                    id = UUID.randomUUID().toString(),
                    name = it.name,
                    quantity = it.quantity,
                    itemPhotoUrl = it.itemPhotoUrl,
                )
            },
            createdAt = LocalDateTime.now(),
        )
        LaundryRecords.insert {
            it[id] = record.id
            it[LaundryRecords.userId] = record.userId
            it[LaundryRecords.placeId] = record.placeId
            it[LaundryRecords.dropOffDateTime] = record.dropOffDateTime
            it[LaundryRecords.amountPaid] = record.amountPaid
            it[LaundryRecords.status] = record.status
            it[LaundryRecords.bulkPhotoUrl] = record.bulkPhotoUrl
            it[LaundryRecords.createdAt] = record.createdAt
        }
        record.items.forEach { item ->
            LaundryItems.insert {
                it[id] = item.id
                it[LaundryItems.recordId] = record.id
                it[LaundryItems.name] = item.name
                it[LaundryItems.quantity] = item.quantity
                it[LaundryItems.itemPhotoUrl] = item.itemPhotoUrl
            }
        }
        record
    }

    suspend fun updateStatus(userId: String, recordId: String, status: String): LaundryRecord? = suspendTransaction(database) {
        val updatedRows = LaundryRecords.update({
            (LaundryRecords.id eq recordId) and (LaundryRecords.userId eq userId)
        }) {
            it[LaundryRecords.status] = status
        }
        if (updatedRows == 0) {
            null
        } else {
            val row = LaundryRecords.selectAll()
                .where { (LaundryRecords.id eq recordId) and (LaundryRecords.userId eq userId) }
                .singleOrNull() ?: return@suspendTransaction null
            val itemRows = LaundryItems.selectAll()
                .where { LaundryItems.recordId eq recordId }
                .toList()
            row.toLaundryRecord(itemRows)
        }
    }

    suspend fun delete(userId: String, recordId: String): Boolean = suspendTransaction(database) {
        LaundryRecords.deleteWhere {
            (LaundryRecords.id eq recordId) and (LaundryRecords.userId eq userId)
        } > 0
    }

    private fun ResultRow.toLaundryRecord(itemRows: List<ResultRow>) = LaundryRecord(
        id = this[LaundryRecords.id],
        userId = this[LaundryRecords.userId],
        placeId = this[LaundryRecords.placeId],
        dropOffDateTime = this[LaundryRecords.dropOffDateTime],
        amountPaid = this[LaundryRecords.amountPaid],
        status = this[LaundryRecords.status],
        bulkPhotoUrl = this[LaundryRecords.bulkPhotoUrl],
        items = itemRows.map { itemRow ->
            LaundryRecordItem(
                id = itemRow[LaundryItems.id],
                name = itemRow[LaundryItems.name],
                quantity = itemRow[LaundryItems.quantity],
                itemPhotoUrl = itemRow[LaundryItems.itemPhotoUrl],
            )
        },
        createdAt = this[LaundryRecords.createdAt],
    )
}