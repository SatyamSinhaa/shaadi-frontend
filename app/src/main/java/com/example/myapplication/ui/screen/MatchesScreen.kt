package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.viewmodel.MatchesViewModel
import kotlinx.coroutines.launch

@Composable
fun MatchesScreen(
    modifier: Modifier = Modifier,
    currentUserId: Int,
    viewModel: MatchesViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Set current user ID when screen is first composed
    LaunchedEffect(currentUserId) {
        viewModel.setCurrentUserId(currentUserId)
        viewModel.loadInitialUsers()
    }

    // Observe scroll position for prefetching
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            Pair(layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0, layoutInfo.totalItemsCount)
        }.collect { (lastVisibleItemIndex, totalItemsCount) ->
            if (viewModel.shouldLoadMore(totalItemsCount, lastVisibleItemIndex)) {
                coroutineScope.launch {
                    viewModel.loadMoreUsers()
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading && uiState.users.isEmpty() -> {
                // Initial loading state
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            uiState.error != null && uiState.users.isEmpty() -> {
                // Error state for initial load
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Failed to load matches",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.error ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.retry() }) {
                        Text("Retry")
                    }
                }
            }

            uiState.users.isNotEmpty() -> {
                // Show users list
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(uiState.users) { index, user ->
                        UserCard(
                            user = user,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Loading more indicator at the end
                    if (uiState.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }

                    // Error indicator for loading more
                    if (uiState.error != null && uiState.users.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Failed to load more matches",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { viewModel.retry() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                }
            }

            else -> {
                // Empty state
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No matches found",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Check back later for new matches!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: User,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Profile Image
            AsyncImage(
                model = user.photoUrl ?: "https://via.placeholder.com/150",
                contentDescription = "Profile picture of ${user.name}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Name and Age
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                user.age?.let { age ->
                    Text(
                        text = "$age years",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Location
            user.cityTown?.let { city ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📍 $city",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    user.state?.let { state ->
                        Text(
                            text = ", $state",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bio
            user.bio?.let { bio ->
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Religion and other details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                user.religion?.let {
                    Chip(text = it)
                }
                user.gotr?.let {
                    Chip(text = "Gotra: $it")
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
