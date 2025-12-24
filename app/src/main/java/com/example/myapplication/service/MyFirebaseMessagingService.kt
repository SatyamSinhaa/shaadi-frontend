package com.example.myapplication.service

import android.util.Log
import com.example.myapplication.data.api.RetrofitClient
import com.example.myapplication.ui.viewmodel.LoginState
import com.example.myapplication.util.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(it.title ?: "Notification", it.body ?: "")
        }
        
        // Also handle data payload if provided
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            // Typically you might handle specific data actions here
        }
    }

    private fun showNotification(title: String, message: String) {
        val helper = NotificationHelper(this)
        helper.showNotification(title, message)
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // Store the new token in SharedPreferences
        // MainActivity will check for it and update the server when logged in
        val prefs = getSharedPreferences("shaadi_prefs", MODE_PRIVATE)
        prefs.edit().putString("pending_fcm_token", token).apply()
    }

    companion object {
        private const val TAG = "MyFirebaseMsgService"
    }
}
