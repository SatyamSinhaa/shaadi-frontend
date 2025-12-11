package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.api.RetrofitClient
import com.example.myapplication.data.model.ChatRequest
import com.example.myapplication.data.model.ErrorResponse
import com.example.myapplication.data.model.Favourite
import com.example.myapplication.data.model.LoginDto
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.model.RegisterDto
import com.example.myapplication.data.model.Subscription
import com.example.myapplication.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LoginViewModel : ViewModel() {
    private val apiService = RetrofitClient.apiService

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

    fun login(userName: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            // Clear previous messages when starting new login
            _messages.value = emptyList()
            
            try {
                val response = apiService.login(LoginDto(userName, password))
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        _loginState.value = LoginState.Success(user)
                        fetchSubscription(user.id)
                        fetchAllUsers()
                    } ?: run {
                        _loginState.value = LoginState.Error("Empty response body")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (errorBody != null) {
                        try {
                            val errorResponse = RetrofitClient.gson.fromJson(errorBody, ErrorResponse::class.java)
                            errorResponse.error
                        } catch (e: Exception) {
                            "Login failed: ${response.message()}"
                        }
                    } else {
                        "Login failed: ${response.message()}"
                    }
                    _loginState.value = LoginState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Exception: ${e.message}")
            }
        }
    }

    private fun fetchSubscription(userId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.getSubscription(userId)
                if (response.isSuccessful) {
                    _subscription.value = response.body()
                } else {
                    _subscription.value = null
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
                        // Update the login state with the updated user
                        _loginState.value = LoginState.Success(updatedUser)
                    }
                } else {
                    // Handle error for updateUser if needed
                }
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    fun logout() {
        _loginState.value = LoginState.Idle
        _users.value = emptyList()
        _favourites.value = emptyList()
        _selectedUser.value = null
        _chatTargetUser.value = null
        _messages.value = emptyList()
        _sendMessageError.value = null
        _subscription.value = null
        _chatRequests.value = emptyList()
        _notifications.value = emptyList()
        _unreadNotificationCount.value = 0
    }

    fun fetchAllUsers() {
        val currentUserGender = (loginState.value as? LoginState.Success)?.user?.gender
        val oppositeGender = when (currentUserGender?.lowercase()) {
            "male" -> "Female"
            "female" -> "Male"
            else -> null
        }
        viewModelScope.launch {
            try {
                val response: Response<List<User>> = apiService.getAllUsers(oppositeGender)
                if (response.isSuccessful) {
                    response.body()?.let { _users.value = it }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun fetchFavourites(userId: Int) {
        viewModelScope.launch {
            try {
                val response: Response<List<Favourite>> = apiService.getFavourites(userId)
                if (response.isSuccessful) {
                    response.body()?.let { _favourites.value = it }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun addFavourite(userId: Int, favouritedUserId: Int) {
        viewModelScope.launch {
            try {
                val response: Response<Unit> = apiService.addFavourite(userId, favouritedUserId)
                if (response.isSuccessful) {
                    fetchFavourites(userId)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun removeFavourite(userId: Int, favouritedUserId: Int) {
        viewModelScope.launch {
            try {
                val response: Response<Unit> = apiService.removeFavourite(userId, favouritedUserId)
                if (response.isSuccessful) {
                    fetchFavourites(userId)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun fetchUserById(userId: Int) {
        _userProfileLoading.value = true
        viewModelScope.launch {
            try {
                val response = apiService.getUserById(userId)
                if (response.isSuccessful) {
                    response.body()?.let { user ->
                        _selectedUser.value = user
                    }
                }
            } catch (e: Exception) {
                // Handle error
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
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun fetchChatRequests(userId: Int) {
        viewModelScope.launch {
            try {
                val response: Response<List<ChatRequest>> = apiService.getChatRequests(userId)
                if (response.isSuccessful) {
                    response.body()?.let { _chatRequests.value = it }
                }
            } catch (e: Exception) {
                // Handle error
            }
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
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun rejectChatRequest(requestId: Int, userId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.rejectChatRequest(requestId, mapOf("userId" to userId))
                if (response.isSuccessful) {
                    fetchChatRequests(userId)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun sendMessage(sender: User, receiver: User, content: String) {
        viewModelScope.launch {
            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val currentTimestamp = dateFormat.format(Date())

                val message = Message(sender = sender, receiver = receiver, content = content, sentAt = currentTimestamp)
                val response: Response<Message> = apiService.sendMessage(message)
                if (response.isSuccessful) {
                    fetchMessages(sender.id)
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (errorBody != null) {
                        try {
                            val errorResponse = RetrofitClient.gson.fromJson(errorBody, ErrorResponse::class.java)
                            errorResponse.error
                        } catch (e: Exception) {
                            "Failed to send message: ${response.message()}"
                        }
                    } else {
                        "Failed to send message: ${response.message()}"
                    }
                    _sendMessageError.value = errorMessage
                }
            } catch (e: Exception) {
                _sendMessageError.value = "Failed to send message: ${e.localizedMessage ?: "Unknown error"}"
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
                val registerDto = RegisterDto(email, password, name, gender)
                val response: Response<User> = apiService.register(registerDto)

                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        _registerState.value = LoginState.Success(user)
                    } else {
                        _registerState.value = LoginState.Error("Registration failed: No user data")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (errorBody != null) {
                        try {
                            val errorResponse = RetrofitClient.gson.fromJson(errorBody, ErrorResponse::class.java)
                            errorResponse.error
                        } catch (e: Exception) {
                            "Registration failed: ${response.message()}"
                        }
                    } else {
                        "Registration failed: ${response.message()}"
                    }
                    _registerState.value = LoginState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _registerState.value = LoginState.Error("Registration failed: ${e.localizedMessage ?: "Unknown error"}")
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
                if (response.isSuccessful) {
                    // Refresh messages after marking as read
                    fetchMessages(receiverId)
                }
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    fun fetchNotifications(userId: Int) {
        viewModelScope.launch {
            try {
                val response: Response<List<Notification>> = apiService.getNotifications(userId)
                if (response.isSuccessful) {
                    response.body()?.let { _notifications.value = it }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun fetchUnreadNotificationCount(userId: Int) {
        viewModelScope.launch {
            try {
                val response: Response<Map<String, Long>> = apiService.getUnreadNotificationCount(userId)
                if (response.isSuccessful) {
                    response.body()?.let { map ->
                        _unreadNotificationCount.value = map["count"]?.toInt() ?: 0
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun markNotificationAsRead(notificationId: Long, userId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.markNotificationAsRead(notificationId, mapOf("userId" to userId))
                if (response.isSuccessful) {
                    // Refresh notifications and unread count after marking as read
                    fetchNotifications(userId)
                    fetchUnreadNotificationCount(userId)
                }
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    fun markAllNotificationsAsRead(userId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.markAllNotificationsAsRead(userId)
                if (response.isSuccessful) {
                    // Refresh notifications and unread count after marking all as read
                    fetchNotifications(userId)
                    fetchUnreadNotificationCount(userId)
                }
            } catch (e: Exception) {
                // Handle error if needed
            }
        }
    }

    fun getChatRequestStatus(currentUserId: Int, otherUserId: Int): String? {
        // Check if there's a pending request between these users
        val pendingRequests = chatRequests.value
        return pendingRequests.find { request ->
            (request.sender.id == currentUserId && request.receiver.id == otherUserId) ||
            (request.sender.id == otherUserId && request.receiver.id == currentUserId)
        }?.status
    }

    fun canChatWithUser(currentUserId: Int, otherUserId: Int): Boolean {
        // Check if there's an accepted request between these users
        val pendingRequests = chatRequests.value
        return pendingRequests.any { request ->
            ((request.sender.id == currentUserId && request.receiver.id == otherUserId) ||
             (request.sender.id == otherUserId && request.receiver.id == currentUserId)) &&
            request.status == "ACCEPTED"
        }
    }

    fun cancelChatRequest(requestId: Int, userId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.cancelChatRequest(requestId, userId)
                if (response.isSuccessful) {
                    fetchChatRequests(userId)
                    // Also refresh notifications to remove the canceled request notification
                    fetchNotifications(userId)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun getChatRequestId(currentUserId: Int, otherUserId: Int): Int? {
        // Find the request ID between these users
        val pendingRequests = chatRequests.value
        return pendingRequests.find { request ->
            (request.sender.id == currentUserId && request.receiver.id == otherUserId) ||
            (request.sender.id == otherUserId && request.receiver.id == currentUserId)
        }?.id
    }

    fun sendChatRequest(senderId: Int, receiverId: Int) {
        viewModelScope.launch {
            try {
                val response = apiService.sendChatRequest(mapOf("senderId" to senderId, "receiverId" to receiverId))
                if (response.isSuccessful) {
                    fetchChatRequests(senderId)
                    // Also refresh notifications to show the new sent request notification
                    fetchNotifications(senderId)
                    fetchUnreadNotificationCount(senderId)
                }
            } catch (e: Exception) {
                // Handle error - could add error state here if needed
            }
        }
    }
}

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: User) : LoginState()
    data class Error(val message: String) : LoginState()
}
