package com.ronydiaz.dockerlearn

import com.ronydiaz.dockerlearn.database.DatabaseFactory
import com.ronydiaz.dockerlearn.repository.InMemoryCategoryRepository
import com.ronydiaz.dockerlearn.repository.InMemoryTaskRepository
import com.ronydiaz.dockerlearn.repository.InMemoryUserRepository
import com.ronydiaz.dockerlearn.repository.PostgresCategoryRepository
import com.ronydiaz.dockerlearn.repository.PostgresTaskRepository
import com.ronydiaz.dockerlearn.repository.PostgresUserRepository
import com.ronydiaz.dockerlearn.routes.authRouting
import com.ronydiaz.dockerlearn.routes.categoryRouting
import com.ronydiaz.dockerlearn.routes.taskRouting
import com.ronydiaz.dockerlearn.routes.webSocketRouting
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
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    val host = "0.0.0.0"

    println("🚀 Servidor Ktor iniciando en http://$host:$port ...")

    embeddedServer(Netty, port = port, host = host, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            coerceInputValues = true
        })
    }

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Patch)
        allowMethod(HttpMethod.Delete)
    }

    // Instalación de soporte para WebSockets (tiempo real bidireccional)
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    // Inicializar PostgreSQL en Neon si DATABASE_URL está configurada (o en local.properties)
    val isDbConnected = DatabaseFactory.init()

    val userRepository = if (isDbConnected) {
        println("👤 Usando PostgresUserRepository (Neon PostgreSQL)")
        PostgresUserRepository()
    } else {
        println("💾 Usando InMemoryUserRepository (Memoria RAM)")
        InMemoryUserRepository()
    }

    val taskRepository = if (isDbConnected) {
        println("📦 Usando PostgresTaskRepository (Neon PostgreSQL)")
        PostgresTaskRepository()
    } else {
        println("💾 Usando InMemoryTaskRepository (Memoria RAM)")
        InMemoryTaskRepository()
    }

    val categoryRepository = if (isDbConnected) {
        println("🏷️ Usando PostgresCategoryRepository (Neon PostgreSQL)")
        PostgresCategoryRepository()
    } else {
        println("💾 Usando InMemoryCategoryRepository (Memoria RAM)")
        InMemoryCategoryRepository()
    }

    routing {
        get("/") {
            val dbStatus = if (isDbConnected) "PostgreSQL (Neon) 🐘" else "Memoria RAM 💾"
            call.respondText("🚀 Backend Ktor funcionando para Rony Diaz! [Persistencia: $dbStatus]")
        }

        get("/health") {
            call.respondText("OK")
        }

        authRouting(userRepository)
        taskRouting(taskRepository)
        categoryRouting(categoryRepository)
        webSocketRouting()
    }
}
