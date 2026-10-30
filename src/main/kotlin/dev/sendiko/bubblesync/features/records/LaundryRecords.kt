package dev.sendiko.bubblesync.features.records

import dev.sendiko.bubblesync.features.auth.UserService
import dev.sendiko.bubblesync.features.places.LaundryPlaces
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime

object LaundryRecords : Table("laundry_records") {
    val id = varchar("id", length = 36).defaultExpression(CustomFunction("UUID", VarCharColumnType(36)))
    val userId = reference("user_id", UserService.Users.id)
    val placeId = reference("place_id", LaundryPlaces.id)
    val dropOffDateTime = datetime("drop_off_date_time")
    val amountPaid = decimal("amount_paid", precision = 10, scale = 2)
    val status = varchar("status", length = 50)
    val bulkPhotoUrl = varchar("bulk_photo_url", length = 2048).nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

object LaundryItems : Table("laundry_items") {
    val id = varchar("id", length = 36).defaultExpression(CustomFunction("UUID", VarCharColumnType(36)))
    val recordId = reference("record_id", LaundryRecords.id, onDelete = ReferenceOption.CASCADE)
    val name = varchar("name", length = 255)
    val quantity = integer("quantity").default(1)
    val itemPhotoUrl = varchar("item_photo_url", length = 2048).nullable()

    override val primaryKey = PrimaryKey(id)
}