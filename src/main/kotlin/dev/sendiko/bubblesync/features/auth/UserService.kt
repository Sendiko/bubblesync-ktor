package dev.sendiko.bubblesync.features.auth

import dev.sendiko.bubblesync.features.places.LaundryPlaces
import dev.sendiko.bubblesync.features.records.LaundryItems
import dev.sendiko.bubblesync.features.records.LaundryRecords
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.datetime
import org.jetbrains.exposed.v1.r2dbc.*
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import java.time.LocalDateTime
import java.util.UUID

data class UserAccount(
    val id: String,
    val email: String,
    val name: String,
    val passwordHash: String,
    val createdAt: LocalDateTime,
)

class UserService(private val database: R2dbcDatabase) {
    object Users : Table("users") {
        val id = varchar("id", length = 36)
        val email = varchar("email", length = 254).uniqueIndex()
        val passwordHash = varchar("password_hash", length = 255)
        val name = varchar("name", length = 100)
        val createdAt = datetime("created_at")

        override val primaryKey = PrimaryKey(id)
    }

    suspend fun createSchema() {
        suspendTransaction(database) {
            SchemaUtils.create(Users, LaundryPlaces, LaundryRecords, LaundryItems)
        }
    }

    suspend fun create(email: String, passwordHash: String, name: String): UserAccount = suspendTransaction(database) {
        val account = UserAccount(
            id = UUID.randomUUID().toString(),
            email = email,
            name = name,
            passwordHash = passwordHash,
            createdAt = LocalDateTime.now(),
        )
        Users.insert {
            it[id] = account.id
            it[Users.email] = account.email
            it[Users.passwordHash] = account.passwordHash
            it[Users.name] = account.name
            it[Users.createdAt] = account.createdAt
        }
        account
    }

    suspend fun findByEmail(email: String): UserAccount? = suspendTransaction(database) {
        Users.selectAll()
            .where { Users.email eq email }
            .map { it.toUserAccount() }
            .singleOrNull()
    }

    private fun ResultRow.toUserAccount() = UserAccount(
        id = this[Users.id],
        email = this[Users.email],
        name = this[Users.name],
        passwordHash = this[Users.passwordHash],
        createdAt = this[Users.createdAt],
    )
}