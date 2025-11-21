package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.viewmodel.LoginViewModel
import com.example.myapplication.ui.viewmodel.LoginState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun ProfileScreen(modifier: Modifier = Modifier, viewModel: LoginViewModel = viewModel()) {
    val loginState by viewModel.loginState.collectAsState()
    var showEditForm by remember { mutableStateOf(false) }

    when (loginState) {
        is LoginState.Success -> {
            val user = (loginState as LoginState.Success).user

            if (showEditForm) {
                EditProfileForm(modifier = modifier, user = user, onSave = { updatedUser ->
                    viewModel.updateUser(updatedUser)
                    showEditForm = false
                }, onCancel = { showEditForm = false })
            } else {
                ProfileView(modifier = modifier, user = user, onEdit = { showEditForm = true }, onLogout = { viewModel.logout() })
            }
        }
        else -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No user logged in", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun ProfileView(modifier: Modifier = Modifier, user: User, onEdit: () -> Unit, onLogout: () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
            .windowInsetsPadding(WindowInsets.systemBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            content = {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Photo
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (user.photoUrl != null) {
                            AsyncImage(
                                model = user.photoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = user.name.firstOrNull()?.toString() ?: "?",
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = user.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = user.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Basic Info Section
        ProfileSection(title = "Basic Information") {
            ProfileField(label = "Age", value = user.age?.toString())
            ProfileField(label = "Gender", value = user.gender)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Personal Details Section
        ProfileSection(title = "Personal Details") {
            ProfileField(label = "Gotr", value = user.gotr)
            ProfileField(label = "Caste", value = user.caste)
            ProfileField(label = "Category", value = user.category)
            ProfileField(label = "Religion", value = user.religion)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Location Section
        ProfileSection(title = "Location") {
            ProfileField(label = "City/Town", value = user.cityTown)
            ProfileField(label = "District", value = user.district)
            ProfileField(label = "State", value = user.state)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bio Section
        ProfileSection(title = "About Me") {
            Text(
                text = user.bio ?: "No bio available",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onEdit,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile")
            }
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logout")
            }
        }
    }
}

@Composable
fun ProfileSection(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                content()
            }
        }
    )
}

@Composable
fun ProfileField(label: String, value: String?) {
    if (value != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$label:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun EditProfileForm(modifier: Modifier = Modifier, user: User, onSave: (User) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(user.name) }
    var email by remember { mutableStateOf(user.email) }
    var age by remember { mutableStateOf(user.age?.toString() ?: "") }
    var gender by remember { mutableStateOf(user.gender ?: "") }
    var gotr by remember { mutableStateOf(user.gotr ?: "") }
    var caste by remember { mutableStateOf(user.caste ?: "") }
    var category by remember { mutableStateOf(user.category ?: "") }
    var religion by remember { mutableStateOf(user.religion ?: "") }
    var cityTown by remember { mutableStateOf(user.cityTown ?: "") }
    var district by remember { mutableStateOf(user.district ?: "") }
    var state by remember { mutableStateOf(user.state ?: "") }
    var bio by remember { mutableStateOf(user.bio ?: "") }
    var photoUrl by remember { mutableStateOf(user.photoUrl ?: "") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = age,
            onValueChange = { age = it },
            label = { Text("Age") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = gender,
            onValueChange = { gender = it },
            label = { Text("Gender") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = gotr,
            onValueChange = { gotr = it },
            label = { Text("Gotr") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = caste,
            onValueChange = { caste = it },
            label = { Text("Caste") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Category") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = religion,
            onValueChange = { religion = it },
            label = { Text("Religion") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = cityTown,
            onValueChange = { cityTown = it },
            label = { Text("City/Town") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = district,
            onValueChange = { district = it },
            label = { Text("District") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = state,
            onValueChange = { state = it },
            label = { Text("State") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        OutlinedTextField(
            value = photoUrl,
            onValueChange = { photoUrl = it },
            label = { Text("Photo URL") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val updatedUser = user.copy(
                        name = name,
                        email = email,
                        age = age.toIntOrNull(),
                        gender = gender.takeIf { it.isNotBlank() },
                        gotr = gotr.takeIf { it.isNotBlank() },
                        caste = caste.takeIf { it.isNotBlank() },
                        category = category.takeIf { it.isNotBlank() },
                        religion = religion.takeIf { it.isNotBlank() },
                        cityTown = cityTown.takeIf { it.isNotBlank() },
                        district = district.takeIf { it.isNotBlank() },
                        state = state.takeIf { it.isNotBlank() },
                        bio = bio.takeIf { it.isNotBlank() },
                        photoUrl = photoUrl.takeIf { it.isNotBlank() }
                    )
                    onSave(updatedUser)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Save")
            }
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
        }
    }
}
