package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.Subscription
import com.example.myapplication.ui.viewmodel.PlansViewModel
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionHistoryScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: PlansViewModel = viewModel()
) {
    val history by viewModel.subscriptionHistory.collectAsState()
    val isLoading by viewModel.historyLoading.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Subscription History") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (history.isEmpty()) {
                Text(
                    text = "No subscription history found.",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(history) { subscription ->
                        SubscriptionHistoryItemCard(subscription)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionHistoryItemCard(subscription: Subscription) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = subscription.planName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            val formattedDate = try {
                val parsedDate = try {
                    LocalDateTime.parse(subscription.startDate).toLocalDate()
                } catch (e: Exception) {
                    LocalDate.parse(subscription.startDate)
                }
                DateTimeFormatter.ofPattern("dd MMM yyyy").format(parsedDate)
            } catch (e: Exception) {
                subscription.startDate
            }
            
            val formattedExpiry = try {
                val parsedDate = try {
                    LocalDateTime.parse(subscription.expiryDate).toLocalDate()
                } catch (e: Exception) {
                    LocalDate.parse(subscription.expiryDate)
                }
                DateTimeFormatter.ofPattern("dd MMM yyyy").format(parsedDate)
            } catch (e: Exception) {
                subscription.expiryDate
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Started: $formattedDate", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = subscription.status,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (subscription.status.equals("ACTIVE", ignoreCase = true)) Color.Green else Color.Gray,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Expires: $formattedExpiry", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
