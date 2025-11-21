package com.example.myapplication.data.model

data class Plan(
    val id: Int,
    val name: String,
    val durationMonths: Int,
    val price: Double,
    val isPublished: Boolean,
    val isAddon: Boolean,
    val chatLimit: Int
)
