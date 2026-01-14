package com.example.myapplication.data.api

data class PaymentRequest(
    val amount: Double,
    val userId: String,
    val mobileNumber: String
)
