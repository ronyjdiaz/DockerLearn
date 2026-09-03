package com.ronydiaz.dockerlearn.database

import org.jetbrains.exposed.sql.Table

object TasksTable : Table("tasks") {
    val id = varchar("id", 36)
    val userId = varchar("user_id", 36).nullable()
    val title = varchar("title", 255)
    val description = text("description").default("")
    val priority = varchar("priority", 20).default("MEDIUM")
    val isCompleted = bool("is_completed").default(false)

    override val primaryKey = PrimaryKey(id)
}
