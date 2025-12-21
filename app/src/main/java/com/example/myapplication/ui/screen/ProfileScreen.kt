package com.example.myapplication.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    NONE, BASIC_INFO, PERSONAL_DETAILS, LOCATION, ABOUT_ME, HEADER
}

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel()
) {
    val loginState by viewModel.loginState.collectAsState()
    val subscription by viewModel.subscription.collectAsState()
    var currentEditSection by remember { mutableStateOf(EditSection.NONE) }
    var showEnlargedPhoto by remember { mutableStateOf(false) }

    when (loginState) {
        is LoginState.Success -> {
            val user = (loginState as LoginState.Success).user

            if (showEnlargedPhoto && user.photoUrl != null) {
                EnlargedPhotoDialog(
                    photoUrl = user.photoUrl,
                    onDismiss = { showEnlargedPhoto = false }
                )
            }

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
                    onLogout = { viewModel.logout() },
                    onPhotoClick = { if (user.photoUrl != null) showEnlargedPhoto = true }
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
fun EnlargedPhotoDialog(photoUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Use full screen width
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.9f))
                .clickable { onDismiss() }, // Click anywhere to dismiss
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Enlarged Profile Photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f) // Square aspect ratio or adjust as needed
                    .padding(16.dp),
                contentScale = ContentScale.Fit
            )
            
            // Optional close button for better UX
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
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
    onLogout: () -> Unit,
    onPhotoClick: () -> Unit = {}
) {
    // State for tab selection
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Details", "Photos")
    val tabIcons = listOf(Icons.Filled.Info, Icons.Filled.GridOn)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Header (Fixed at top)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            content = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile Photo
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onPhotoClick() }, // Make photo clickable
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
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        
                        Column(
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = user.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = { onEditSection(EditSection.HEADER) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Name and Photo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )

        // Tab Row (Instagram Style)
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    icon = { Icon(tabIcons[index], contentDescription = title) }
                )
            }
        }

        // Content below tabs (Scrollable)
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            when (selectedTabIndex) {
                0 -> {
                    // --- All Details in Tab 1 ---
                    
                    // Bio Section
                    val isBioMissing = user.bio.isNullOrBlank()
                    ProfileSection(
                        title = "About Me",
                        onEdit = { onEditSection(EditSection.ABOUT_ME) },
                        hasWarning = isBioMissing
                    ) {
                        Text(
                            text = user.bio ?: "No bio available",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Basic Info Section
                    val isBasicInfoMissing = user.age == null || user.gender.isNullOrBlank()
                    ProfileSection(
                        title = "Basic Information",
                        onEdit = { onEditSection(EditSection.BASIC_INFO) },
                        hasWarning = isBasicInfoMissing
                    ) {
                        ProfileField(label = "Age", value = user.age?.toString())
                        ProfileField(label = "Gender", value = user.gender)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Personal Details Section
                    val isPersonalDetailsMissing = user.gotr.isNullOrBlank() || user.caste.isNullOrBlank() || user.category.isNullOrBlank() || user.religion.isNullOrBlank()
                    ProfileSection(
                        title = "Personal Details",
                        onEdit = { onEditSection(EditSection.PERSONAL_DETAILS) },
                        hasWarning = isPersonalDetailsMissing
                    ) {
                        ProfileField(label = "Gotr", value = user.gotr)
                        ProfileField(label = "Caste", value = user.caste)
                        ProfileField(label = "Category", value = user.category)
                        ProfileField(label = "Religion", value = user.religion)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Location Section
                    val isLocationMissing = user.cityTown.isNullOrBlank() || user.district.isNullOrBlank() || user.state.isNullOrBlank()
                    ProfileSection(
                        title = "Location",
                        onEdit = { onEditSection(EditSection.LOCATION) },
                        hasWarning = isLocationMissing
                    ) {
                        ProfileField(label = "City/Town", value = user.cityTown)
                        ProfileField(label = "District", value = user.district)
                        ProfileField(label = "State", value = user.state)
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
                1 -> {
                   // Photos Tab
                   Column(modifier = Modifier.fillMaxSize()) {
                       // Add Photo Button
                       Button(
                           onClick = { /* Add Photo Logic */ },
                           modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                       ) {
                           Icon(Icons.Filled.Add, contentDescription = "Add Photo")
                           Spacer(modifier = Modifier.width(8.dp))
                           Text("Add Photo")
                       }

                       // Photos Grid (Placeholder for now as user only has one photoUrl)
                       if (user.photoUrl != null) {
                           Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                               Card(
                                   modifier = Modifier.weight(1f).aspectRatio(1f),
                                   shape = RoundedCornerShape(8.dp),
                                   elevation = CardDefaults.cardElevation(2.dp),
                                   onClick = { onPhotoClick() } // Enlarge photo on click from grid as well
                               ) {
                                   AsyncImage(
                                       model = user.photoUrl,
                                       contentDescription = "Photo",
                                       modifier = Modifier.fillMaxSize(),
                                       contentScale = ContentScale.Crop
                                   )
                               }
                               // Placeholder for second item in row if needed
                               Spacer(modifier = Modifier.weight(1f)) 
                           }
                       } else {
                           Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                               Text("No Photos Yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                           }
                       }
                   }
                }
            }
        }
    }
}

@Composable
fun ProfileSection(
    title: String, 
    onEdit: (() -> Unit)? = null, 
    hasWarning: Boolean = false,
    content: @Composable () -> Unit
) {
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
                        Box {
                            IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit $title",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (hasWarning) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Missing Information",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                )
                            }
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
    var name by remember { mutableStateOf(user.name) }
    var photoUrl by remember { mutableStateOf(user.photoUrl ?: "") }

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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Edit ${section.name.replace("_", " ").lowercase().capitalize(java.util.Locale.ROOT)}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        when (section) {
            EditSection.HEADER -> {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = photoUrl,
                    onValueChange = { photoUrl = it },
                    label = { Text("Photo URL") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
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
                        EditSection.HEADER -> user.copy(
                            name = name.takeIf { it.isNotBlank() } ?: user.name,
                            photoUrl = photoUrl.takeIf { it.isNotBlank() }
                        )
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
