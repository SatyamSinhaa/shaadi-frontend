package com.example.myapplication.data.api

import com.example.myapplication.data.model.ChatRequest
import com.example.myapplication.data.model.Favourite
import com.example.myapplication.data.model.LoginDto
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.model.Plan
import com.example.myapplication.data.model.RegisterDto
import com.example.myapplication.data.model.User
import com.example.myapplication.data.model.Subscription
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("api/users/login")
    suspend fun login(@Body loginDto: LoginDto): Response<User>

    @GET("api/users")
    suspend fun getAllUsers(@retrofit2.http.Query("gender") gender: String? = null): Response<List<User>>

    @GET("api/users/search")
    suspend fun searchUsers(
        @Query("minAge") minAge: Int? = null,
        @Query("maxAge") maxAge: Int? = null,
        @Query("name") name: String? = null,
        @Query("location") location: String? = null,
        @Query("religion") religion: String? = null,
        @Query("gender") gender: String? = null
    ): Response<List<User>>

    @GET("api/users/{userId}/favourites")
    suspend fun getFavourites(@Path("userId") userId: Int): Response<List<Favourite>>

    @POST("api/users/{userId}/favourites/{favouritedUserId}")
    suspend fun addFavourite(@Path("userId") userId: Int, @Path("favouritedUserId") favouritedUserId: Int): Response<Unit>

    @DELETE("api/users/{userId}/favourites/{favouritedUserId}")
    suspend fun removeFavourite(@Path("userId") userId: Int, @Path("favouritedUserId") favouritedUserId: Int): Response<Unit>

    @GET("api/chat/{userId}")
    suspend fun getMessages(@Path("userId") userId: Int): Response<List<Message>>

    @POST("api/chat")
    suspend fun sendMessage(@Body message: Message): Response<Message>

    @POST("api/chat/mark-read/{receiverId}/{senderId}")
    suspend fun markMessagesAsRead(@Path("receiverId") receiverId: Int, @Path("senderId") senderId: Int): Response<Unit>

    @GET("api/chat/requests/{userId}")
    suspend fun getChatRequests(@Path("userId") userId: Int): Response<List<ChatRequest>>

    @POST("api/chat/request")
    suspend fun sendChatRequest(@Body body: Map<String, Int>): Response<ChatRequest>

    @POST("api/chat/request/{requestId}/accept")
    suspend fun acceptChatRequest(@Path("requestId") requestId: Int, @Body body: Map<String, Int>): Response<Unit>

    @POST("api/chat/request/{requestId}/reject")
    suspend fun rejectChatRequest(@Path("requestId") requestId: Int, @Body body: Map<String, Int>): Response<Unit>

    @DELETE("api/chat/request/{requestId}")
    suspend fun cancelChatRequest(@Path("requestId") requestId: Int, @Query("userId") userId: Int): Response<Unit>

    @POST("api/users/register")
    suspend fun register(@Body registerDto: RegisterDto): Response<User>

    @PUT("api/users/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body user: User): Response<User>

    @GET("api/users/{id}")
    suspend fun getUserById(@Path("id") id: Int): Response<User>

    @GET("api/plans")
    suspend fun getAllPlans(): Response<List<Plan>>

    @GET("api/users/{userId}/subscription")
    suspend fun getSubscription(@Path("userId") userId: Int): Response<Subscription>

    @GET("api/users/{userId}/subscription-history")
    suspend fun getSubscriptionHistory(@Path("userId") userId: Int): Response<List<Subscription>>

    @POST("api/users/{userId}/purchase-subscription")
    suspend fun purchaseSubscription(@Path("userId") userId: Int, @Body body: Map<String, Int>): Response<Subscription>

    @GET("api/notifications/{userId}")
    suspend fun getNotifications(@Path("userId") userId: Int): Response<List<Notification>>

    @GET("api/notifications/{userId}/unread")
    suspend fun getUnreadNotifications(@Path("userId") userId: Int): Response<List<Notification>>

    @GET("api/notifications/{userId}/unread/count")
    suspend fun getUnreadNotificationCount(@Path("userId") userId: Int): Response<Map<String, Long>>

    @POST("api/notifications/{notificationId}/read")
    suspend fun markNotificationAsRead(@Path("notificationId") notificationId: Long, @Body body: Map<String, Int>): Response<Unit>

    @POST("api/notifications/{userId}/read-all")
    suspend fun markAllNotificationsAsRead(@Path("userId") userId: Int): Response<Unit>
}
