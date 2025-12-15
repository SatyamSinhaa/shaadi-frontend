package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
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
    
    // Check if user is blocked
    // The blockedUsers state might not be populated if fetchBlockedUsers wasn't called recently.
    // However, if we came from BlockedProfilesScreen, it should be.
    // We can also fetch it here to be sure or assume isBlocked if we add logic.
    // Let's observe blocked users to check status
    val blockedUsers by viewModel.blockedUsers.collectAsState()
    val isBlocked = remember(blockedUsers, user.id) {
        blockedUsers.any { block ->
            val blocked = block["blocked"] as? Map<*, *>
            val blockedId = (blocked?.get("id") as? Double)?.toInt()
            blockedId == user.id
        }
    }

    // Bottom Sheet State
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

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
            // Ideally we should also ensure blocked users are fetched if we rely on it for UI state
            viewModel.fetchBlockedUsers(currentUserId)
        }
    }

    // Tab state
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Details", "Photos")
    val tabIcons = listOf(Icons.Filled.Info, Icons.Filled.GridOn)

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Options",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Option: Block/Unblock User
                if (isBlocked) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                showBottomSheet = false
                                if (currentUserId != null) {
                                    viewModel.unblockUser(currentUserId, user.id) {
                                        // Refresh blocked users list
                                        viewModel.fetchBlockedUsers(currentUserId)
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.Check, contentDescription = null) // Using Check icon for Unblock or maybe Undo
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = "Unblock User", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { 
                                showBottomSheet = false
                                if (currentUserId != null) {
                                    viewModel.blockUser(currentUserId, user.id) {
                                        // Optionally navigate back after blocking
                                        onBack()
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(text = "Block User", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = user.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showBottomSheet = true }) {
                        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Options")
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
                            // If user is blocked, show ONLY Unblock button here instead of chat/request buttons
                            if (isBlocked) {
                                Button(
                                    onClick = {
                                        if (currentUserId != null) {
                                            viewModel.unblockUser(currentUserId, user.id) {
                                                viewModel.fetchBlockedUsers(currentUserId)
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Unblock")
                                }
                            } else {
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
                        // Photos Grid
                        if (user.photoUrl != null) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(
                                    modifier = Modifier.weight(1f).aspectRatio(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(2.dp)
                                ) {
                                    AsyncImage(
                                        model = user.photoUrl,
                                        contentDescription = "Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                // Placeholder for second item
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No Photos Yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
