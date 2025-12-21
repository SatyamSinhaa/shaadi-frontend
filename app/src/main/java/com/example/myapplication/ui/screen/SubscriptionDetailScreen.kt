package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.viewmodel.LoginState
import com.example.myapplication.ui.viewmodel.LoginViewModel
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(),
    onBack: () -> Unit
) {
    val subscription by viewModel.subscription.collectAsState()
    val loginState by viewModel.loginState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Subscription Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (loginState) {
                is LoginState.Success -> {
                    if (subscription != null) {
                        ProfileSection(title = "Current Subscription") {
                            val formattedExpiry = try {
                                val parsedDate = try {
                                    java.time.LocalDateTime.parse(subscription!!.expiryDate).toLocalDate()
                                } catch (e: Exception) {
                                    java.time.LocalDate.parse(subscription!!.expiryDate)
                                }
                                DateTimeFormatter.ofPattern("dd MMM yyyy").format(parsedDate)
                            } catch (e: Exception) {
                                subscription!!.expiryDate
                            }
                            ProfileField(label = "Plan Name", value = subscription!!.planName)
                            ProfileField(label = "Plan Duration (Months)", value = subscription!!.planDurationMonths.toString())
                            ProfileField(label = "Expiry Date", value = formattedExpiry)
                            ProfileField(label = "Chat Limit", value = subscription!!.chatLimit.toString())
                            ProfileField(label = "Plan Chat Limit", value = subscription!!.planChatLimit.toString())
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No active subscription found.")
                        }
                    }
                }
                else -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Please log in to view subscription details.")
                    }
                }
            }
        }
    }
}
