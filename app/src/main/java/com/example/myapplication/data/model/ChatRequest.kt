package com.example.myapplication.data.model

data class ChatRequest(
    val id: Int,
    val sender: User,
    val receiver: User,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)
