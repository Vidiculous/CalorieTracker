package com.calorietracker.data.model

data class ChatMessage(
    val id: String,
    val role: String, // user | ai
    val content: String,
    val imageBase64: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
