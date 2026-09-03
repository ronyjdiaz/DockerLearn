package com.ronydiaz.dockerlearn.routes

import com.ronydiaz.dockerlearn.models.CreateTaskRequest
import com.ronydiaz.dockerlearn.repository.TaskRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.taskRouting(repository: TaskRepository) {
    route("/tasks") {
        get {
            call.respond(repository.allTasks())
        }

        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Falta el parámetro ID",
                status = HttpStatusCode.BadRequest
            )
            val task = repository.taskById(id) ?: return@get call.respondText(
                "Tarea no encontrada",
                status = HttpStatusCode.NotFound
            )
            call.respond(task)
        }

        post {
            try {
                val request = call.receive<CreateTaskRequest>()
                val createdTask = repository.addTask(request)
                call.respond(HttpStatusCode.Created, createdTask)
            } catch (e: Exception) {
                call.respondText(
                    "Error procesando JSON: ${e.localizedMessage}",
                    status = HttpStatusCode.BadRequest
                )
            }
        }

        delete("{id}") {
            val id = call.parameters["id"] ?: return@delete call.respondText(
                "Falta el parámetro ID",
                status = HttpStatusCode.BadRequest
            )
            if (repository.deleteTask(id)) {
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
