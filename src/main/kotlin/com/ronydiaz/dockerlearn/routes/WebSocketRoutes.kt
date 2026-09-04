package com.ronydiaz.dockerlearn.routes

import com.ronydiaz.dockerlearn.websockets.TaskNotificationService
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText

fun Route.webSocketRouting() {
    webSocket("/ws/tasks") {
        TaskNotificationService.register(this)
        try {
            // Enviar mensaje de bienvenida al cliente
            send(Frame.Text("""{"type":"CONNECTED","message":"✅ Conectado exitosamente al canal de tareas en tiempo real"}"""))

            // Mantener el canal abierto escuchando cualquier mensaje del cliente
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    println("📨 [WebSocket Recibido]: $text")
                    // Eco de confirmación
                    send(Frame.Text("""{"type":"ACK","received":"$text"}"""))
                }
            }
        } catch (e: Exception) {
            println("❌ [WebSocket Error]: ${e.localizedMessage}")
        } finally {
            TaskNotificationService.unregister(this)
        }
    }
}
