package com.ronydiaz.dockerlearn.websockets

import com.ronydiaz.dockerlearn.models.Task
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Collections

@Serializable
data class TaskSocketEvent(
    val action: String, // "CREATED", "UPDATED", "DELETED"
    val task: Task
)

object TaskNotificationService {
    // Almacena las sesiones de WebSockets activas de forma segura entre hilos
    private val sessions = Collections.synchronizedSet(LinkedHashSet<DefaultWebSocketServerSession>())

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    fun register(session: DefaultWebSocketServerSession) {
        sessions.add(session)
        println("🔌 [WebSocket] Nuevo cliente conectado. Total activos: ${sessions.size}")
    }

    fun unregister(session: DefaultWebSocketServerSession) {
        sessions.remove(session)
        println("🔌 [WebSocket] Cliente desconectado. Total activos: ${sessions.size}")
    }

    suspend fun broadcastTaskCreated(task: Task) = withContext(Dispatchers.IO) {
        if (sessions.isEmpty()) return@withContext

        val event = TaskSocketEvent(action = "CREATED", task = task)
        val message = json.encodeToString(event)
        val frame = Frame.Text(message)

        val iterator = sessions.iterator()
        while (iterator.hasNext()) {
            val session = iterator.next()
            try {
                session.send(frame)
            } catch (e: Exception) {
                println("⚠️ [WebSocket] Error enviando a sesión: ${e.message}. Eliminando...")
                iterator.remove()
            }
        }
    }
}
