package com.example.myapplication.data.model

data class Notification(
    val id: Long? = null,
    val recipient: User,
    val type: String,
    val message: String,
    val relatedUser: User? = null,
    val isRead: Boolean = false,
    val createdAt: String
)

enum class NotificationType(val value: String) {
    REQUEST_RECEIVED("REQUEST_RECEIVED"),
    REQUEST_ACCEPTED("REQUEST_ACCEPTED"),
    REQUEST_REJECTED("REQUEST_REJECTED")
}
