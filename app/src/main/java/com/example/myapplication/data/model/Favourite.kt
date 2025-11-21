package com.example.myapplication.data.model

data class Favourite(
    val id: Long,
    val user: User,
    val favouritedUser: User
)
