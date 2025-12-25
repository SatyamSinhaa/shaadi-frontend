package com.example.myapplication.ui.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.api.RetrofitClient
import com.example.myapplication.data.api.SupabaseConfig
import com.example.myapplication.data.api.WebSocketMessage
import com.example.myapplication.data.api.PhotoUpdateRequest
import com.example.myapplication.data.model.ChatRequest
import com.example.myapplication.data.model.ErrorResponse
import com.example.myapplication.data.model.Favourite
import com.example.myapplication.data.model.LoginDto
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.model.RegisterDto
import com.example.myapplication.data.model.Subscription
import com.example.myapplication.data.model.User
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class LoginViewModel : ViewModel() {
    private val apiService = RetrofitClient.apiService
    private val webSocketManager = RetrofitClient.webSocketManager

    companion object {
        private const val PREFS_NAME = "shaadi_prefs"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PASSWORD = "user_password"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    init {
        // Collect WebSocket messages and update appropriate states
        viewModelScope.launch {
            try {
                webSocketManager.messageFlow.collect { webSocketMessage ->
                    when (webSocketMessage) {
                        is WebSocketMessage.ChatMessage -> {
                            Log.i("LoginViewModel", "🔔 WebSocket ChatMessage received: ${webSocketMessage.message.content}")
                            val currentMessages = _messages.value.toMutableList()
                            val messageExists = currentMessages.any { it.id == webSocketMessage.message.id }
                            if (!messageExists) {
                                currentMessages.add(webSocketMessage.message)
                                _messages.value = currentMessages
                            }
                        }
                        is WebSocketMessage.ChatRequestMessage -> {
                            val currentRequests = _chatRequests.value.toMutableList()
                            val requestExists = currentRequests.any { it.id == webSocketMessage.chatRequest.id }
                            if (!requestExists) {
                                currentRequests.add(webSocketMessage.chatRequest)
                                _chatRequests.value = currentRequests
                            }
                        }
                        is WebSocketMessage.NotificationMessage -> {
                            val currentNotifications = _notifications.value.toMutableList()
                            val notificationExists = currentNotifications.any { it.id == webSocketMessage.notification.id }
                            if (!notificationExists) {
                                currentNotifications.add(0, webSocketMessage.notification)
                                _notifications.value = currentNotifications
                                _unreadNotificationCount.value = _unreadNotificationCount.value + 1
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "❌ Error in WebSocket message collection", e)
            }
        }

        // Fallback polling monitoring
        viewModelScope.launch {
            try {
                webSocketManager.isConnected.collect { isConnected ->
                    if (!isConnected && loginState.value is LoginState.Success) {
                        val user = (loginState.value as LoginState.Success).user
                        startFallbackPolling(user.id)
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "❌ Error in WebSocket connection monitoring", e)
            }
        }
    }

    private fun startFallbackPolling(userId: Int) {
        viewModelScope.launch {
            while (webSocketManager.isConnected.value == false && loginState.value is LoginState.Success) {
                try {
                    fetchMessages(userId)
                    fetchChatRequests(userId)
                    delay(10000L)
                } catch (e: Exception) {
                    delay(30000L)
                }
            }
        }
    }

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    private val _registerState = MutableStateFlow<LoginState>(LoginState.Idle)
    val registerState: StateFlow<LoginState> = _registerState.asStateFlow()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users.asStateFlow()

    private val _favourites = MutableStateFlow<List<Favourite>>(emptyList())
    val favourites: StateFlow<List<Favourite>> = _favourites.asStateFlow()

    private val _selectedUser = MutableStateFlow<User?>(null)
    val selectedUser: StateFlow<User?> = _selectedUser.asStateFlow()

    private val _chatTargetUser = MutableStateFlow<User?>(null)
    val chatTargetUser: StateFlow<User?> = _chatTargetUser.asStateFlow()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _sendMessageError = MutableStateFlow<String?>(null)
    val sendMessageError: StateFlow<String?> = _sendMessageError.asStateFlow()

    private val _subscription = MutableStateFlow<Subscription?>(null)
    val subscription: StateFlow<Subscription?> = _subscription.asStateFlow()

    private val _userProfileLoading = MutableStateFlow(false)
    val userProfileLoading: StateFlow<Boolean> = _userProfileLoading.asStateFlow()

    private val _chatRequests = MutableStateFlow<List<ChatRequest>>(emptyList())
    val chatRequests: StateFlow<List<ChatRequest>> = _chatRequests.asStateFlow()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

    private val _blockedUsers = MutableStateFlow<List<Map<String, Any>>>(emptyList())
    val blockedUsers: StateFlow<List<Map<String, Any>>> = _blockedUsers.asStateFlow()

    val isWebSocketConnected = webSocketManager.isConnected

    fun login(context: Context, userName: String, password: String, rememberMe: Boolean = true) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            _messages.value = emptyList()

            try {
                val response = apiService.login(LoginDto(userName, password))
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        _loginState.value = LoginState.Success(user)
                        saveLoginCredentials(context, userName, password)
                        saveUserId(context, user.id)
                    webSocketManager.connect(user.id)
                    fetchSubscription(user.id)
                    fetchAllUsers()
                    fetchMessages(user.id)
                    fetchChatRequests(user.id)
                    }
                } else {
                    _loginState.value = LoginState.Error("Login failed")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Exception: ${e.message}")
            }
        }
    }

    private fun saveUserId(context: Context, userId: Int) {
        val prefs = getSharedPreferences(context)
        prefs.edit().putInt(KEY_USER_ID, userId).apply()
    }

    fun uploadImageAndSync(context: Context, uri: Uri, userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Uploading photo...", Toast.LENGTH_SHORT).show()
                }
                
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error reading image file", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                
                val fileName = "profile_${userId}_${UUID.randomUUID()}.jpg"
                val bucketName = SupabaseConfig.BUCKET_NAME
                val bucket = SupabaseConfig.supabase.storage.from(bucketName)
                
                bucket.upload(fileName, bytes)

                val encodedBucketName = bucketName.replace(" ", "%20")
                val publicUrl = "https://${SupabaseConfig.PROJECT_ID}.supabase.co/storage/v1/object/public/$encodedBucketName/$fileName"

                val response = apiService.updateProfilePhoto(userId, PhotoUpdateRequest(userId, publicUrl))
                
                if (response.isSuccessful) {
                    // Refetch user by ID to trigger navigation correctly if needed
                    val userResponse = apiService.getUserById(userId)
                    if (userResponse.isSuccessful) {
                        userResponse.body()?.let { updatedUser ->
                            val currentState = _loginState.value
                            if (currentState is LoginState.Success && currentState.user.id == userId) {
                                _loginState.value = LoginState.Success(updatedUser)
                            }
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Profile photo updated!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("Upload", "Error during upload/sync: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun uploadGalleryPhoto(context: Context, uri: Uri, userId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Adding to gallery...", Toast.LENGTH_SHORT).show()
                }
                
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null) return@launch
                
                val fileName = "gallery_${userId}_${UUID.randomUUID()}.jpg"
                val bucketName = SupabaseConfig.BUCKET_NAME
                val bucket = SupabaseConfig.supabase.storage.from(bucketName)
                
                bucket.upload(fileName, bytes)

                val encodedBucketName = bucketName.replace(" ", "%20")
                val publicUrl = "https://${SupabaseConfig.PROJECT_ID}.supabase.co/storage/v1/object/public/$encodedBucketName/$fileName"

                val response = apiService.addPhotoToGallery(userId, mapOf("photoUrl" to publicUrl))
                
                if (response.isSuccessful) {
                     val userResponse = apiService.getUserById(userId)
                     if (userResponse.isSuccessful) {
                         userResponse.body()?.let { updatedUser ->
                             val currentState = _loginState.value
                             if (currentState is LoginState.Success && currentState.user.id == userId) {
                                 _loginState.value = LoginState.Success(updatedUser)
                             }
                         }
                     }
                     
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Photo added to gallery!", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("Upload", "Gallery upload failed", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gallery upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchSubscription(userId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getSubscription(userId)
                if (response.isSuccessful) {
                    _subscription.value = response.body()
                }
            } catch (e: Exception) {
                _subscription.value = null
            }
        }
    }
    
    fun updateUser(user: User) {
        viewModelScope.launch {
            try {
                val response: Response<User> = apiService.updateUser(user.id, user)
                if (response.isSuccessful) {
                    response.body()?.let { updatedUser ->
                        _loginState.value = LoginState.Success(updatedUser)
                    }
                }
            } catch (e: Exception) {}
        }
    }

    fun updateFcmToken(userId: Int, token: String) {
        viewModelScope.launch {
            try {
                Log.d("LoginViewModel", "📱 Updating FCM token for user $userId: $token")
                val response = apiService.updateFcmToken(userId, mapOf("token" to token))
                if (response.isSuccessful) {
                    Log.d("LoginViewModel", "✅ FCM token updated successfully")
                } else {
                    Log.e("LoginViewModel", "❌ FCM token update failed: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
               Log.e("LoginViewModel", "❌ Exception updating FCM token", e)
            }
        }
    }

    fun logout(context: Context) {
        clearSavedCredentials(context)
        webSocketManager.disconnect()
        _loginState.value = LoginState.Idle
        _users.value = emptyList()
        _messages.value = emptyList()
        _subscription.value = null
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun saveLoginCredentials(context: Context, email: String, password: String) {
        val prefs = getSharedPreferences(context)
        prefs.edit()
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_USER_PASSWORD, password)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun getSavedCredentials(context: Context): Pair<String, String>? {
        val prefs = getSharedPreferences(context)
        val email = prefs.getString(KEY_USER_EMAIL, null)
        val password = prefs.getString(KEY_USER_PASSWORD, null)
        return if (email != null && password != null) Pair(email, password) else null
    }

    private fun clearSavedCredentials(context: Context) {
        val prefs = getSharedPreferences(context)
        prefs.edit()
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_PASSWORD)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }

    fun tryAutoLogin(context: Context) {
        val savedCredentials = getSavedCredentials(context)
        if (savedCredentials != null) {
            val (email, password) = savedCredentials
            login(context, email, password, rememberMe = true)
        }
    }

    fun isLoggedIn(context: Context): Boolean {
        val prefs = getSharedPreferences(context)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun fetchAllUsers() {
        val currentUser = (loginState.value as? LoginState.Success)?.user
        val oppositeGender = when (currentUser?.gender?.lowercase()) {
            "male" -> "Female"
            "female" -> "Male"
            else -> null
        }
        viewModelScope.launch {
            try {
                val response: Response<List<User>> = apiService.getAllUsers(oppositeGender, currentUser?.id)
                if (response.isSuccessful) {
                    response.body()?.let { _users.value = it }
                }
            } catch (e: Exception) {}
        }
    }

    fun fetchFavourites(userId: Int) {
        viewModelScope.launch {
            try {
                val response: Response<List<Favourite>> = apiService.getFavourites(userId)
                if (response.isSuccessful) {
                    response.body()?.let { _favourites.value = it }
                }
            } catch (e: Exception) {}
        }
    }

    fun addFavourite(userId: Int, favouritedUserId: Int) {
        viewModelScope.launch {
            try {
                val response: Response<Unit> = apiService.addFavourite(userId, favouritedUserId)
                if (response.isSuccessful) fetchFavourites(userId)
            } catch (e: Exception) {}
        }
    }

    fun removeFavourite(userId: Int, favouritedUserId: Int) {
        viewModelScope.launch {
            try {
                val response: Response<Unit> = apiService.removeFavourite(userId, favouritedUserId)
                if (response.isSuccessful) fetchFavourites(userId)
            } catch (e: Exception) {}
        }
    }

    fun fetchUserById(userId: Int) {
        _userProfileLoading.value = true
        viewModelScope.launch {
            try {
                val response = apiService.getUserById(userId)
                if (response.isSuccessful) {
                    response.body()?.let { _selectedUser.value = it }
                }
            } catch (e: Exception) {
            } finally {
                _userProfileLoading.value = false
            }
        }
    }

    fun selectUser(user: User?) {
        _selectedUser.value = user
    }

    fun setChatTargetUser(user: User?) {
        _chatTargetUser.value = user
    }

    fun fetchMessages(userId: Int) {
        viewModelScope.launch {
            try {
                val response: Response<List<Message>> = apiService.getMessages(userId)
                if (response.isSuccessful) {
                    response.body()?.let { _messages.value = it }
                }
            } catch (e: Exception) {}
        }
    }

    fun fetchChatRequests(userId: Int) {
        viewModelScope.launch {
            try {
                val response: Response<List<ChatRequest>> = apiService.getChatRequests(userId)
                if (response.isSuccessful) {
                    response.body()?.let { _chatRequests.value = it }
                }
            } catch (e: Exception) {}
        }
    }

    fun acceptChatRequest(requestId: Int, userId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val response = apiService.acceptChatRequest(requestId, mapOf("userId" to userId))
                if (response.isSuccessful) {
                    fetchChatRequests(userId)
                    onSuccess()
                }
            } catch (e: Exception) {}
        }
    }

    fun rejectChatRequest(requestId: Int, userId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.rejectChatRequest(requestId, mapOf("userId" to userId))
                if (response.isSuccessful) fetchChatRequests(userId)
            } catch (e: Exception) {}
        }
    }

    fun sendMessage(sender: User, receiver: User, content: String) {
        viewModelScope.launch {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val currentTimestamp = dateFormat.format(Date())
                val message = Message(sender = sender, receiver = receiver, content = content, sentAt = currentTimestamp)
                val response: Response<Message> = apiService.sendMessage(message)
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    _sendMessageError.value = "Failed: $errorBody"
                }
            } catch (e: Exception) {
                _sendMessageError.value = "Error: ${e.message}"
            }
        }
    }

    fun refreshMessages(userId: Int) {
        fetchMessages(userId)
    }

    fun register(name: String, email: String, password: String, gender: String) {
        _registerState.value = LoginState.Loading
        viewModelScope.launch {
            try {
                val response: Response<User> = apiService.register(RegisterDto(email, password, name, gender))
                if (response.isSuccessful) {
                    response.body()?.let { _registerState.value = LoginState.Success(it) }
                } else {
                    _registerState.value = LoginState.Error("Registration failed")
                }
            } catch (e: Exception) {
                _registerState.value = LoginState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun clearSendMessageError() {
        _sendMessageError.value = null
    }

    fun markMessagesAsRead(receiverId: Int, senderId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.markMessagesAsRead(receiverId, senderId)
                if (response.isSuccessful) fetchMessages(receiverId)
            } catch (e: Exception) {}
        }
    }

    fun fetchNotifications(userId: Int) {
        viewModelScope.launch {
            try {
                val response: Response<List<Notification>> = apiService.getNotifications(userId)
                if (response.isSuccessful) {
                    response.body()?.let { _notifications.value = it }
                }
            } catch (e: Exception) {}
        }
    }

    fun fetchUnreadNotificationCount(userId: Int) {
        viewModelScope.launch {
            try {
                val response: Response<Map<String, Long>> = apiService.getUnreadNotificationCount(userId)
                if (response.isSuccessful) {
                    response.body()?.let { _unreadNotificationCount.value = it["count"]?.toInt() ?: 0 }
                }
            } catch (e: Exception) {}
        }
    }

    fun markNotificationAsRead(notificationId: Long, userId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.markNotificationAsRead(notificationId, mapOf("userId" to userId))
                if (response.isSuccessful) {
                    fetchNotifications(userId)
                    fetchUnreadNotificationCount(userId)
                }
            } catch (e: Exception) {}
        }
    }

    fun markAllNotificationsAsRead(userId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.markAllNotificationsAsRead(userId)
                if (response.isSuccessful) {
                    fetchNotifications(userId)
                    fetchUnreadNotificationCount(userId)
                }
            } catch (e: Exception) {}
        }
    }

    fun cancelChatRequest(requestId: Int, userId: Int) {
        viewModelScope.launch {
            try {
                if (apiService.cancelChatRequest(requestId, userId).isSuccessful) fetchChatRequests(userId)
            } catch (e: Exception) {}
        }
    }

    fun sendChatRequest(senderId: Int, receiverId: Int) {
        viewModelScope.launch {
            try {
                if (apiService.sendChatRequest(mapOf("senderId" to senderId, "receiverId" to receiverId)).isSuccessful) fetchChatRequests(senderId)
            } catch (e: Exception) {}
        }
    }

    fun blockUser(blockerId: Int, blockedId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (apiService.blockUser(blockerId, blockedId).isSuccessful) {
                    fetchAllUsers()
                    onSuccess()
                }
            } catch (e: Exception) {}
        }
    }

    fun unblockUser(blockerId: Int, blockedId: Int, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (apiService.unblockUser(blockerId, blockedId).isSuccessful) {
                    fetchAllUsers()
                    onSuccess()
                }
            } catch (e: Exception) {}
        }
    }

    fun fetchBlockedUsers(blockerId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getBlockedUsers(blockerId)
                if (response.isSuccessful) response.body()?.let { _blockedUsers.value = it }
            } catch (e: Exception) {}
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}
