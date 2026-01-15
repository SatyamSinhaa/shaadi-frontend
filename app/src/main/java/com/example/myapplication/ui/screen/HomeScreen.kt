package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.R
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.viewmodel.LoginViewModel
import com.example.myapplication.ui.viewmodel.LoginState

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(),
    onUserClick: (User) -> Unit = {}
) {
    val users by viewModel.users.collectAsState()
    val loginState by viewModel.loginState.collectAsState()
    
    // Filter users based on logged-in user's district
    val nearbyMatches = remember(users, loginState) {
        val currentUser = (loginState as? LoginState.Success)?.user
        if (currentUser?.district != null) {
            users.filter { 
                it.district.equals(currentUser.district, ignoreCase = true) && 
                it.id != currentUser.id // Exclude current user from matches
            }
        } else {
            // Fallback: Show all users if district is missing, or empty list? 
             users.filter { it.id != currentUser?.id }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Banner Section
        item(span = { GridItemSpan(2) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Decorative text mimicking the screenshot
                     Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(0.7f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("|| जय जोहार ||", color = Color.White, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("हमर जोड़ी", color = Color.Yellow, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                        Text("मा आपमन के स्वागत हे", color = Color.White, fontSize = 20.sp)
                    }
                    // Image placeholder on right could be added here
                }
            }
        }
        
        // Nearby Matches Header
        item(span = { GridItemSpan(2) }) {
             Column {
                Spacer(modifier = Modifier.height(16.dp))
                val currentUser = (loginState as? LoginState.Success)?.user
                val locationText = if (currentUser?.district != null) " in ${currentUser.district}" else ""
                
                Text(
                    text = "Nearby Matches$locationText (${nearbyMatches.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
             }
        }

        // Nearby Matches Slider
        item(span = { GridItemSpan(2) }) {
            if (nearbyMatches.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Text("No matches found in your district yet.")
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(nearbyMatches) { user ->
                        MatchCard(
                            user = user, 
                            onClick = { onUserClick(user) },
                            modifier = Modifier.width(200.dp).height(280.dp)
                        )
                    }
                }
            }
        }

        // Social Media Buttons Section
        item(span = { GridItemSpan(2) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Follow Us",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SocialMediaButton(
                        name = "Facebook",
                        color = Color(0xFF1877F2),
                        painter = painterResource(id = R.drawable.ic_facebook)
                    )
                    SocialMediaButton(
                        name = "Instagram",
                        color = Color(0xFFE4405F),
                        painter = painterResource(id = R.drawable.ic_instagram)
                    )
                    SocialMediaButton(
                        name = "YouTube",
                        color = Color(0xFFFF0000),
                        painter = painterResource(id = R.drawable.ic_youtube)
                    )
                }
            }
        }
    }
}

@Composable
fun SocialMediaButton(
    name: String,
    color: Color,
    painter: Painter,
    onClick: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painter,
                contentDescription = name,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MatchCard(user: User, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onClick
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image
            if (user.photoUrl != null) {
                AsyncImage(
                    model = user.photoUrl,
                    contentDescription = user.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                     Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                }
            }

            // Gradient & Text Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.1f), Color.Black.copy(alpha = 0.8f)),
                            startY = 300f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${user.age ?: "?"} yrs • 160 cm", 
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Text(
                    text = "${user.religion ?: ""} | ${user.caste ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Text(
                    text = "${user.cityTown ?: ""}, ${user.state ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }
        }
    }
}
