package com.example.myapplication.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.data.model.Notification
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.viewmodel.LoginState
import com.example.myapplication.ui.viewmodel.LoginViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
    onRequestClick: (() -> Unit)? = null
) {
    val loginState by viewModel.loginState.collectAsState()
    val notifications by viewModel.notifications.collectAsState()

    var showRequestScreen by remember { mutableStateOf(false) }
    var showUserProfileScreen by remember { mutableStateOf<User?>(null) }
    var chatUser by remember { mutableStateOf<User?>(null) }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            val user = (loginState as LoginState.Success).user
            viewModel.fetchNotifications(user.id)
        }
    }

    if (chatUser != null) {
        BackHandler { chatUser = null }
        MessageDetailScreen(
            modifier = modifier,
            receiver = chatUser!!,
            onBack = { chatUser = null },
            viewModel = viewModel
        )
    } else if (showRequestScreen) {
        BackHandler { showRequestScreen = false }
        RequestScreen(
            modifier = modifier,
            onBack = { showRequestScreen = false },
            onChatClick = { user -> chatUser = user },
            viewModel = viewModel
        )
    } else if (showUserProfileScreen != null) {
        BackHandler { showUserProfileScreen = null }
        UserProfileScreen(
            modifier = modifier,
            user = showUserProfileScreen,
            onBack = { showUserProfileScreen = null },
            onChatClick = { user -> chatUser = user },
            viewModel = viewModel
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Notifications") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(onClick = {
                            val user = (loginState as? LoginState.Success)?.user
                            if (user != null) {
                                viewModel.markAllNotificationsAsRead(user.id)
                            }
                        }) {
                            Text("Read All")
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (notifications.isEmpty()) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No notifications")
                }
            } else {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(notifications) { item ->
                        NotificationRow(
                            item = item,
                            viewModel = viewModel,
                            loginState = loginState,
                            onClick = {
                                if (item.message.contains("sends a request", ignoreCase = true)) {
                                    showRequestScreen = true
                                } else if ((item.message.contains("rejected your chat request", ignoreCase = true) ||
                                            item.message.contains("You sent a request", ignoreCase = true) ||
                                            item.message.contains("accepted your request", ignoreCase = true)) &&
                                            item.relatedUser != null) {
                                    showUserProfileScreen = item.relatedUser
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationRow(
    item: Notification,
    viewModel: LoginViewModel,
    loginState: LoginState,
    onClick: () -> Unit
) {
    val currentUser = (loginState as? LoginState.Success)?.user

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
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
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (item.relatedUser?.photoUrl != null) {
                    AsyncImage(
                        model = item.relatedUser.photoUrl,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = item.relatedUser?.name?.firstOrNull()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.message,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (item.isRead) FontWeight.Normal else FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Format timestamp
                val timeFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                val displayTime = try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val date = dateFormat.parse(item.createdAt)
                    timeFormat.format(date)
                } catch (e: Exception) {
                    item.createdAt // fallback
                }

                Text(
                    text = displayTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Mark as read button if not read
            if (!item.isRead && currentUser != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        item.id?.let { notificationId ->
                            viewModel.markNotificationAsRead(notificationId, currentUser.id)
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = "Mark as read",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
