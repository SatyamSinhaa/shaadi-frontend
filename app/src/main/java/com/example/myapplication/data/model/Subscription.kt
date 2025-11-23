package com.example.myapplication.data.model

data class Subscription(
    val subscriptionId: Int,
    val userId: Int,
    val planId: Int,
    val planName: String,
    val planDurationMonths: Int,
    val planChatLimit: Int,
    val startDate: String,
    val expiryDate: String,
    val status: String,
    val chatLimit: Int
)
