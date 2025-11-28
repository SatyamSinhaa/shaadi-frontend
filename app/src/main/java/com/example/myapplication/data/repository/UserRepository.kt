package com.example.myapplication.data.repository

import com.example.myapplication.data.api.ApiService
import com.example.myapplication.data.model.Favourite
import com.example.myapplication.data.model.Subscription
import com.example.myapplication.data.model.User
import retrofit2.Response

class UserRepository(private val apiService: ApiService) {
    suspend fun getAllUsers(gender: String?): Response<List<User>> = apiService.getAllUsers(gender)
    
    suspend fun getUserById(id: Int): Response<User> = apiService.getUserById(id)
    
    suspend fun updateUser(id: Int, user: User): Response<User> = apiService.updateUser(id, user)
    
    suspend fun searchUsers(
        minAge: Int?,
        maxAge: Int?,
        name: String?,
        location: String?,
        religion: String?,
        gender: String?
    ): Response<List<User>> = apiService.searchUsers(minAge, maxAge, name, location, religion, gender)
    
    suspend fun getFavourites(userId: Int): Response<List<Favourite>> = apiService.getFavourites(userId)
    
    suspend fun addFavourite(userId: Int, favouritedUserId: Int): Response<Unit> = 
        apiService.addFavourite(userId, favouritedUserId)
    
    suspend fun removeFavourite(userId: Int, favouritedUserId: Int): Response<Unit> = 
        apiService.removeFavourite(userId, favouritedUserId)
        
    suspend fun getSubscription(userId: Int): Response<Subscription> = apiService.getSubscription(userId)
}
