package com.example.myapplication.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.api.ApiService
import com.example.myapplication.data.api.RetrofitClient
import com.example.myapplication.data.model.ErrorResponse
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatViewModel(private val apiService: ApiService = RetrofitClient.apiService) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _sendMessageError = MutableStateFlow<String?>(null)
    val sendMessageError: StateFlow<String?> = _sendMessageError.asStateFlow()

    private val _chatTargetUser = MutableStateFlow<User?>(null)
    val chatTargetUser: StateFlow<User?> = _chatTargetUser.asStateFlow()

    fun fetchMessages(userId: Int) {
        viewModelScope.launch {
            try {
                val response: Response<List<Message>> = apiService.getMessages(userId)
                if (response.isSuccessful) {
                    response.body()?.let { _messages.value = it }
                }
            } catch (e: Exception) {
                // Handle error (logging or state update)
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

    fun setChatTargetUser(user: User?) {
        _chatTargetUser.value = user
    }

    fun clearSendMessageError() {
        _sendMessageError.value = null
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }
}
