package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.ui.viewmodel.LoginState
import com.example.myapplication.ui.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedProfilesScreen(
    modifier: Modifier = Modifier, 
    onBack: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
    onUserClick: (Int) -> Unit = {}
) {
    val loginState by viewModel.loginState.collectAsState()
    val blockedUsers by viewModel.blockedUsers.collectAsState()
    val currentUser = (loginState as? LoginState.Success)?.user

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            viewModel.fetchBlockedUsers(currentUser.id)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Blocked Profiles") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (blockedUsers.isEmpty()) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text("No blocked profiles.")
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(blockedUsers) { blockData ->
                    // Assuming structure from getBlockedUsers response: List<Map<String, Any>>
                    // where "blocked" is a Map representing the User
                    val blockedUserMap = blockData["blocked"] as? Map<*, *>
                    
                    if (blockedUserMap != null) {
                        val blockedId = (blockedUserMap["id"] as? Double)?.toInt() ?: 0
                        val name = blockedUserMap["name"] as? String ?: "Unknown"
                        val photoUrl = blockedUserMap["photoUrl"] as? String

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onUserClick(blockedId) },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (photoUrl != null) {
                                        AsyncImage(
                                            model = photoUrl,
                                            contentDescription = "Profile Photo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = name.firstOrNull()?.toString() ?: "?",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Button(
                                    onClick = { 
                                        if (currentUser != null) {
                                            viewModel.unblockUser(currentUser.id, blockedId) {
                                                // Refresh happens inside unblockUser success
                                                viewModel.fetchBlockedUsers(currentUser.id)
                                            }
                                        }
                                    }
                                ) {
                                    Text("Unblock")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
