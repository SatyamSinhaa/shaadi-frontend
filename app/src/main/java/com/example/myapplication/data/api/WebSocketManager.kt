package com.example.myapplication.data.api

import android.util.Log
import com.example.myapplication.data.model.ChatRequest
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.Notification
import com.google.gson.Gson
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import ua.naiksoftware.stomp.dto.StompMessage
import java.util.concurrent.TimeUnit

sealed class WebSocketMessage {
    data class ChatMessage(val message: Message) : WebSocketMessage()
    data class ChatRequestMessage(val chatRequest: ChatRequest) : WebSocketMessage()
    data class NotificationMessage(val notification: Notification) : WebSocketMessage()
}

class WebSocketManager(private val baseUrl: String, private val gson: Gson = Gson()) {

    private var stompClient: StompClient? = null
    private var currentUserId: Int? = null
    private val compositeDisposable = CompositeDisposable()

    private val _messageFlow = MutableSharedFlow<WebSocketMessage>()
    val messageFlow: SharedFlow<WebSocketMessage> = _messageFlow.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var shouldReconnect = false
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 20

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    fun connect(userId: Int) {
        if (_isConnected.value && currentUserId == userId) return

        Log.i(TAG, "🔌 Initiating WebSocket connection for user $userId")
        disconnectInternal()
        currentUserId = userId
        shouldReconnect = true
        reconnectAttempts = 0

        connectInternal()
    }

    private fun connectInternal() {
        val userId = currentUserId ?: return
        val finalWsUrl = if (baseUrl.contains("/ws")) {
            if (baseUrl.endsWith("/websocket")) {
                "$baseUrl?userId=$userId"
            } else {
                "$baseUrl/websocket?userId=$userId"
            }
        } else {
            "$baseUrl/ws/websocket?userId=$userId"
        }
        
        Log.d(TAG, "📡 Connecting STOMP client to $finalWsUrl")

        try {
            val client = Stomp.over(Stomp.ConnectionProvider.OKHTTP, finalWsUrl, null, okHttpClient)
            client.withClientHeartbeat(10000).withServerHeartbeat(10000)
            stompClient = client

            compositeDisposable.clear()

            // --- Robust Subscription Strategy ---
            
            // 1. Standard Spring user queue
            subscribeToPath(client, "/user/queue/messages", "messages")
            
            // 2. Explicit ID based queue (some custom configs use this)
            subscribeToPath(client, "/user/$userId/queue/messages", "messages")
            
            // 3. Generic chat topic (if server broadcasts to topics)
            subscribeToPath(client, "/topic/messages", "messages")

            // 4. Chat Requests & Notifications
            subscribeToPath(client, "/user/queue/chatRequests", "chatRequests")
            subscribeToPath(client, "/user/queue/notifications", "notifications")

            // Lifecycle
            compositeDisposable.add(client.lifecycle().subscribe({ event ->
                when (event.type) {
                    LifecycleEvent.Type.OPENED -> {
                        Log.i(TAG, "🟢 STOMP connection OPENED for user $userId")
                        _isConnected.value = true
                        reconnectAttempts = 0
                    }
                    LifecycleEvent.Type.ERROR -> {
                        Log.e(TAG, "🔴 STOMP connection ERROR: ${event.exception?.message}")
                        _isConnected.value = false
                        attemptReconnect()
                    }
                    LifecycleEvent.Type.CLOSED -> {
                        Log.w(TAG, "🟡 STOMP connection CLOSED")
                        _isConnected.value = false
                        attemptReconnect()
                    }
                    else -> {}
                }
            }, { err -> Log.e(TAG, "❌ Lifecycle error: ${err.message}") }))

            client.connect()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Initialization error", e)
            _isConnected.value = false
            attemptReconnect()
        }
    }

    private fun subscribeToPath(client: StompClient, path: String, type: String) {
        compositeDisposable.add(client.topic(path).subscribe({ msg ->
            Log.i(TAG, "📨 WebSocket data received on $path")
            Log.d(TAG, "📨 Raw JSON Payload: ${msg.payload}")
            handleMessage(msg, type)
        }, { err -> 
            Log.e(TAG, "❌ Subscription error on $path: ${err.message}") 
        }))
    }

    private fun attemptReconnect() {
        if (!shouldReconnect || reconnectAttempts >= maxReconnectAttempts) return
        reconnectAttempts++
        val delayMs = minOf(2000L * reconnectAttempts, 30000L)
        scope.launch {
            delay(delayMs)
            if (shouldReconnect) connectInternal()
        }
    }

    fun disconnect() {
        shouldReconnect = false
        disconnectInternal()
    }

    private fun disconnectInternal() {
        compositeDisposable.clear()
        try { stompClient?.disconnect() } catch (e: Exception) {}
        stompClient = null
        _isConnected.value = false
        currentUserId = null
    }

    private fun handleMessage(stompMessage: StompMessage, type: String) {
        val payload = stompMessage.payload
        scope.launch {
            try {
                when (type) {
                    "messages" -> {
                        val message = gson.fromJson(payload, Message::class.java)
                        _messageFlow.emit(WebSocketMessage.ChatMessage(message))
                    }
                    "chatRequests" -> {
                        val chatRequest = gson.fromJson(payload, ChatRequest::class.java)
                        _messageFlow.emit(WebSocketMessage.ChatRequestMessage(chatRequest))
                    }
                    "notifications" -> {
                        val notification = gson.fromJson(payload, Notification::class.java)
                        _messageFlow.emit(WebSocketMessage.NotificationMessage(notification))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error parsing $type payload. Data: $payload", e)
            }
        }
    }

    companion object {
        private const val TAG = "WebSocketManager"
    }
}
