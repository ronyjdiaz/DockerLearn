package com.ronydiaz.dockerlearn.repository

import com.ronydiaz.dockerlearn.database.MessagesTable
import com.ronydiaz.dockerlearn.database.UsersTable
import com.ronydiaz.dockerlearn.models.ChatMessage
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

interface MessageRepository {
    fun saveMessage(senderId: String, receiverId: String?, content: String): ChatMessage
    fun getPublicHistory(limit: Int = 50): List<ChatMessage>
    fun getDirectHistory(user1: String, user2: String, limit: Int = 50): List<ChatMessage>
}

class PostgresMessageRepository : MessageRepository {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.of("UTC"))

    private fun resultRowToChatMessage(row: ResultRow): ChatMessage {
        return ChatMessage(
            id = row[MessagesTable.id],
            senderId = row[MessagesTable.senderId],
            senderName = row[UsersTable.name],
            receiverId = row[MessagesTable.receiverId],
            content = row[MessagesTable.content],
            createdAt = row[MessagesTable.createdAt]
        )
    }

    override fun saveMessage(senderId: String, receiverId: String?, content: String): ChatMessage {
        val newId = UUID.randomUUID().toString()
        val now = Instant.now()
        val formattedTime = timeFormatter.format(now)

        transaction {
            MessagesTable.insert {
                it[id] = newId
                it[this.senderId] = senderId
                it[this.receiverId] = receiverId
                it[this.content] = content
                it[createdAt] = formattedTime
            }
        }

        // Obtener el nombre del emisor
        val senderName = transaction {
            UsersTable.selectAll()
                .where { UsersTable.id eq senderId }
                .map { it[UsersTable.name] }
                .singleOrNull() ?: "Usuario"
        }

        return ChatMessage(
            id = newId,
            senderId = senderId,
            senderName = senderName,
            receiverId = receiverId,
            content = content,
            createdAt = formattedTime
        )
    }

    override fun getPublicHistory(limit: Int): List<ChatMessage> = transaction {
        MessagesTable.join(UsersTable, JoinType.INNER, additionalConstraint = { MessagesTable.senderId eq UsersTable.id })
            .selectAll()
            .where { MessagesTable.receiverId.isNull() }
            .orderBy(MessagesTable.createdAt to SortOrder.ASC)
            .limit(limit)
            .map(::resultRowToChatMessage)
    }

    override fun getDirectHistory(user1: String, user2: String, limit: Int): List<ChatMessage> = transaction {
        MessagesTable.join(UsersTable, JoinType.INNER, additionalConstraint = { MessagesTable.senderId eq UsersTable.id })
            .selectAll()
            .where {
                ((MessagesTable.senderId eq user1) and (MessagesTable.receiverId eq user2)) or
                ((MessagesTable.senderId eq user2) and (MessagesTable.receiverId eq user1))
            }
            .orderBy(MessagesTable.createdAt to SortOrder.ASC)
            .limit(limit)
            .map(::resultRowToChatMessage)
    }
}

class InMemoryMessageRepository : MessageRepository {
    private val messages = CopyOnWriteArrayList<ChatMessage>()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.of("UTC"))

    override fun saveMessage(senderId: String, receiverId: String?, content: String): ChatMessage {
        val msg = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            senderName = "Usuario",
            receiverId = receiverId,
            content = content,
            createdAt = timeFormatter.format(Instant.now())
        )
        messages.add(msg)
        return msg
    }

    override fun getPublicHistory(limit: Int): List<ChatMessage> =
        messages.filter { it.receiverId == null }.takeLast(limit)

    override fun getDirectHistory(user1: String, user2: String, limit: Int): List<ChatMessage> =
        messages.filter {
            (it.senderId == user1 && it.receiverId == user2) ||
            (it.senderId == user2 && it.receiverId == user1)
        }.takeLast(limit)
}
