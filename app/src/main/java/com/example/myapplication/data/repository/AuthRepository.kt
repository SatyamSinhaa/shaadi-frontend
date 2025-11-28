package com.example.myapplication.data.repository

import com.example.myapplication.data.api.ApiService
import com.example.myapplication.data.model.LoginDto
import com.example.myapplication.data.model.RegisterDto
import com.example.myapplication.data.model.User
import retrofit2.Response

class AuthRepository(private val apiService: ApiService) {
    suspend fun login(loginDto: LoginDto): Response<User> = apiService.login(loginDto)
    
    suspend fun register(registerDto: RegisterDto): Response<User> = apiService.register(registerDto)
}
