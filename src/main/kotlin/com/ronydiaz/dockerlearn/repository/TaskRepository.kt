package com.ronydiaz.dockerlearn.repository

import com.ronydiaz.dockerlearn.models.CreateTaskRequest
import com.ronydiaz.dockerlearn.models.Priority
import com.ronydiaz.dockerlearn.models.Task
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface TaskRepository {
    fun allTasks(userId: String? = null): List<Task>
    fun taskById(id: String, userId: String? = null): Task?
    fun addTask(request: CreateTaskRequest, userId: String? = null): Task
    fun deleteTask(id: String, userId: String? = null): Boolean
}

class InMemoryTaskRepository : TaskRepository {
    private val tasks = ConcurrentHashMap<String, Task>()

    override fun allTasks(userId: String?): List<Task> {
        return if (userId != null) {
            tasks.values.filter { it.userId == userId || it.userId == null }
        } else {
            tasks.values.toList()
        }
    }

    override fun taskById(id: String, userId: String?): Task? {
        val task = tasks[id] ?: return null
        if (userId != null && task.userId != null && task.userId != userId) return null
        return task
    }

    override fun addTask(request: CreateTaskRequest, userId: String?): Task {
        val newId = UUID.randomUUID().toString()
        val task = Task(
            id = newId,
            userId = userId,
            title = request.title,
            description = request.description,
            priority = request.priority,
            isCompleted = false
        )
        tasks[newId] = task
        return task
    }

    override fun deleteTask(id: String, userId: String?): Boolean {
        val task = tasks[id] ?: return false
        if (userId != null && task.userId != null && task.userId != userId) return false
        return tasks.remove(id) != null
    }
}
