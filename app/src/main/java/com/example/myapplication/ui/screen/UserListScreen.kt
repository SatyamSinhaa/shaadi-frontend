package com.example.myapplication.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.viewmodel.LoginViewModel
import com.example.myapplication.ui.viewmodel.LoginState

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UserListScreen(modifier: Modifier = Modifier, viewModel: LoginViewModel = viewModel(), onChatClick: (User) -> Unit = {}) {
    val users by viewModel.users.collectAsState()
    val loginState by viewModel.loginState.collectAsState()

    val currentUserId = (loginState as? LoginState.Success)?.user?.id
    val filteredUsers = users.filter { it.id != currentUserId }

    val pagerState = rememberPagerState(pageCount = { filteredUsers.size })

    Box(modifier = modifier.fillMaxSize()) {
        if (filteredUsers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No matches found", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                FullPageUserItem(
                    user = filteredUsers[page],
                    currentUserId = currentUserId ?: 0,
                    viewModel = viewModel,
                    onAction = { action ->
                        when (action) {
                            "Chat" -> onChatClick(filteredUsers[page])
                            "Wishlist" -> viewModel.addFavourite(currentUserId ?: 0, filteredUsers[page].id)
                            // Handle other actions if needed
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun FullPageUserItem(user: User, currentUserId: Int, viewModel: LoginViewModel, onAction: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black) // Fallback color
    ) {
        // Full screen image
        if (user.photoUrl != null) {
            AsyncImage(
                model = user.photoUrl,
                contentDescription = "User photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Placeholder if no image
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "Default user icon",
                    modifier = Modifier.size(100.dp),
                    tint = Color.White
                )
            }
        }

        // Gradient Overlay for text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.2f),
                            Color.Black.copy(alpha = 0.8f)
                        ),
                        startY = 300f
                    )
                )
        )

        // Content Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // User Info
            Text(
                text = "${user.name}, ${user.age ?: "N/A"}",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${user.cityTown ?: "Unknown City"}, ${user.state ?: ""}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${user.gender ?: ""} • ${user.religion ?: ""} • ${user.caste ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
             Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.bio ?: "No bio available",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ActionButton(
                    icon = Icons.Filled.Send, // Using Send for Interest based on screenshot
                    label = "Interest",
                    onClick = { onAction("Interest") }
                )
                ActionButton(
                    icon = Icons.Filled.FavoriteBorder,
                    label = "Wishlist",
                    onClick = { onAction("Wishlist") }
                )
                ActionButton(
                    icon = Icons.Filled.StarBorder,
                    label = "Shortlist",
                    onClick = { onAction("Shortlist") }
                )
                ActionButton(
                    icon = Icons.Filled.ChatBubbleOutline,
                    label = "Chat",
                    onClick = { onAction("Chat") }
                )
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

// Restored UserItem for use in other screens like SearchScreen
@Composable
fun UserItem(user: User, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Square photo on the left
            Box(modifier = Modifier.size(60.dp)) {
                if (user.photoUrl != null) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = "User photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Default icon based on gender
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Default user icon",
                        modifier = Modifier.fillMaxSize(),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            // Name on the right
            Text(text = user.name, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
