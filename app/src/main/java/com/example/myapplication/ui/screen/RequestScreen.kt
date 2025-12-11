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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.viewmodel.LoginState
import com.example.myapplication.ui.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    onChatClick: (User) -> Unit = {},
    viewModel: LoginViewModel = viewModel()
) {
    val loginState by viewModel.loginState.collectAsState()
    val allChatRequests by viewModel.chatRequests.collectAsState()

    var showUserProfileScreen by remember { mutableStateOf<User?>(null) }

    // Filter to only show pending requests where current user is the receiver
    val currentUserId = (loginState as? LoginState.Success)?.user?.id
    val chatRequests = allChatRequests.filter { request ->
        request.status == "PENDING" && request.receiver.id == currentUserId
    }

    LaunchedEffect(loginState) {
        if (loginState is LoginState.Success) {
            val user = (loginState as LoginState.Success).user
            viewModel.fetchChatRequests(user.id)
        }
    }

    if (showUserProfileScreen != null) {
        BackHandler { showUserProfileScreen = null }
        UserProfileScreen(
            modifier = modifier,
            user = showUserProfileScreen,
            onBack = { showUserProfileScreen = null },
            onChatClick = onChatClick,
            onAcceptRequest = {}, // Changed from onChatClick to {} to prevent auto-opening chat
            viewModel = viewModel
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Requests") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (chatRequests.isEmpty()) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No pending requests")
                }
            } else {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(chatRequests) { request ->
                        // Pass the current logged-in user's ID to the accept/reject functions
                        val currentUserId = (loginState as? LoginState.Success)?.user?.id ?: 0
                        RequestItem(
                            user = request.sender,
                            onAccept = { viewModel.acceptChatRequest(request.id, currentUserId, onSuccess = {}) },
                            onReject = { viewModel.rejectChatRequest(request.id, currentUserId) },
                            onClick = { showUserProfileScreen = request.sender }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RequestItem(
    user: User,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
            // Circular Profile Image
            Box(
                modifier = Modifier
                    .size(64.dp)
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
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Name
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            // Action Buttons
            Row {
                IconButton(
                    onClick = onReject,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Reject")
                }
                
                IconButton(
                    onClick = onAccept,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.Green)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Accept")
                }
            }
        }
    }
}
