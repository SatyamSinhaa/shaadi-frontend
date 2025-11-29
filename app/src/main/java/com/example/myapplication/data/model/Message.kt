package com.example.myapplication.data.model

data class Message(
    val id: Int? = null,
    val sender: User,
    val receiver: User,
    val content: String,
    val sentAt: String,
    val read: Boolean = false
)
