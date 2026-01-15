package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.viewmodel.LoginState
import com.example.myapplication.ui.viewmodel.LoginViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationOn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(modifier: Modifier = Modifier, onBackToLogin: (String) -> Unit, onRegisterSuccess: (User) -> Unit, viewModel: LoginViewModel = viewModel(), googleUserInfo: com.example.myapplication.ui.viewmodel.GoogleUserInfo? = null, existingUser: User? = null) {
    // State to manage the current step
    var currentStep by remember { mutableStateOf(0) }
    val steps = listOf("Account", "Personal", "Location")
    val stepIcons = listOf(Icons.Filled.AccountCircle, Icons.Filled.Person, Icons.Filled.LocationOn)

    // Shared state for registration data
    // Step 1: Account
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }

    // Step 2: Personal Details
    var age by remember { mutableStateOf("") }
    var caste by remember { mutableStateOf("") }
    var gotr by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var religion by remember { mutableStateOf("") }

    // Step 3: Location
    var cityTown by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }

    val registerState by viewModel.registerState.collectAsState()
    var registeredUser by remember { mutableStateOf<User?>(null) }

    // Pre-fill data from Google user or existing user
    LaunchedEffect(googleUserInfo, existingUser) {
        if (existingUser != null) {
            // Existing user with incomplete profile - pre-fill all available data
            registeredUser = existingUser
            name = existingUser.name
            email = existingUser.email
            gender = existingUser.gender ?: ""
            age = existingUser.age?.toString() ?: ""
            caste = existingUser.caste ?: ""
            gotr = existingUser.gotr ?: ""
            category = existingUser.category ?: ""
            religion = existingUser.religion ?: ""
            cityTown = existingUser.cityTown ?: ""
            district = existingUser.district ?: ""
            state = existingUser.state ?: ""

            // Determine which step to start from based on missing data
            currentStep = when {
                existingUser.cityTown.isNullOrBlank() || existingUser.state.isNullOrBlank() -> 2 // Location missing
                existingUser.age == null || existingUser.religion.isNullOrBlank() -> 1 // Personal missing
                else -> 0 // All basic info present, but profile incomplete
            }
        } else if (googleUserInfo != null) {
            // New Google user
            name = googleUserInfo.name
            email = googleUserInfo.email
            mobileNumber = "" // Will be entered by user
        }
    }

    // Handle registration result
    LaunchedEffect(registerState) {
        if (registerState is LoginState.Success) {
            val user = (registerState as LoginState.Success).user
            // Ensure photos is not null
            val safeUser = user.copy(photos = user.photos ?: emptyList())
            registeredUser = safeUser
            // Move to next step only if we are at step 0 (Register)
            if (currentStep == 0) {
                currentStep = 1
            }
        }
    }

    Scaffold(
        topBar = {
            TabRow(selectedTabIndex = currentStep) {
                steps.forEachIndexed { index, title ->
                    Tab(
                        selected = currentStep == index,
                        onClick = { 
                            // Allow clicking only previous steps or if current step is valid (optional, but sticking to strict flow for now)
                            // For now, disable manual navigation to forward steps
                        },
                        enabled = false, // Disable direct tab clicking to enforce flow
                        text = { Text(title) },
                        icon = { Icon(stepIcons[index], contentDescription = title) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (currentStep) {
                0 -> {
                    // Step 1: Account Registration
                    Text("Create Account", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { if (googleUserInfo == null) email = it },
                        label = { Text("Email") },
                        readOnly = googleUserInfo != null,
                        enabled = googleUserInfo == null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+91",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        OutlinedTextField(
                            value = mobileNumber,
                            onValueChange = { mobileNumber = it },
                            label = { Text("Mobile Number") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    var expanded by remember { mutableStateOf(false) }
                    val genders = listOf("Male", "Female")
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(

                            value = gender,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gender") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            genders.forEach { selectionOption ->
                                DropdownMenuItem(
                                    text = { Text(selectionOption) },
                                    onClick = {
                                        gender = selectionOption
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Note: Mobile number and email will not be visible to others",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Red,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (googleUserInfo != null) {
                                // For Google users, save Firebase UID immediately after Step 1
                                viewModel.registerWithGoogleAndSaveUID(googleUserInfo, name, email, mobileNumber, gender) { user ->
                                    // User is now registered with Firebase UID saved
                                    registeredUser = user
                                    currentStep = 1
                                }
                            } else {
                                viewModel.register(name, email, mobileNumber, gender)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = name.isNotBlank() && email.isNotBlank() && mobileNumber.isNotBlank() && gender.isNotBlank() && registerState !is LoginState.Loading
                    ) {
                        if (registerState is LoginState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Next")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { onBackToLogin("") }) {
                        Text("Already have an account? Login")
                    }
                    
                    if (registerState is LoginState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text((registerState as LoginState.Error).message, color = MaterialTheme.colorScheme.error)
                    }
                }
                1 -> {
                    // Step 2: Personal Details
                    Text("Personal Details", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = caste, onValueChange = { caste = it }, label = { Text("Caste") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = gotr, onValueChange = { gotr = it }, label = { Text("Gotra") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = religion, onValueChange = { religion = it }, label = { Text("Religion") }, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            registeredUser?.let { user ->
                                val updatedUser = user.copy(
                                    age = age.toIntOrNull(),
                                    caste = caste.takeIf { it.isNotBlank() },
                                    gotr = gotr.takeIf { it.isNotBlank() },
                                    category = category.takeIf { it.isNotBlank() },
                                    religion = religion.takeIf { it.isNotBlank() },
                                    photos = user.photos // Preserve existing photos
                                )
                                viewModel.updateUserDuringRegistration(updatedUser) { savedUser ->
                                    registeredUser = savedUser
                                    currentStep = 2 // Go to Location tab
                                    // Debug: Print current step
                                    println("DEBUG: Moving to Location tab (Step 2)")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = age.isNotBlank() && religion.isNotBlank() // Require both age and religion
                    ) {
                        Text("Next")
                    }
                }
                2 -> {
                    // Step 3: Location Details
                    Text("Location Details", style = MaterialTheme.typography.headlineMedium)
                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(value = cityTown, onValueChange = { cityTown = it }, label = { Text("City/Town") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = district, onValueChange = { district = it }, label = { Text("District") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text("State") }, modifier = Modifier.fillMaxWidth())

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            println("DEBUG: Complete Registration button clicked")
                            registeredUser?.let { user ->
                                val updatedUser = user.copy(
                                    cityTown = cityTown.takeIf { it.isNotBlank() },
                                    district = district.takeIf { it.isNotBlank() },
                                    state = state.takeIf { it.isNotBlank() },
                                    photos = user.photos // Preserve existing photos
                                )
                                viewModel.updateUser(updatedUser)
                                onRegisterSuccess(updatedUser) // Final completion
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = cityTown.isNotBlank() && state.isNotBlank()
                    ) {
                        Text("Complete Registration")
                    }
                }
            }
        }
    }
}
