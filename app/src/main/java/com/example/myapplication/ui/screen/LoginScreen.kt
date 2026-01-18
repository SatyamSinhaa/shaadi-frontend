package com.example.myapplication.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.ui.viewmodel.LoginState
import com.example.myapplication.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.delay

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    onRegisterClick: () -> Unit,
    viewModel: LoginViewModel = viewModel(),
    registrationSuccessMessage: String? = null,
    onMessageShown: () -> Unit = {},
    onGoogleSignInClick: () -> Unit = {}
) {
    val loginState by viewModel.loginState.collectAsState()
    val context = LocalContext.current
    var userInitiatedLogin by remember { mutableStateOf(false) }

    // Auto-hide success message after a delay
    LaunchedEffect(registrationSuccessMessage) {
        if (registrationSuccessMessage != null) {
            delay(2000)
            onMessageShown()
        }
    }

    // Reset the user-initiated flag whenever the login process is not active.
    // This ensures the "Auto logging in..." message can show up again if needed.
    LaunchedEffect(loginState) {
        if (loginState !is LoginState.Loading) {
            userInitiatedLogin = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(180.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Login",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Google Sign-In Button
            Button(
                onClick = {
                    userInitiatedLogin = true
                    onGoogleSignInClick()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = loginState !is LoginState.Loading, // Disable button when loading
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4285F4), // Google Blue
                    contentColor = Color.White
                )
            ) {
                // Show loader inside button if login was started by the user
                if (loginState is LoginState.Loading && userInitiatedLogin) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text("Sign in with Google")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Show auto-login indicator if the app is trying to log in automatically
            if (loginState is LoginState.Loading && !userInitiatedLogin) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Auto logging in...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Show registration success message if present
            if (registrationSuccessMessage != null) {
                Text(
                    text = registrationSuccessMessage,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            // Show error message if login fails
            if (loginState is LoginState.Error) {
                Text(
                    text = (loginState as LoginState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}
