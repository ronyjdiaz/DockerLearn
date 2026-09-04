package com.ronydiaz.dockerlearn.database

import org.jetbrains.exposed.sql.Table

object MessagesTable : Table("messages") {
    val id = varchar("id", 36)
    val senderId = varchar("sender_id", 36).references(UsersTable.id)
    val receiverId = varchar("receiver_id", 36).references(UsersTable.id).nullable() // null = Sala General
    val content = text("content")
    val createdAt = varchar("created_at", 30) // Formato ISO-8601 o timestamp legible

    override val primaryKey = PrimaryKey(id)
}
