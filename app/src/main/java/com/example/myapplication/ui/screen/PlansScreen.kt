package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.Plan
import com.example.myapplication.ui.viewmodel.PlansViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: PlansViewModel = viewModel()
) {
    val plans by viewModel.plans.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium Plans") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("< Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null -> {
                    Text(
                        text = error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(plans) { plan ->
                            PlanItem(plan = plan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlanItem(plan: Plan) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = plan.name,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Duration: ${plan.durationMonths} months",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Price: $${plan.price}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Chat Limit: ${plan.chatLimit}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Published: ${if (plan.isPublished) "Yes" else "No"}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Addon: ${if (plan.isAddon) "Yes" else "No"}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
