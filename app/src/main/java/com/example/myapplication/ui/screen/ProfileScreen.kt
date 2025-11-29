package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
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
import java.time.format.DateTimeFormatter

enum class EditSection {
    NONE, BASIC_INFO, PERSONAL_DETAILS, LOCATION, ABOUT_ME
}

@Composable
fun ProfileScreen(modifier: Modifier = Modifier, viewModel: LoginViewModel = viewModel()) {
    val loginState by viewModel.loginState.collectAsState()
    val subscription by viewModel.subscription.collectAsState()
    var currentEditSection by remember { mutableStateOf(EditSection.NONE) }

    when (loginState) {
        is LoginState.Success -> {
            val user = (loginState as LoginState.Success).user

            if (currentEditSection != EditSection.NONE) {
                SectionEditForm(
                    modifier = modifier,
                    user = user,
                    section = currentEditSection,
                    onSave = { updatedUser ->
                        viewModel.updateUser(updatedUser)
                        currentEditSection = EditSection.NONE
                    },
                    onCancel = { currentEditSection = EditSection.NONE }
                )
            } else {
                ProfileView(
                    modifier = modifier,
                    user = user,
                    subscription = subscription,
                    onEditSection = { section -> currentEditSection = section },
                    onLogout = { viewModel.logout() }
                )
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
fun ProfileView(
    modifier: Modifier = Modifier,
    user: User,
    subscription: com.example.myapplication.data.model.Subscription? = null,
    onEditSection: (EditSection) -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 16.dp) // Removed top padding
            .windowInsetsPadding(WindowInsets.systemBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            content = {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 16.dp),
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

        // Bio Section
        ProfileSection(
            title = "About Me",
            onEdit = { onEditSection(EditSection.ABOUT_ME) }
        ) {
            Text(
                text = user.bio ?: "No bio available",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Basic Info Section
        ProfileSection(
            title = "Basic Information",
            onEdit = { onEditSection(EditSection.BASIC_INFO) }
        ) {
            ProfileField(label = "Age", value = user.age?.toString())
            ProfileField(label = "Gender", value = user.gender)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Personal Details Section
        ProfileSection(
            title = "Personal Details",
            onEdit = { onEditSection(EditSection.PERSONAL_DETAILS) }
        ) {
            ProfileField(label = "Gotr", value = user.gotr)
            ProfileField(label = "Caste", value = user.caste)
            ProfileField(label = "Category", value = user.category)
            ProfileField(label = "Religion", value = user.religion)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Location Section
        ProfileSection(
            title = "Location",
            onEdit = { onEditSection(EditSection.LOCATION) }
        ) {
            ProfileField(label = "City/Town", value = user.cityTown)
            ProfileField(label = "District", value = user.district)
            ProfileField(label = "State", value = user.state)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Subscription Section
        if (subscription != null) {
            ProfileSection(title = "Subscription Details") {
                val formattedExpiry = try {
                    val parsedDate = try {
                        // Try parsing as LocalDateTime (ISO format) first
                        java.time.LocalDateTime.parse(subscription.expiryDate).toLocalDate()
                    } catch (e: Exception) {
                        // Fallback to LocalDate
                        java.time.LocalDate.parse(subscription.expiryDate)
                    }
                    DateTimeFormatter.ofPattern("dd MMM yyyy").format(parsedDate)
                } catch (e: Exception) {
                    subscription.expiryDate
                }
                ProfileField(label = "Plan Name", value = subscription.planName)
                ProfileField(label = "Plan Duration (Months)", value = subscription.planDurationMonths.toString())
                ProfileField(label = "Expiry Date", value = formattedExpiry)
                ProfileField(label = "Chat Limit", value = subscription.chatLimit.toString())
                ProfileField(label = "Plan Chat Limit", value = subscription.planChatLimit.toString())
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
fun ProfileSection(title: String, onEdit: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (onEdit != null) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit $title",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
        }
    )
}

@Composable
fun ProfileField(label: String, value: String?) {
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
            text = if (value.isNullOrBlank()) "-" else value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SectionEditForm(
    modifier: Modifier = Modifier,
    user: User,
    section: EditSection,
    onSave: (User) -> Unit,
    onCancel: () -> Unit
) {
    // Local state for all fields, we'll only show relevant ones based on section
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(16.dp)
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Edit ${section.name.replace("_", " ").lowercase().capitalize(java.util.Locale.ROOT)}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when (section) {
            EditSection.BASIC_INFO -> {
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
            }
            EditSection.PERSONAL_DETAILS -> {
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
            }
            EditSection.LOCATION -> {
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
            }
            EditSection.ABOUT_ME -> {
                OutlinedTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    // Create updated user based on the section being edited
                    val updatedUser = when (section) {
                        EditSection.BASIC_INFO -> user.copy(
                            age = age.toIntOrNull(),
                            gender = gender.takeIf { it.isNotBlank() }
                        )
                        EditSection.PERSONAL_DETAILS -> user.copy(
                            gotr = gotr.takeIf { it.isNotBlank() },
                            caste = caste.takeIf { it.isNotBlank() },
                            category = category.takeIf { it.isNotBlank() },
                            religion = religion.takeIf { it.isNotBlank() }
                        )
                        EditSection.LOCATION -> user.copy(
                            cityTown = cityTown.takeIf { it.isNotBlank() },
                            district = district.takeIf { it.isNotBlank() },
                            state = state.takeIf { it.isNotBlank() }
                        )
                        EditSection.ABOUT_ME -> user.copy(
                            bio = bio.takeIf { it.isNotBlank() }
                        )
                        else -> user
                    }
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
