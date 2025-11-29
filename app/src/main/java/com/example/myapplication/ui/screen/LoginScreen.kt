package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.viewmodel.LoginState
import com.example.myapplication.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect

@Composable
fun LoginScreen(modifier: Modifier = Modifier, onRegisterClick: () -> Unit, viewModel: LoginViewModel = viewModel(), registrationSuccessMessage: String? = null, onMessageShown: () -> Unit = {}) {
    var email by remember { mutableStateOf("user1@gmail.com") }
    var password by remember { mutableStateOf("123") }
    val loginState by viewModel.loginState.collectAsState()
    val users by viewModel.users.collectAsState()

    // Auto-hide success message after 2 seconds
    LaunchedEffect(registrationSuccessMessage) {
        if (registrationSuccessMessage != null) {
            delay(2000)
            onMessageShown()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.login(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = loginState !is LoginState.Loading
        ) {
            if (loginState is LoginState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Login")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onRegisterClick) {
            Text("Create a new account")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Show registration success message if present
        if (registrationSuccessMessage != null) {
            Text(
                text = registrationSuccessMessage,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        when (loginState) {
            is LoginState.Success -> {
                val user = (loginState as LoginState.Success).user
                Text(
                    text = "Welcome, ${user.name}!",
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is LoginState.Error -> {
                Text(
                    text = (loginState as LoginState.Error).message,
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }
    }
}
