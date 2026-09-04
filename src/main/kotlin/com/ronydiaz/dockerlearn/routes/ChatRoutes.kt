package com.ronydiaz.dockerlearn.routes

import com.ronydiaz.dockerlearn.models.SendMessagePayload
import com.ronydiaz.dockerlearn.repository.MessageRepository
import com.ronydiaz.dockerlearn.repository.UserRepository
import com.ronydiaz.dockerlearn.security.JwtService
import com.ronydiaz.dockerlearn.websockets.ChatManager
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.CloseReason
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

fun Route.chatRouting(messageRepository: MessageRepository, userRepository: UserRepository) {
    // Endpoints REST para historial previo
    route("/messages") {
        get("/public") {
            val userId = call.extractUserId()
            if (userId == null) {
                return@get call.respondText("No autorizado", status = HttpStatusCode.Unauthorized)
            }
            val history = messageRepository.getPublicHistory(50)
            call.respond(history)
        }

        get("/direct/{targetUserId}") {
            val userId = call.extractUserId()
            if (userId == null) {
                return@get call.respondText("No autorizado", status = HttpStatusCode.Unauthorized)
            }
            val targetUserId = call.parameters["targetUserId"] ?: return@get call.respondText(
                "Falta el targetUserId",
                status = HttpStatusCode.BadRequest
            )
            val history = messageRepository.getDirectHistory(userId, targetUserId, 50)
            call.respond(history)
        }

        get("/users") {
            val userId = call.extractUserId()
            if (userId == null) {
                return@get call.respondText("No autorizado", status = HttpStatusCode.Unauthorized)
            }
            val all = userRepository.allUsers()
            call.respond(all)
        }
    }

    // Endpoint WebSocket para streaming de mensajes en tiempo real
    webSocket("/ws/chat") {
        // En WebSockets, el token puede venir por query param: /ws/chat?token=...
        val token = call.request.queryParameters["token"]
        val decoded = if (!token.isNullOrBlank()) JwtService.verifyToken(token) else null
        val userId = decoded?.subject

        if (userId == null) {
            println("🚨 [Chat WebSocket] Intento de conexión sin token válido.")
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Token JWT requerido"))
            return@webSocket
        }

        val user = userRepository.findById(userId)
        if (user == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Usuario no encontrado"))
            return@webSocket
        }

        ChatManager.register(user, this)
        try {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    try {
                        val payload = json.decodeFromString<SendMessagePayload>(text)
                        if (payload.content.isNotBlank()) {
                            // Guardar en la base de datos
                            val savedMessage = messageRepository.saveMessage(
                                senderId = user.id,
                                receiverId = payload.receiverId,
                                content = payload.content.trim()
                            )

                            // Emitir el mensaje en tiempo real
                            if (payload.receiverId.isNullOrBlank()) {
                                ChatManager.broadcastPublicMessage(savedMessage)
                            } else {
                                ChatManager.sendDirectMessage(savedMessage, payload.receiverId)
                            }
                        }
                    } catch (e: Exception) {
                        println("⚠️ [Chat] Error procesando payload recibido: ${e.message}")
                    }
                }
            }
        } finally {
            ChatManager.unregister(user.id)
        }
    }
}
