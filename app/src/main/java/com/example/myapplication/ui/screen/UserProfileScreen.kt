package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.viewmodel.LoginViewModel

@Composable
fun UserProfileScreen(modifier: Modifier = Modifier, user: User?, onBack: () -> Unit, viewModel: LoginViewModel = viewModel()) {
    if (user == null) return
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        Button(onClick = onBack) {
            Text("Back")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "User Profile",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text("Name: ${user.name}", style = MaterialTheme.typography.bodyLarge)
        Text("Email: ${user.email}", style = MaterialTheme.typography.bodyMedium)
        user.age?.let { Text("Age: $it", style = MaterialTheme.typography.bodyMedium) }
        user.gender?.let { Text("Gender: $it", style = MaterialTheme.typography.bodyMedium) }
        user.religion?.let { Text("Religion: $it", style = MaterialTheme.typography.bodyMedium) }
        user.cityTown?.let { Text("City/Town: $it", style = MaterialTheme.typography.bodyMedium) }
        user.district?.let { Text("District: $it", style = MaterialTheme.typography.bodyMedium) }
        user.state?.let { Text("State: $it", style = MaterialTheme.typography.bodyMedium) }
        user.bio?.let { Text("Bio: $it", style = MaterialTheme.typography.bodyMedium) }
    }
}
