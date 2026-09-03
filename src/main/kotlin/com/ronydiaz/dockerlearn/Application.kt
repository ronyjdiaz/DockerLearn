package com.ronydiaz.dockerlearn

import com.ronydiaz.dockerlearn.repository.InMemoryTaskRepository
import com.ronydiaz.dockerlearn.routes.taskRouting
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main() {
    // Para Google Cloud Run y Docker:
    // El puerto se lee de la variable de entorno PORT (Cloud Run asigna este puerto automáticamente).
    // Si no está definida (en local), usamos 8080 por defecto.
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val host = "0.0.0.0" // Escuchar en 0.0.0.0 es indispensable para Docker y Cloud Run

    println("🚀 Servidor Ktor iniciando en http://$host:$port ...")

    embeddedServer(Netty, port = port, host = host, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // Configuración de serialización JSON (idéntico a lo que usa Retrofit con kotlinx.serialization)
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    // Configuración de CORS para permitir peticiones desde cualquier origen (móvil, emulador o web)
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
    }

    val repository = InMemoryTaskRepository()

    // Definición de las rutas del backend
    routing {
        get("/") {
            call.respondText("🚀 Backend Ktor funcionando correctamente para Rony Diaz!")
        }

        get("/health") {
            call.respondText("OK")
        }

        taskRouting(repository)
    }
}
