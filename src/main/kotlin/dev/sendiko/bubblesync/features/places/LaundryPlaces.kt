package dev.sendiko.bubblesync.features.places

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.CustomFunction
import org.jetbrains.exposed.v1.core.VarCharColumnType
import org.jetbrains.exposed.v1.javatime.CurrentDateTime
import org.jetbrains.exposed.v1.javatime.datetime

object LaundryPlaces : Table("laundry_places") {
    val id = varchar("id", length = 36).defaultExpression(CustomFunction("UUID", VarCharColumnType(36)))
    val name = varchar("name", length = 255)
    val location = varchar("location", length = 1000)
    val contactNumber = varchar("contact_number", length = 50).nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}