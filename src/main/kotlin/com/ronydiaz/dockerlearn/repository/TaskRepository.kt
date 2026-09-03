package com.ronydiaz.dockerlearn.repository

import com.ronydiaz.dockerlearn.models.CreateTaskRequest
import com.ronydiaz.dockerlearn.models.Priority
import com.ronydiaz.dockerlearn.models.Task
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

interface TaskRepository {
    fun allTasks(): List<Task>
    fun taskById(id: String): Task?
    fun addTask(request: CreateTaskRequest): Task
    fun deleteTask(id: String): Boolean
}

class InMemoryTaskRepository : TaskRepository {
    private val tasks = ConcurrentHashMap<String, Task>()

    init {
        val sample1 = Task(
            id = UUID.randomUUID().toString(),
            title = "Aprender Docker y Backend",
            description = "Construir API Ktor y desplegarla en Google Cloud",
            priority = Priority.HIGH,
            isCompleted = false
        )
        val sample2 = Task(
            id = UUID.randomUUID().toString(),
            title = "Conectar con Android",
            description = "Consumir los endpoints desde la app en Android Studio",
            priority = Priority.MEDIUM,
            isCompleted = false
        )
        tasks[sample1.id] = sample1
        tasks[sample2.id] = sample2
    }

    override fun allTasks(): List<Task> = tasks.values.toList()

    override fun taskById(id: String): Task? = tasks[id]

    override fun addTask(request: CreateTaskRequest): Task {
        val newTask = Task(
            id = UUID.randomUUID().toString(),
            title = request.title,
            description = request.description,
            priority = request.priority,
            isCompleted = false
        )
        tasks[newTask.id] = newTask
        return newTask
    }

    override fun deleteTask(id: String): Boolean = tasks.remove(id) != null
}
