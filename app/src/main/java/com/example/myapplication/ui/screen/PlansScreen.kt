package com.example.myapplication.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.Plan
import com.example.myapplication.data.model.Subscription
import com.example.myapplication.ui.viewmodel.LoginViewModel
import com.example.myapplication.ui.viewmodel.PlansViewModel
import com.example.myapplication.ui.viewmodel.LoginState
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PlansScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: PlansViewModel = viewModel(),
    loginViewModel: LoginViewModel = viewModel()
) {
    val plans by viewModel.plans.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val loginState by loginViewModel.loginState.collectAsState()
    
    var selectedPlan by remember { mutableStateOf<Plan?>(null) }

    // If a plan is selected, show PaymentScreen
    if (selectedPlan != null) {
        PaymentScreen(
            plan = selectedPlan!!,
            viewModel = viewModel,
            onBack = { 
                selectedPlan = null 
                viewModel.resetPurchaseState()
            },
            onActivate = {
                val userId = (loginState as? LoginState.Success)?.user?.id
                if (userId != null) {
                    viewModel.purchaseSubscription(userId, selectedPlan!!.id)
                }
            }
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
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
                    if (plans.isNotEmpty()) {
                        val pagerState = rememberPagerState(pageCount = { plans.size })
                        
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top
                        ) {
                            Text(
                                text = "Choose Your Plan",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
                            )

                            HorizontalPager(
                                state = pagerState,
                                contentPadding = PaddingValues(horizontal = 32.dp),
                                pageSpacing = 16.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) { page ->
                                PlanCard(
                                    plan = plans[page],
                                    onSelect = { selectedPlan = plans[page] }
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            // Page Indicator
                            Row(
                                Modifier
                                    .height(50.dp)
                                    .fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(plans.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                    Box(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .clip(RoundedCornerShape(50)) // Clip to circle
                                            .background(color)
                                            .size(10.dp)
                                    )
                                }
                            }
                        }
                    } else {
                         Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No plans available")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionHistoryItem(subscription: Subscription) {
    Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
        Text(
            text = subscription.planName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
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
            
            Text("Started: $formattedDate", style = MaterialTheme.typography.bodySmall)
            Text(subscription.status, style = MaterialTheme.typography.labelMedium, color = if(subscription.status.equals("ACTIVE", ignoreCase = true)) Color.Green else Color.Gray)
        }
    }
}

@Composable
fun PlanCard(plan: Plan, onSelect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp), // Big card height
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header with price and name
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "₹${plan.price}",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "for ${plan.durationMonths} months",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Features list
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    PlanFeature(text = "Chat Limit: ${plan.chatLimit} Messages")
                    Spacer(modifier = Modifier.height(12.dp))
                    PlanFeature(text = "Duration: ${plan.durationMonths} Months")
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (plan.isAddon) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Add-on Pack",
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Button(
                    onClick = onSelect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Select Plan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PlanFeature(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.DarkGray
        )
    }
}
