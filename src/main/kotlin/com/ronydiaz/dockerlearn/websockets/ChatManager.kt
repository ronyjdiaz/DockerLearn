package com.ronydiaz.dockerlearn.websockets

import com.ronydiaz.dockerlearn.models.ChatMessage
import com.ronydiaz.dockerlearn.models.ChatSocketEvent
import com.ronydiaz.dockerlearn.models.OnlineUser
import com.ronydiaz.dockerlearn.models.User
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.websocket.Frame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

data class ChatUserSession(
    val user: User,
    val session: DefaultWebSocketServerSession
)

object ChatManager {
    // Mapa seguro concurrente: userId -> ChatUserSession
    private val sessions = ConcurrentHashMap<String, ChatUserSession>()

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    suspend fun register(user: User, session: DefaultWebSocketServerSession) {
        sessions[user.id] = ChatUserSession(user, session)
        println("💬 [Chat] Usuario conectado: ${user.name} (${user.email}). Conectados: ${sessions.size}")

        // Enviar confirmación al usuario recién conectado
        val welcomeEvent = ChatSocketEvent(
            type = "CONNECTED",
            info = "Bienvenido al chat en tiempo real, ${user.name}!"
        )
        try {
            val text = json.encodeToString(welcomeEvent)
            session.send(Frame.Text(text))
        } catch (e: Exception) {
            println("⚠️ Error enviando bienvenida: ${e.message}")
        }

        // Notificar a todos la nueva lista de usuarios en línea
        broadcastOnlineUsers()
    }

    suspend fun unregister(userId: String) {
        val removed = sessions.remove(userId)
        if (removed != null) {
            println("💬 [Chat] Usuario desconectado: ${removed.user.name}. Conectados: ${sessions.size}")
            broadcastOnlineUsers()
        }
    }

    private suspend fun broadcastOnlineUsers() = withContext(Dispatchers.IO) {
        val onlineList = sessions.values.map {
            OnlineUser(id = it.user.id, name = it.user.name, email = it.user.email)
        }
        val event = ChatSocketEvent(type = "ONLINE_USERS", users = onlineList)
        val jsonText = json.encodeToString(event)

        sessions.values.forEach { userSession ->
            try {
                // Instanciar un nuevo Frame.Text por cada sesión para evitar reusar el mismo ByteBuffer
                userSession.session.send(Frame.Text(jsonText))
            } catch (e: Exception) {
                println("⚠️ [Chat] Fallo enviando lista online a ${userSession.user.name}: ${e.message}")
            }
        }
    }

    suspend fun broadcastPublicMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        val event = ChatSocketEvent(type = "NEW_MESSAGE", message = message)
        val jsonText = json.encodeToString(event)

        sessions.values.forEach { userSession ->
            try {
                userSession.session.send(Frame.Text(jsonText))
            } catch (e: Exception) {
                println("⚠️ [Chat] Fallo al enviar mensaje público a ${userSession.user.name}: ${e.message}")
            }
        }
    }

    suspend fun sendDirectMessage(message: ChatMessage, targetUserId: String) = withContext(Dispatchers.IO) {
        val event = ChatSocketEvent(type = "NEW_MESSAGE", message = message)
        val jsonText = json.encodeToString(event)

        // Enviar al destinatario si está conectado
        sessions[targetUserId]?.let { targetSession ->
            try {
                targetSession.session.send(Frame.Text(jsonText))
            } catch (e: Exception) {
                println("⚠️ [Chat] Fallo al enviar mensaje directo a $targetUserId: ${e.message}")
            }
        }

        // Enviar también al emisor para que lo vea en su propia ventana
        sessions[message.senderId]?.let { senderSession ->
            if (message.senderId != targetUserId) {
                try {
                    senderSession.session.send(Frame.Text(jsonText))
                } catch (e: Exception) {
                    println("⚠️ [Chat] Fallo al enviar confirmación al emisor ${message.senderId}: ${e.message}")
                }
            }
        }
    }
}
