package com.ronydiaz.dockerlearn.database

import org.jetbrains.exposed.sql.Table

object UsersTable : Table("users") {
    val id = varchar("id", 36)
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val name = varchar("name", 100)
    val role = varchar("role", 20).default("USER")

    override val primaryKey = PrimaryKey(id)
}
