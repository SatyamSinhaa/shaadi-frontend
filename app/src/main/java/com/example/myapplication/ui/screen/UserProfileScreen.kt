package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
    onChatClick: (User) -> Unit = {}
) {
    if (user == null) return

    val loginState by viewModel.loginState.collectAsState()
    val currentUserId = (loginState as? LoginState.Success)?.user?.id
    val favourites by viewModel.favourites.collectAsState()

    // Check if this user is in favourites
    val isFavourite = favourites.any { it.favouritedUser.id == user.id }

    // Fetch favourites when screen loads to ensure state is up to date
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            viewModel.fetchFavourites(currentUserId)
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
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Header (Photo, Name, Email, Actions)
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                                .size(120.dp)
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
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier.height(120.dp)
                        ) {
                            Column {
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
                            }
                            
                            // Action Buttons (Wishlist, Message)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (currentUserId != null) {
                                            if (isFavourite) {
                                                viewModel.removeFavourite(currentUserId, user.id)
                                            } else {
                                                viewModel.addFavourite(currentUserId, user.id)
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = "Wishlist",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                
                                IconButton(
                                    onClick = { onChatClick(user) },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ChatBubbleOutline,
                                        contentDescription = "Message",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

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
    }
}
