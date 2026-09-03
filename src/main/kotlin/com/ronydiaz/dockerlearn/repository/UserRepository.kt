package com.ronydiaz.dockerlearn.repository

import com.ronydiaz.dockerlearn.database.UsersTable
import com.ronydiaz.dockerlearn.models.RegisterRequest
import com.ronydiaz.dockerlearn.models.User
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class UserRecord(
    val user: User,
    val passwordHash: String
)

interface UserRepository {
    fun findByEmail(email: String): UserRecord?
    fun findById(id: String): User?
    fun createUser(request: RegisterRequest, passwordHash: String): User
}

class PostgresUserRepository : UserRepository {
    private fun resultRowToUserRecord(row: ResultRow) = UserRecord(
        user = User(
            id = row[UsersTable.id],
            email = row[UsersTable.email],
            name = row[UsersTable.name],
            role = row[UsersTable.role]
        ),
        passwordHash = row[UsersTable.passwordHash]
    )

    override fun findByEmail(email: String): UserRecord? = transaction {
        UsersTable.selectAll()
            .where { UsersTable.email eq email.trim().lowercase() }
            .map(::resultRowToUserRecord)
            .singleOrNull()
    }

    override fun findById(id: String): User? = transaction {
        UsersTable.selectAll()
            .where { UsersTable.id eq id }
            .map { row ->
                User(
                    id = row[UsersTable.id],
                    email = row[UsersTable.email],
                    name = row[UsersTable.name],
                    role = row[UsersTable.role]
                )
            }
            .singleOrNull()
    }

    override fun createUser(request: RegisterRequest, passwordHash: String): User {
        val newId = UUID.randomUUID().toString()
        val cleanEmail = request.email.trim().lowercase()
        val cleanName = request.name.trim()

        transaction {
            UsersTable.insert {
                it[id] = newId
                it[email] = cleanEmail
                it[name] = cleanName
                it[this.passwordHash] = passwordHash
                it[role] = "USER"
            }
        }
        return User(
            id = newId,
            email = cleanEmail,
            name = cleanName,
            role = "USER"
        )
    }
}

class InMemoryUserRepository : UserRepository {
    private val records = ConcurrentHashMap<String, UserRecord>()

    override fun findByEmail(email: String): UserRecord? =
        records.values.firstOrNull { it.user.email.equals(email, ignoreCase = true) }

    override fun findById(id: String): User? =
        records[id]?.user

    override fun createUser(request: RegisterRequest, passwordHash: String): User {
        val newId = UUID.randomUUID().toString()
        val user = User(newId, request.email.trim().lowercase(), request.name.trim(), "USER")
        records[newId] = UserRecord(user, passwordHash)
        return user
    }
}
