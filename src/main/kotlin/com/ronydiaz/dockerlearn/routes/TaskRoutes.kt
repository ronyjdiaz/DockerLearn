package com.ronydiaz.dockerlearn.routes

import com.ronydiaz.dockerlearn.models.CreateTaskRequest
import com.ronydiaz.dockerlearn.repository.TaskRepository
import com.ronydiaz.dockerlearn.security.JwtService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json

private val jsonParser = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
}

fun ApplicationCall.extractUserId(): String? {
    val kongUserId = request.header("X-Consumer-Custom-ID") ?: request.header("X-Consumer-Username")
    if (!kongUserId.isNullOrBlank()) return kongUserId

    val authHeader = request.header("Authorization")
    if (!authHeader.isNullOrBlank() && authHeader.startsWith("Bearer ")) {
        val token = authHeader.removePrefix("Bearer ").trim()
        val decoded = JwtService.verifyToken(token)
        return decoded?.subject
    }

    return null
}

fun Route.taskRouting(repository: TaskRepository) {
    route("/tasks") {
        get {
            val userId = call.extractUserId()
            if (userId == null) {
                return@get call.respondText(
                    "🚨 401 No autorizado: Debes enviar un Token JWT válido en el header Authorization: Bearer <token>",
                    status = HttpStatusCode.Unauthorized
                )
            }
            call.respond(repository.allTasks(userId))
        }

        get("{id}") {
            val userId = call.extractUserId()
            if (userId == null) {
                return@get call.respondText(
                    "🚨 401 No autorizado: Debes enviar un Token JWT válido en el header Authorization: Bearer <token>",
                    status = HttpStatusCode.Unauthorized
                )
            }
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Falta el parámetro ID",
                status = HttpStatusCode.BadRequest
            )
            val task = repository.taskById(id, userId) ?: return@get call.respondText(
                "Tarea no encontrada",
                status = HttpStatusCode.NotFound
            )
            call.respond(task)
        }

        post {
            try {
                val userId = call.extractUserId()
                if (userId == null) {
                    return@post call.respondText(
                        "🚨 401 No autorizado: Debes enviar un Token JWT válido en el header Authorization: Bearer <token>",
                        status = HttpStatusCode.Unauthorized
                    )
                }
                val bodyText = call.receiveText()
                val request = jsonParser.decodeFromString<CreateTaskRequest>(bodyText)
                val createdTask = repository.addTask(request, userId)
                call.respond(HttpStatusCode.Created, createdTask)
            } catch (e: Exception) {
                call.respondText(
                    "Error procesando JSON: ${e.message}",
                    status = HttpStatusCode.BadRequest
                )
            }
        }

        delete("{id}") {
            val userId = call.extractUserId()
            if (userId == null) {
                return@delete call.respondText(
                    "🚨 401 No autorizado: Debes enviar un Token JWT válido en el header Authorization: Bearer <token>",
                    status = HttpStatusCode.Unauthorized
                )
            }
            val id = call.parameters["id"] ?: return@delete call.respondText(
                "Falta el parámetro ID",
                status = HttpStatusCode.BadRequest
            )
            if (repository.deleteTask(id, userId)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respondText(
                    "Tarea no encontrada para eliminar",
                    status = HttpStatusCode.NotFound
                )
            }
        }
    }
}
