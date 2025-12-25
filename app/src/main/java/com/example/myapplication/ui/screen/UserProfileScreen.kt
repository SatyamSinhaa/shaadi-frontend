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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
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

    // Photo Enlargement State
    var showEnlargedPhoto by remember { mutableStateOf(false) }

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
            viewModel.fetchBlockedUsers(currentUserId)
        }
    }

    // Tab state
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Details", "Photos")
    val tabIcons = listOf(Icons.Filled.Info, Icons.Filled.GridOn)

    if (showEnlargedPhoto && user.photoUrl != null) {
        EnlargedPhotoDialog(
            photoUrl = user.photoUrl,
            onDismiss = { showEnlargedPhoto = false }
        )
    }

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
                                        viewModel.fetchBlockedUsers(currentUserId)
                                    }
                                }
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.Check, contentDescription = null)
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
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { if (user.photoUrl != null) showEnlargedPhoto = true },
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
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.weight(1f)
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
                                    if (canChat) {
                                        Button(onClick = { onChatClick(user) }) {
                                            Icon(Icons.Filled.ChatBubbleOutline, contentDescription = "Chat")
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Chat")
                                        }
                                    } else if (chatRequest != null && chatRequest.status == "PENDING") {
                                        if (chatRequest.sender.id == currentUserId) {
                                            Button(
                                                onClick = {
                                                    viewModel.cancelChatRequest(chatRequest.id, currentUserId ?: 0)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                            ) {
                                                Text("Cancel Request")
                                            }
                                        } else {
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
                                        Button(
                                            onClick = {
                                                viewModel.sendChatRequest(currentUserId ?: 0, user.id)
                                            }
                                        ) {
                                            Icon(Icons.Filled.Send, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Send Request")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )

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

            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
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

                        ProfileSection(title = "Basic Information") {
                            if (user.age != null) ProfileField(label = "Age", value = user.age.toString())
                            if (user.gender != null) ProfileField(label = "Gender", value = user.gender)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        ProfileSection(title = "Personal Details") {
                            if (user.gotr != null) ProfileField(label = "Gotr", value = user.gotr)
                            if (user.caste != null) ProfileField(label = "Caste", value = user.caste)
                            if (user.category != null) ProfileField(label = "Category", value = user.category)
                            if (user.religion != null) ProfileField(label = "Religion", value = user.religion)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        ProfileSection(title = "Location") {
                            if (user.cityTown != null) ProfileField(label = "City/Town", value = user.cityTown)
                            if (user.district != null) ProfileField(label = "District", value = user.district)
                            if (user.state != null) ProfileField(label = "State", value = user.state)
                        }
                    }
                    1 -> {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No Photos Yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
