package com.example.myapplication.data.model

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: String,
    val age: Int?,
    val gender: String?,
    val gotr: String?,
    val caste: String?,
    val category: String?,
    val religion: String?,
    val cityTown: String?,
    val district: String?,
    val state: String?,
    val bio: String?,
    val photoUrl: String?,
    val firebaseUid: String?,
    val freeChatLimit: Int,
    val photos: List<Photo> = emptyList()
)
