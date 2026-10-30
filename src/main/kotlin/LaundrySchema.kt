package dev.sendiko

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime
import java.time.LocalDateTime

object LaundryPlaces : Table("laundry_places") {
    val id = varchar("id", length = 36).defaultExpression(CustomFunction("UUID", VarCharColumnType(36)))
    val name = varchar("name", length = 255)
    val location = varchar("location", length = 1000)
    val contactNumber = varchar("contact_number", length = 50).nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

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