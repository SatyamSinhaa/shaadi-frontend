package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.viewmodel.LoginState
import com.example.myapplication.ui.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    modifier: Modifier = Modifier,
    user: User?,
    onBack: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
    onChatClick: (User) -> Unit = {},
    onAcceptRequest: (User) -> Unit = {}
) {
    if (user == null) return

    val loginState by viewModel.loginState.collectAsState()
    val currentUserId = (loginState as? LoginState.Success)?.user?.id
    val chatRequests by viewModel.chatRequests.collectAsState()

    // Find the relevant chat request
    val chatRequest = remember(chatRequests, currentUserId, user.id) {
        if (currentUserId == null) null
        else chatRequests.find {
            (it.sender.id == currentUserId && it.receiver.id == user.id) ||
            (it.sender.id == user.id && it.receiver.id == currentUserId)
        }
    }

    val canChat = chatRequest?.status == "ACCEPTED"

    // Fetch chat requests when screen loads
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            viewModel.fetchChatRequests(currentUserId)
        }
    }

    // Tab state
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Details", "Photos")
    val tabIcons = listOf(Icons.Filled.Info, Icons.Filled.GridOn)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = user.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Header (Photo, Name, Email, Actions)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                content = {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Photo
                        Box(
                            modifier = Modifier
                                .size(100.dp) // Matched to ProfileScreen
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
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        
                        Column(
                            verticalArrangement = Arrangement.Center, // Matched to ProfileScreen
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.weight(1f) // Ensure column takes remaining space
                        ) {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))

                            // Action Buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Dynamic Chat/Request Button
                                if (canChat) {
                                    // Show chat icon if request is accepted
                                    Button(onClick = { onChatClick(user) }) {
                                        Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Chat")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Chat")
                                    }
                                } else if (chatRequest != null && chatRequest.status == "PENDING") {
                                    // PENDING REQUEST
                                    if (chatRequest.sender.id == currentUserId) {
                                        // Current user sent the request -> Show Cancel
                                        Button(
                                            onClick = {
                                                viewModel.cancelChatRequest(chatRequest.id, currentUserId ?: 0)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Text("Cancel Request")
                                        }
                                    } else {
                                        // Current user received the request -> Show Accept/Reject
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Button(
                                                onClick = {
                                                    viewModel.acceptChatRequest(chatRequest.id, currentUserId ?: 0, onSuccess = {
                                                        onAcceptRequest(user)
                                                    })
                                                },
                                            ) {
                                                Icon(Icons.Filled.Check, contentDescription = "Accept")
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Accept")
                                            }
                                            Button(
                                                onClick = { viewModel.rejectChatRequest(chatRequest.id, currentUserId ?: 0) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                            ) {
                                                Text("Reject", color = MaterialTheme.colorScheme.onErrorContainer)
                                            }
                                        }
                                    }
                                } else {
                                    // Show send request button if no request exists or it was rejected/cancelled
                                    Button(
                                        onClick = {
                                            viewModel.sendChatRequest(currentUserId ?: 0, user.id)
                                        }
                                    ) {
                                        Text("Send Request")
                                    }
                                }
                            }
                        }
                    }
                }
            )

            // Tab Row (Instagram Style)
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = { Icon(tabIcons[index], contentDescription = title) }
                    )
                }
            }

            // Content below tabs
            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // Bio Section
                        if (user.bio != null) {
                            ProfileSection(title = "About Me") {
                                Text(
                                    text = user.bio,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Basic Info Section
                        ProfileSection(title = "Basic Information") {
                            if (user.age != null) ProfileField(label = "Age", value = user.age.toString())
                            if (user.gender != null) ProfileField(label = "Gender", value = user.gender)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Personal Details Section
                        ProfileSection(title = "Personal Details") {
                            if (user.gotr != null) ProfileField(label = "Gotr", value = user.gotr)
                            if (user.caste != null) ProfileField(label = "Caste", value = user.caste)
                            if (user.category != null) ProfileField(label = "Category", value = user.category)
                            if (user.religion != null) ProfileField(label = "Religion", value = user.religion)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Location Section
                        ProfileSection(title = "Location") {
                            if (user.cityTown != null) ProfileField(label = "City/Town", value = user.cityTown)
                            if (user.district != null) ProfileField(label = "District", value = user.district)
                            if (user.state != null) ProfileField(label = "State", value = user.state)
                        }
                    }
                    1 -> {
                        // Empty Tab 2 (Photos)
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No Photos Yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
