package com.ronydiaz.dockerlearn.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatMessage(
    val id: String,
    val senderId: String,
    val senderName: String,
    val receiverId: String? = null, // null = Sala General
    val content: String,
    val createdAt: String
)

@Serializable
data class SendMessagePayload(
    val receiverId: String? = null,
    val content: String
)

@Serializable
data class OnlineUser(
    val id: String,
    val name: String,
    val email: String
)

@Serializable
data class ChatSocketEvent(
    val type: String, // "NEW_MESSAGE", "ONLINE_USERS", "CONNECTED", "ERROR"
    val message: ChatMessage? = null,
    val users: List<OnlineUser>? = null,
    val info: String? = null
)
