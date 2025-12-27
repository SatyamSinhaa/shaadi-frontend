package com.example.myapplication.ui.screen

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.myapplication.MainActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.Plan
import com.example.myapplication.ui.viewmodel.PlansViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    plan: Plan,
    viewModel: PlansViewModel,
    onBack: () -> Unit,
    onActivate: () -> Unit,
    activity: MainActivity
) {
    var showPaymentGateway by remember { mutableStateOf(false) }

    if (showPaymentGateway) {
        PaymentGatewayScreen(
            plan = plan,
            onBack = { showPaymentGateway = false },
            activity = activity
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Confirm Payment") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "You are about to purchase:",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    PlanCardDisplay(plan)

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { showPaymentGateway = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "Proceed to Payment",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentGatewayScreen(
    plan: Plan,
    onBack: () -> Unit,
    activity: MainActivity
) {
    var isProcessing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loginViewModel: com.example.myapplication.ui.viewmodel.LoginViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
    val loginState by loginViewModel.loginState.collectAsState()

    // Activity result launcher for payment
    val activityResultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            if (result.resultCode == Activity.RESULT_OK) {
                Log.d("PaymentGateway", "Payment completed successfully")

                // Purchase the subscription after successful payment
                try {
                    val currentUser = when (loginState) {
                        is com.example.myapplication.ui.viewmodel.LoginState.Success -> (loginState as com.example.myapplication.ui.viewmodel.LoginState.Success).user
                        else -> null
                    }

                    if (currentUser != null) {
                        val apiService = com.example.myapplication.data.api.RetrofitClient.apiService
                        val purchaseResponse = apiService.purchaseSubscription(
                            userId = currentUser.id,
                            body = mapOf("planId" to plan.id)
                        )

                        if (purchaseResponse.isSuccessful) {
                            Toast.makeText(context, "Payment successful! Plan activated.", Toast.LENGTH_LONG).show()
                            Log.d("PaymentGateway", "Subscription purchased successfully")
                            // Navigate back to plans screen
                            onBack()
                        } else {
                            Log.e("PaymentGateway", "Failed to purchase subscription: ${purchaseResponse.message()}")
                            Toast.makeText(context, "Payment completed but failed to activate plan. Please contact support.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "User session expired. Please login again.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("PaymentGateway", "Error purchasing subscription: ${e.message}")
                    Toast.makeText(context, "Payment completed but plan activation failed.", Toast.LENGTH_LONG).show()
                }
            } else {
                Log.d("PaymentGateway", "Payment failed or cancelled")
                Toast.makeText(context, "Payment failed or cancelled", Toast.LENGTH_SHORT).show()
            }
            isProcessing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Gateway") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Order Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Order Summary",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Plan:",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = plan.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Amount:",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "₹${plan.price}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    isProcessing = true
                    scope.launch {
                        try {
                            val currentUser = when (loginState) {
                                is com.example.myapplication.ui.viewmodel.LoginState.Success -> (loginState as com.example.myapplication.ui.viewmodel.LoginState.Success).user
                                else -> null
                            }

                            if (currentUser != null) {
                                // Create UPI payment intent
                                val transactionId = "TXN${System.currentTimeMillis()}"

                                try {
                                    // Try different UPI schemes - just launch and let Android handle it
                                    val upiUrls = listOf(
                                        "tez://upi/pay?pa=merchant@tez&pn=ShaadiApp&am=${plan.price}&cu=INR&tn=Payment%20for%20${plan.name}&tr=$transactionId",
                                        "phonepe://pay?pa=merchant@phonepe&pn=ShaadiApp&am=${plan.price}&cu=INR&tn=Payment%20for%20${plan.name}&tr=$transactionId",
                                        "paytmmp://pay?pa=merchant@paytm&pn=ShaadiApp&am=${plan.price}&cu=INR&tn=Payment%20for%20${plan.name}&tr=$transactionId",
                                        "bhim://pay?pa=merchant@bhim&pn=ShaadiApp&am=${plan.price}&cu=INR&tn=Payment%20for%20${plan.name}&tr=$transactionId",
                                        "upi://pay?pa=merchant@upi&pn=ShaadiApp&am=${plan.price}&cu=INR&tn=Payment%20for%20${plan.name}&tr=$transactionId"
                                    )

                                    var intentLaunched = false
                                    for (upiUrl in upiUrls) {
                                        try {
                                            Log.d("PaymentGateway", "Trying UPI scheme: $upiUrl")
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(upiUrl))
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                                            // Try to launch the intent directly
                                            activityResultLauncher.launch(intent)
                                            intentLaunched = true
                                            Log.d("PaymentGateway", "UPI intent launched successfully: $upiUrl")
                                            break

                                        } catch (e: Exception) {
                                            Log.d("PaymentGateway", "UPI scheme $upiUrl failed: ${e.message}")
                                            continue
                                        }
                                    }

                                    if (!intentLaunched) {
                                        // Last resort - try to open Google Play for UPI apps
                                        try {
                                            val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=UPI&c=apps"))
                                            activityResultLauncher.launch(playStoreIntent)
                                            Log.d("PaymentGateway", "Opened Play Store for UPI apps")
                                        } catch (e: Exception) {
                                            Log.e("PaymentGateway", "Failed to open Play Store: ${e.message}")
                                            Toast.makeText(context, "No UPI app found. Please install Google Pay, PhonePe, or Paytm from Play Store.", Toast.LENGTH_LONG).show()
                                            isProcessing = false
                                        }
                                    }

                                } catch (e: Exception) {
                                    isProcessing = false
                                    Log.e("PaymentGateway", "UPI intent failed: ${e.message}")
                                    Toast.makeText(context, "Payment failed to start", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                                isProcessing = false
                            }
                        } catch (e: Exception) {
                            Log.e("PaymentGateway", "Error: ${e.message}")
                            Toast.makeText(context, "Payment failed", Toast.LENGTH_SHORT).show()
                            isProcessing = false
                        }
                    }
                },
                enabled = !isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Pay ₹${plan.price}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentMethodCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                           else MaterialTheme.colorScheme.surface
        ),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary
                      else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PlanCardDisplay(plan: Plan) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(400.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${plan.price}",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                PlanFeature(text = "Chat Limit: ${plan.chatLimit} Messages")
                Spacer(modifier = Modifier.height(12.dp))
                PlanFeature(text = "Duration: ${plan.durationMonths} Months")
                if (plan.isAddon) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Add-on Pack", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
