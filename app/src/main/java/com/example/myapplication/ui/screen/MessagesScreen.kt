package com.example.myapplication.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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
    val chatRequests by viewModel.chatRequests.collectAsState()
    
    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    // State to track if we are showing a chat with a specific user (passed from other screens)
    var activeChatUser by remember { mutableStateOf<User?>(null) }
    var showRequestsScreen by remember { mutableStateOf(false) }

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
            viewModel.fetchChatRequests(user.id)
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
        showRequestsScreen = false
        viewModel.setChatTargetUser(null)
    }

    // Calculate pending requests count
    val currentUser = (loginState as? LoginState.Success)?.user
    val pendingRequestCount = remember(chatRequests, currentUser) {
        if (currentUser != null) {
            chatRequests.count { it.receiver.id == currentUser.id && it.status == "PENDING" }
        } else 0
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
    } else if (showRequestsScreen) {
        BackHandler {
            showRequestsScreen = false
        }
        RequestScreen(modifier = modifier, onBack = { showRequestsScreen = false }, onChatClick = { user -> activeChatUser = user; showRequestsScreen = false })
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp) // Removed bottom padding
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Messages",
                    style = MaterialTheme.typography.headlineMedium
                )
                Button(onClick = { showRequestsScreen = true }) {
                    Text("Requests")
                    if (pendingRequestCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge {
                            Text(pendingRequestCount.toString())
                        }
                    }
                }
            }

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
                        .sortedByDescending { user ->
                            // Sort users by the timestamp of the latest message
                            messages
                                .filter { (it.sender.id == currentUser.id && it.receiver.id == user.id) || (it.sender.id == user.id && it.receiver.id == currentUser.id) }
                                .maxOfOrNull { it.sentAt }
                        }

                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(uniqueUsers) { user ->
                            val conversationMessages = messages.filter { (it.sender.id == currentUser.id && it.receiver.id == user.id) || (it.sender.id == user.id && it.receiver.id == currentUser.id) }
                            val lastMessage = conversationMessages.maxByOrNull { it.sentAt }
                            val unreadCount = conversationMessages.count { it.receiver.id == currentUser.id && !it.read }
                            MessageItem(user = user, lastMessage = lastMessage, unreadCount = unreadCount, onClick = { selectedMessage = lastMessage })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageItem(user: User, lastMessage: Message?, unreadCount: Int = 0, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Image
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (user.photoUrl != null) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = user.name.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                lastMessage?.let {
                    Text(
                        text = it.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
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
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } ?: Text(
                    text = "No messages yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Unread count badge
            if (unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
