package com.ronydiaz.dockerlearn.routes

import com.ronydiaz.dockerlearn.models.CreateCategoryRequest
import com.ronydiaz.dockerlearn.repository.CategoryRepository
import io.ktor.http.HttpStatusCode
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

fun Route.categoryRouting(repository: CategoryRepository) {
    route("/categories") {
        get {
            call.respond(repository.allCategories())
        }

        get("{id}") {
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Falta el ID",
                status = HttpStatusCode.BadRequest
            )
            val category = repository.categoryById(id) ?: return@get call.respondText(
                "Categoría no encontrada",
                status = HttpStatusCode.NotFound
            )
            call.respond(category)
        }

        post {
            try {
                val bodyText = call.receiveText()
                val request = jsonParser.decodeFromString<CreateCategoryRequest>(bodyText)
                val created = repository.addCategory(request)
                call.respond(HttpStatusCode.Created, created)
            } catch (e: Exception) {
                call.respondText(
                    "Error procesando JSON: ${e.message}",
                    status = HttpStatusCode.BadRequest
                )
            }
        }

        delete("{id}") {
            val id = call.parameters["id"] ?: return@delete call.respondText(
                "Falta el ID",
                status = HttpStatusCode.BadRequest
            )
            if (repository.deleteCategory(id)) {
                call.respond(HttpStatusCode.NoContent)
            } else {
                call.respondText(
                    "Categoría no encontrada para eliminar",
                    status = HttpStatusCode.NotFound
                )
            }
        }
    }
}
