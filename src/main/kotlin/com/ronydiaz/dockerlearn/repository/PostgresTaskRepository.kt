package com.ronydiaz.dockerlearn.repository

import com.ronydiaz.dockerlearn.database.TasksTable
import com.ronydiaz.dockerlearn.models.CreateTaskRequest
import com.ronydiaz.dockerlearn.models.Priority
import com.ronydiaz.dockerlearn.models.Task
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

class PostgresTaskRepository : TaskRepository {

    private fun resultRowToTask(row: ResultRow) = Task(
        id = row[TasksTable.id],
        title = row[TasksTable.title],
        description = row[TasksTable.description],
        priority = runCatching { Priority.valueOf(row[TasksTable.priority]) }.getOrDefault(Priority.MEDIUM),
        isCompleted = row[TasksTable.isCompleted]
    )

    override fun allTasks(): List<Task> = transaction {
        TasksTable.selectAll().map(::resultRowToTask)
    }

    override fun taskById(id: String): Task? = transaction {
        TasksTable.selectAll()
            .where { TasksTable.id eq id }
            .map(::resultRowToTask)
            .singleOrNull()
    }

    override fun addTask(request: CreateTaskRequest): Task {
        val newId = UUID.randomUUID().toString()
        transaction {
            TasksTable.insert {
                it[id] = newId
                it[title] = request.title
                it[description] = request.description
                it[priority] = request.priority.name
                it[isCompleted] = false
            }
        }
        return Task(
            id = newId,
            title = request.title,
            description = request.description,
            priority = request.priority,
            isCompleted = false
        )
    }

    override fun deleteTask(id: String): Boolean = transaction {
        TasksTable.deleteWhere { TasksTable.id eq id } > 0
    }
}
