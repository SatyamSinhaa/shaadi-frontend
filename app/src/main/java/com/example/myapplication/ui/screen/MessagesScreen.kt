package com.example.myapplication.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.viewmodel.LoginViewModel
import com.example.myapplication.ui.viewmodel.LoginState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun MessagesScreen(
    modifier: Modifier = Modifier, 
    onBack: () -> Unit = {}, 
    viewModel: LoginViewModel = viewModel(),
    refreshTrigger: Boolean = false,
    onChatStatusChanged: (Boolean) -> Unit = {}
) {
    val loginState by viewModel.loginState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val chatTargetUser by viewModel.chatTargetUser.collectAsState()
    
    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    // State to track if we are showing a chat with a specific user (passed from other screens)
    var activeChatUser by remember { mutableStateOf<User?>(null) }

    // Determine if chat is visible immediately, to avoid flicker
    val isChatVisible = activeChatUser != null || selectedMessage != null
    
    // Use side effect to notify parent about chat visibility change
    SideEffect {
        onChatStatusChanged(isChatVisible)
    }

    // Reset chat status when leaving the screen (e.g. tab switch)
    DisposableEffect(Unit) {
        onDispose {
            onChatStatusChanged(false)
        }
    }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            val user = (loginState as LoginState.Success).user
            viewModel.fetchMessages(user.id)
        }
    }

    // Effect to handle external chat requests (e.g. from Home screen)
    LaunchedEffect(chatTargetUser) {
        if (chatTargetUser != null) {
            activeChatUser = chatTargetUser
        }
    }

    // Effect to handle refresh/reset (e.g. clicking bottom bar tab again)
    LaunchedEffect(refreshTrigger) {
        selectedMessage = null
        activeChatUser = null
        viewModel.setChatTargetUser(null)
    }

    // Logic to determine what to show
    // If activeChatUser is set, show DetailScreen for that user
    // Else if selectedMessage is set, show DetailScreen for the user in that message
    // Else show list
    
    if (activeChatUser != null) {
        BackHandler {
            activeChatUser = null
            viewModel.setChatTargetUser(null)
        }
        MessageDetailScreen(
            modifier = modifier, 
            receiver = activeChatUser!!, 
            onBack = { 
                activeChatUser = null 
                viewModel.setChatTargetUser(null)
            }, 
            viewModel = viewModel
        )
    } else if (selectedMessage != null) {
        val currentUser = (loginState as? LoginState.Success)?.user
        if (currentUser != null) {
            val message = selectedMessage!!
            val otherUser = if (message.sender.id == currentUser.id) message.receiver else message.sender
            
            BackHandler {
                selectedMessage = null
            }
            MessageDetailScreen(
                modifier = modifier, 
                receiver = otherUser, 
                onBack = { selectedMessage = null }, 
                viewModel = viewModel
            )
        } else {
            // Should not happen if logged in
            selectedMessage = null
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp) // Removed bottom padding
        ) {
            Text(
                text = "Messages",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No messages yet")
                }
            } else {
                val currentUser = (loginState as? LoginState.Success)?.user
                if (currentUser != null) {
                    val uniqueUsers = messages
                        .map { if (it.sender.id == currentUser.id) it.receiver else it.sender }
                        .distinctBy { it.id }

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uniqueUsers) { user ->
                            val lastMessage = messages
                                .filter { (it.sender.id == currentUser.id && it.receiver.id == user.id) || (it.sender.id == user.id && it.receiver.id == currentUser.id) }
                                .maxByOrNull { it.sentAt }
                            MessageItem(user = user, lastMessage = lastMessage, onClick = { selectedMessage = lastMessage })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(user: User, lastMessage: Message?, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = user.name, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            lastMessage?.let {
                Text(
                    text = it.content,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val displayTime = try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val date = dateFormat.parse(it.sentAt)
                    timeFormat.format(date)
                } catch (e: Exception) {
                    it.sentAt // fallback to original if parsing fails
                }
                Text(
                    text = displayTime,
                    style = MaterialTheme.typography.bodySmall
                )
            } ?: Text(
                text = "No messages yet",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
