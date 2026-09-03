package com.ronydiaz.dockerlearn.repository

import com.ronydiaz.dockerlearn.database.TasksTable
import com.ronydiaz.dockerlearn.models.CreateTaskRequest
import com.ronydiaz.dockerlearn.models.Priority
import com.ronydiaz.dockerlearn.models.Task
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class PostgresTaskRepository : TaskRepository {

    private fun resultRowToTask(row: ResultRow) = Task(
        id = row[TasksTable.id],
        userId = row[TasksTable.userId],
        title = row[TasksTable.title],
        description = row[TasksTable.description],
        priority = runCatching { Priority.valueOf(row[TasksTable.priority]) }.getOrDefault(Priority.MEDIUM),
        isCompleted = row[TasksTable.isCompleted]
    )

    override fun allTasks(userId: String?): List<Task> = transaction {
        if (userId != null) {
            TasksTable.selectAll()
                .where { (TasksTable.userId eq userId) or (TasksTable.userId.isNull()) }
                .map(::resultRowToTask)
        } else {
            TasksTable.selectAll().map(::resultRowToTask)
        }
    }

    override fun taskById(id: String, userId: String?): Task? = transaction {
        if (userId != null) {
            TasksTable.selectAll()
                .where { (TasksTable.id eq id) and ((TasksTable.userId eq userId) or (TasksTable.userId.isNull())) }
                .map(::resultRowToTask)
                .singleOrNull()
        } else {
            TasksTable.selectAll()
                .where { TasksTable.id eq id }
                .map(::resultRowToTask)
                .singleOrNull()
        }
    }

    override fun addTask(request: CreateTaskRequest, userId: String?): Task {
        val newId = UUID.randomUUID().toString()
        transaction {
            TasksTable.insert {
                it[id] = newId
                it[TasksTable.userId] = userId
                it[title] = request.title
                it[description] = request.description
                it[priority] = request.priority.name
                it[isCompleted] = false
            }
        }
        return Task(
            id = newId,
            userId = userId,
            title = request.title,
            description = request.description,
            priority = request.priority,
            isCompleted = false
        )
    }

    override fun deleteTask(id: String, userId: String?): Boolean = transaction {
        if (userId != null) {
            TasksTable.deleteWhere { (TasksTable.id eq id) and (TasksTable.userId eq userId) } > 0
        } else {
            TasksTable.deleteWhere { TasksTable.id eq id } > 0
        }
    }
}
