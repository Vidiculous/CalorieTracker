package com.calorietracker.data.repository

import com.calorietracker.data.db.dao.ChatMessageDao
import com.calorietracker.data.db.entity.ChatMessageEntity
import com.calorietracker.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(private val dao: ChatMessageDao) {

    fun getAllMessages(): Flow<List<ChatMessage>> =
        dao.getAllMessages().map { it.map(ChatMessageEntity::toDomain) }

    suspend fun insert(message: ChatMessage) =
        dao.insert(message.toEntity())

    suspend fun clearAll() =
        dao.clearAll()
}

private fun ChatMessageEntity.toDomain() = ChatMessage(id, role, content, imageBase64, timestamp)
private fun ChatMessage.toEntity() = ChatMessageEntity(id, role, content, imageBase64, timestamp)
