package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
            val scope = rememberCoroutineScope()
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

    // Removed the nested Scaffold and TopAppBar to fix the excessive top spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Custom Header with Back Button and "Profile" label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = "Profile",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showBottomSheet = true }) {
                Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Options")
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Header (Photo, Name, Email, Actions)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                                .size(90.dp)
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
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))

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
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Unblock")
                                }
                            } else {
                                if (canChat) {
                                    Button(
                                        onClick = { onChatClick(user) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Chat")
                                    }
                                } else if (chatRequest != null && chatRequest.status == "PENDING") {
                                    if (chatRequest.sender.id == currentUserId) {
                                        Button(
                                            onClick = {
                                                viewModel.cancelChatRequest(chatRequest.id, currentUserId ?: 0)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Cancel Request")
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    viewModel.acceptChatRequest(chatRequest.id, currentUserId ?: 0, onSuccess = {
                                                        onAcceptRequest(user)
                                                    })
                                                },
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Accept")
                                            }
                                            Button(
                                                onClick = { viewModel.rejectChatRequest(chatRequest.id, currentUserId ?: 0) },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                                                modifier = Modifier.weight(0.8f)
                                            ) {
                                                Text("Reject", color = MaterialTheme.colorScheme.onErrorContainer)
                                            }
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            viewModel.sendChatRequest(currentUserId ?: 0, user.id)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Send Request")
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
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = { Icon(tabIcons[index], contentDescription = title, modifier = Modifier.size(20.dp)) },
                        text = { Text(title, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        if (user.bio != null && user.bio.isNotBlank()) {
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
                            if (user.maritalStatus != null) ProfileField(label = "Marital Status", value = user.maritalStatus)
                            if (user.manglik != null) ProfileField(label = "Manglik", value = if (user.manglik == true) "Yes" else "No")
                            if (user.dateOfBirth != null) ProfileField(label = "Date of Birth", value = user.dateOfBirth)
                            if (user.height != null) ProfileField(label = "Height", value = formatHeightForDisplay(user.height))
                            if (user.annualIncome != null) ProfileField(label = "Annual Income", value = formatIncomeForDisplay(user.annualIncome))
                            if (user.weightKg != null) ProfileField(label = "Weight (kg)", value = user.weightKg.toString())
                            if (user.rashi != null) ProfileField(label = "Rashi", value = user.rashi)
                            if (user.profession != null) ProfileField(label = "Profession", value = user.profession)
                            if (user.education != null) ProfileField(label = "Education", value = user.education)
                            if (user.motherTongue != null) ProfileField(label = "Mother Tongue", value = user.motherTongue)
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
                            if (user.address != null) ProfileField(label = "Address", value = user.address)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        ProfileSection(title = "Family Details") {
                            if (user.fatherName != null) ProfileField(label = "Father Name", value = user.fatherName)
                            if (user.fatherOccupation != null) ProfileField(label = "Father Occupation", value = user.fatherOccupation)
                            if (user.motherName != null) ProfileField(label = "Mother Name", value = user.motherName)
                            if (user.motherOccupation != null) ProfileField(label = "Mother Occupation", value = user.motherOccupation)
                            if (user.numberOfBrothers != null) ProfileField(label = "Number of Brothers", value = user.numberOfBrothers.toString())
                            if (user.numberOfSisters != null) ProfileField(label = "Number of Sisters", value = user.numberOfSisters.toString())
                            if (user.familyType != null) ProfileField(label = "Family Type", value = user.familyType)
                            if (user.familyLocations != null) ProfileField(label = "Family Locations", value = user.familyLocations)
                            if (user.property != null) ProfileField(label = "Property", value = user.property)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        ProfileSection(title = "Lifestyle") {
                            if (user.diet != null) ProfileField(label = "Diet", value = user.diet)
                            if (user.smoking != null) ProfileField(label = "Smoking", value = if (user.smoking == true) "Yes" else "No")
                            if (user.drinking != null) ProfileField(label = "Drinking", value = if (user.drinking == true) "Yes" else "No")
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
