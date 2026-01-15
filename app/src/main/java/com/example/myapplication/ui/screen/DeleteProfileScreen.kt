package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.ui.viewmodel.LoginState
import com.example.myapplication.ui.viewmodel.LoginViewModel
import androidx.activity.compose.BackHandler
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteProfileScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    val loginState by viewModel.loginState.collectAsState()
    var confirmationText by remember { mutableStateOf("") }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val user = (loginState as? LoginState.Success)?.user

    BackHandler { onBack() }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            title = { Text("Delete Profile") },
            text = { Text("Are you sure you want to delete your profile? By typing 'delete' in the confirmation field, you acknowledge that this action cannot be undone. All your photos will be deleted and you will be logged out.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmationDialog = false
                        isDeleting = true
                        errorMessage = null

                        user?.let {
                            viewModel.deleteProfile(
                                context = context,
                                userId = it.id,
                                confirmationText = confirmationText,
                                onSuccess = {
                                    // Profile deleted successfully, will be logged out automatically
                                },
                                onError = { error ->
                                    isDeleting = false
                                    errorMessage = error
                                }
                            )
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Delete Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Delete Your Profile",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚠️ Warning",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Deleting your profile is permanent and cannot be undone. This will:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Delete your account from our system\n• Remove all your profile and gallery photos\n• End your current subscription\n• Log you out immediately",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = confirmationText,
                onValueChange = { confirmationText = it },
                label = { Text("Type 'delete' to confirm profile deletion") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isDeleting
            )

            errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isDeleting) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Deleting profile...", style = MaterialTheme.typography.bodyMedium)
            } else {
                Button(
                    onClick = {
                        if (confirmationText.lowercase() != "delete") {
                            errorMessage = "Please type 'delete' to confirm"
                        } else {
                            showConfirmationDialog = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete Profile", color = MaterialTheme.colorScheme.onError)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "To proceed with deletion, please type 'delete' in the field above. This confirms your understanding that this action is permanent and irreversible.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}
