package com.ronydiaz.dockerlearn.models

import kotlinx.serialization.Serializable

@Serializable
enum class Priority {
    LOW, MEDIUM, HIGH
}

@Serializable
data class Task(
    val id: String,
    val title: String,
    val description: String = "",
    val priority: Priority = Priority.MEDIUM,
    val isCompleted: Boolean = false
)

@Serializable
data class CreateTaskRequest(
    val title: String,
    val description: String = "",
    val priority: Priority = Priority.MEDIUM
)
