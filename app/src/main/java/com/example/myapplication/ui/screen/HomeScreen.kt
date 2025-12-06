package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.viewmodel.LoginViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(),
    onUserClick: (User) -> Unit = {}
) {
    val users by viewModel.users.collectAsState()
    
    // Filter users if needed, or just show all. Assuming all users are "Matches"
    val nearbyMatches = users 

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
                colors = CardDefaults.cardColors(containerColor = Color(0xFFC62828)) // Reddish color from screenshot
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
                        Text("मोर जोड़ी", color = Color.Yellow, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                        Text("मा आपमन के स्वागत हे", color = Color.White, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("हर समाज, हर समुदाय मन बर खास सुविधा के साथ।", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        Text("अपन जोड़ीदार खोजव अपन मनपसंद ते।", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                    // Image placeholder on right could be added here
                }
            }
        }
        
        // Premium Matches Header
        item(span = { GridItemSpan(2) }) {
             Column {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Premium Matches (0)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
             }
        }

        // Premium Matches Empty State
        item(span = { GridItemSpan(2) }) {
             Column(
                 modifier = Modifier
                     .fillMaxWidth()
                     .padding(vertical = 24.dp),
                 horizontalAlignment = Alignment.CenterHorizontally
             ) {
                 Icon(
                     imageVector = Icons.Filled.Menu, // Using Menu as Crown placeholder if Crown not available
                     contentDescription = "Premium",
                     modifier = Modifier.size(64.dp).padding(bottom = 8.dp),
                     tint = Color.LightGray
                 )
                 Text(
                     text = "No Premium Profiles Available",
                     color = Color.LightGray,
                     style = MaterialTheme.typography.bodyMedium
                 )
             }
        }

        // Nearby Matches Header
        item(span = { GridItemSpan(2) }) {
             Column {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Nearby Matches (${nearbyMatches.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
             }
        }

        // Nearby Matches Slider
        item(span = { GridItemSpan(2) }) {
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
        
        // Bottom spacer to avoid content hiding behind bottom nav
        item(span = { GridItemSpan(2) }) {
            Spacer(modifier = Modifier.height(16.dp))
        }
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
