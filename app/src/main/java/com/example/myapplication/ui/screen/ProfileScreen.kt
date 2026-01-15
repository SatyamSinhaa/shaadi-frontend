package com.example.myapplication.ui.screen

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.yalantis.ucrop.UCrop
import android.content.Intent
import androidx.activity.result.ActivityResult

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

// Helper functions for height formatting and parsing
fun formatHeightForDisplay(height: Int?): String {
    if (height == null || height <= 0) return ""
    val feet = height / 100
    val inches = height % 100
    return "$feet'$inches\""
}

fun parseHeightFromFeetInches(feet: Int, inches: Int): Int {
    return feet * 100 + inches
}

fun parseFeetFromHeight(height: Int?): Int? {
    return height?.div(100)
}

fun parseInchesFromHeight(height: Int?): Int? {
    return height?.rem(100)
}

// Legacy helper function for backward compatibility
fun formatHeight(raw: String): String {
    if (raw.isEmpty()) return ""
    val digits = raw.filter { it.isDigit() }
    return when (digits.length) {
        0 -> ""
        1 -> "${digits}\""
        2 -> "${digits[0]}'${digits[1]}\""
        else -> {
            val feet = digits.dropLast(2)
            val inches = digits.takeLast(2)
            "$feet'$inches\""
        }
    }
}

fun formatIncomeForDisplay(income: Long?): String {
    if (income == null || income <= 0) return ""
    val incomeStr = income.toString()
    val digits = incomeStr.reversed()

    // Indian numbering system: first group of 3 digits from right, then groups of 2
    val groups = mutableListOf<String>()
    var remainingDigits = digits

    // First group: 3 digits from right
    if (remainingDigits.length >= 3) {
        groups.add(remainingDigits.take(3).reversed())
        remainingDigits = remainingDigits.drop(3)
    } else {
        groups.add(remainingDigits.reversed())
        remainingDigits = ""
    }

    // Remaining groups: 2 digits each
    while (remainingDigits.isNotEmpty()) {
        val groupSize = minOf(2, remainingDigits.length)
        groups.add(remainingDigits.take(groupSize).reversed())
        remainingDigits = remainingDigits.drop(groupSize)
    }

    return groups.reversed().joinToString(",")
}

fun parseHeightToCm(formatted: String): Int? {
    // Extract feet and inches from formatted string like "5'10\""
    val match = Regex("(\\d+)'(\\d+)\"").find(formatted)
    return match?.let {
        val feet = it.groupValues[1].toInt()
        val inches = it.groupValues[2].toInt()
        (feet * 30.48 + inches * 2.54).toInt() // Convert to cm
    }
}

enum class EditSection {
    NONE, BASIC_INFO, PERSONAL_DETAILS, LOCATION, ABOUT_ME, HEADER, FAMILY_DETAILS, LIFESTYLE
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
    val context = LocalContext.current

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
                    onCancel = { currentEditSection = EditSection.NONE },
                    viewModel = viewModel
                )
            } else {
                ProfileView(
                    modifier = modifier,
                    user = user,
                    subscription = subscription,
                    onEditSection = { section -> currentEditSection = section },
                    onLogout = { viewModel.logout(context) },
                    onPhotoClick = { if (user.photoUrl != null) showEnlargedPhoto = true },
                    onAddGalleryPhoto = { uri, context -> viewModel.uploadGalleryPhoto(context, uri, user.id) }
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
    onPhotoClick: () -> Unit = {},
    onAddGalleryPhoto: (Uri, Context) -> Unit
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
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        IconButton(
                            onClick = { onEditSection(EditSection.HEADER) }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Name and Photo",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        // Added Warning Icon for Null Profile Photo
                        if (user.photoUrl.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Missing Photo",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(16.dp)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 6.dp, y = (-6).dp)
                                    .background(Color.White, CircleShape)
                            )
                        }
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
                    val isBasicInfoMissing = user.age == null
                    ProfileSection(
                        title = "Basic Information",
                        onEdit = { onEditSection(EditSection.BASIC_INFO) },
                        hasWarning = isBasicInfoMissing
                    ) {
                        ProfileField(label = "Age", value = user.age?.toString())
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Personal Details Section
                    val isPersonalDetailsMissing = user.profession.isNullOrBlank() || user.education.isNullOrBlank() || user.gotr.isNullOrBlank() || user.caste.isNullOrBlank() || user.category.isNullOrBlank() || user.religion.isNullOrBlank()
                    ProfileSection(
                        title = "Personal Details",
                        onEdit = { onEditSection(EditSection.PERSONAL_DETAILS) },
                        hasWarning = isPersonalDetailsMissing
                    ) {
                        ProfileField(label = "Marital Status", value = user.maritalStatus)
                        ProfileField(label = "Manglik", value = if (user.manglik == true) "Yes" else if (user.manglik == false) "No" else null)
                        ProfileField(label = "Date of Birth", value = user.dateOfBirth)
                        ProfileField(label = "Height", value = formatHeightForDisplay(user.height))
                        ProfileField(label = "Weight (kg)", value = user.weightKg?.toString())
                        ProfileField(label = "Rashi", value = user.rashi)
                        ProfileField(label = "Profession", value = user.profession)
                        ProfileField(label = "Education", value = user.education)
                        ProfileField(label = "Annual Income", value = formatIncomeForDisplay(user.annualIncome))
                        ProfileField(label = "Mother Tongue", value = user.motherTongue)
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
                        ProfileField(label = "Address", value = user.address)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Family Details Section
                    val isFamilyDetailsMissing = user.fatherName.isNullOrBlank() || user.motherName.isNullOrBlank()
                    ProfileSection(
                        title = "Family Details",
                        onEdit = { onEditSection(EditSection.FAMILY_DETAILS) },
                        hasWarning = isFamilyDetailsMissing
                    ) {
                        ProfileField(label = "Father Name", value = user.fatherName)
                        ProfileField(label = "Father Occupation", value = user.fatherOccupation)
                        ProfileField(label = "Mother Name", value = user.motherName)
                        ProfileField(label = "Mother Occupation", value = user.motherOccupation)
                        ProfileField(label = "Number of Brothers", value = user.numberOfBrothers?.toString())
                        ProfileField(label = "Number of Sisters", value = user.numberOfSisters?.toString())
                        ProfileField(label = "Family Type", value = user.familyType)
                        ProfileField(label = "Family Locations", value = user.familyLocations)
                        ProfileField(label = "Property", value = user.property)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // Lifestyle Section
                    val isLifestyleMissing = user.diet.isNullOrBlank()
                    ProfileSection(
                        title = "Lifestyle",
                        onEdit = { onEditSection(EditSection.LIFESTYLE) },
                        hasWarning = isLifestyleMissing
                    ) {
                        ProfileField(label = "Diet", value = user.diet)
                        ProfileField(label = "Smoking", value = if (user.smoking == true) "Yes" else if (user.smoking == false) "No" else null)
                        ProfileField(label = "Drinking", value = if (user.drinking == true) "Yes" else if (user.drinking == false) "No" else null)
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
                    val context = LocalContext.current
                    
                    // Gallery Launcher
                    val galleryLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.PickVisualMedia()
                    ) { uri: Uri? ->
                        uri?.let {
                            onAddGalleryPhoto(it, context)
                        }
                    }

                    // Gallery Launcher

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Add Photo Button
                        Button(
                            onClick = { 
                                // galleryLauncher.launch(
                                //     PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                // )
                            },
                            enabled = false, // Disable the button
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Photo")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Photo")
                        }
                        Text("Coming Soon", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

                        // Photos Grid
                        if (user.photos.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            // Simple Grid Implementation
                            val photos = user.photos
                            val chunks = photos.chunked(3) // 3 columns
                            
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                chunks.forEach { rowPhotos ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        rowPhotos.forEach { photo ->
                                            var showDialog by remember { mutableStateOf(false) }
                                            
                                            // Photo Item
                                            Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                                 AsyncImage(
                                                    model = photo.url,
                                                    contentDescription = "Gallery Photo",
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .clickable { showDialog = true },
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                            
                                            if (showDialog) {
                                                EnlargedPhotoDialog(
                                                    photoUrl = photo.url,
                                                    onDismiss = { showDialog = false }
                                                )
                                            }
                                        }
                                        // Fill empty spaces if last row has fewer items
                                        repeat(3 - rowPhotos.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
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
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Missing Information",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-6).dp)
                                        .background(Color.White, CircleShape)
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
    onCancel: () -> Unit,
    viewModel: LoginViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // Photo Picker Launcher
    // Crop Launcher
    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                viewModel.uploadImageAndSync(context, it, user.id)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(result.data!!)
            Log.e("SectionEditForm", "Crop error: $cropError")
        }
    }

    // Photo Picker Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { sourceUri ->
             // Create destination URI in cache
            val destinationFileName = "profile_crop_${System.currentTimeMillis()}.jpg"
            val destinationFile = java.io.File(context.cacheDir, destinationFileName)
            val destinationUri = android.net.Uri.fromFile(destinationFile)

            val uCrop = UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1f, 1f) // Profile pictures are usually square
                .withMaxResultSize(1080, 1080)
                .withOptions(UCrop.Options().apply {
                    setCompressionQuality(80)
                    setFreeStyleCropEnabled(true)
                    setCircleDimmedLayer(true) // Circular crop for profile photo
                    setShowCropGrid(false) // Cleaner look for profile
                })
            
            val intent = uCrop.getIntent(context)
            cropLauncher.launch(intent)
        }
    }

    // Local state for all fields, we'll only show relevant ones based on section
    var name by remember { mutableStateOf(user.name) }

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

    var maritalStatus by remember { mutableStateOf(user.maritalStatus ?: "") }
    var manglik by remember { mutableStateOf(if (user.manglik == true) "Yes" else "No") }
    var dateOfBirth by remember { mutableStateOf(user.dateOfBirth ?: "") }
    var heightFeet by remember { mutableStateOf(parseFeetFromHeight(user.height)?.toString() ?: "") }
    var heightInches by remember { mutableStateOf(parseInchesFromHeight(user.height)?.toString() ?: "") }
    var weightKg by remember { mutableStateOf(user.weightKg?.toString() ?: "") }
    var rawIncome by remember { mutableStateOf(user.annualIncome?.toString() ?: "") }
    var rashi by remember { mutableStateOf(user.rashi ?: "") }
    var profession by remember { mutableStateOf(user.profession ?: "") }
    var education by remember { mutableStateOf(user.education ?: "") }
    var motherTongue by remember { mutableStateOf(user.motherTongue ?: "") }
    var address by remember { mutableStateOf(user.address ?: "") }
    var fatherName by remember { mutableStateOf(user.fatherName ?: "") }
    var fatherOccupation by remember { mutableStateOf(user.fatherOccupation ?: "") }
    var motherName by remember { mutableStateOf(user.motherName ?: "") }
    var motherOccupation by remember { mutableStateOf(user.motherOccupation ?: "") }
    var numberOfBrothers by remember { mutableStateOf(user.numberOfBrothers?.toString() ?: "") }
    var numberOfSisters by remember { mutableStateOf(user.numberOfSisters?.toString() ?: "") }
    var familyType by remember { mutableStateOf(user.familyType ?: "") }
    var familyLocations by remember { mutableStateOf(user.familyLocations ?: "") }
    var property by remember { mutableStateOf(user.property ?: "") }
    var diet by remember { mutableStateOf(user.diet ?: "") }
    var smoking by remember { mutableStateOf(if (user.smoking == true) "Yes" else "No") }
    var drinking by remember { mutableStateOf(if (user.drinking == true) "Yes" else "No") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Edit ${section.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}",
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
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Photo Section with Picker
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Profile Photo", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Current Photo Preview
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (user.photoUrl != null) {
                                AsyncImage(
                                    model = user.photoUrl,
                                    contentDescription = "Current Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select from Gallery")
                        }
                    }
                }
            }
            EditSection.BASIC_INFO -> {
                OutlinedTextField(
                    value = age,
                    onValueChange = { age = it },
                    label = { Text("Age") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            EditSection.PERSONAL_DETAILS -> {
                OutlinedTextField(
                    value = maritalStatus,
                    onValueChange = { maritalStatus = it },
                    label = { Text("Marital Status") },
                    modifier = Modifier.fillMaxWidth()
                )
                // Manglik Radio Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Manglik", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.width(80.dp))
                    RadioButton(
                        selected = manglik == "Yes",
                        onClick = { manglik = "Yes" }
                    )
                    Text("Yes", modifier = Modifier.padding(start = 8.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = manglik == "No",
                        onClick = { manglik = "No" }
                    )
                    Text("No", modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedTextField(
                    value = dateOfBirth,
                    onValueChange = { dateOfBirth = it },
                    label = { Text("Date of Birth") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("yyyy-mm-dd") }
                )
                // Height fields: feet and inches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = heightFeet,
                        onValueChange = { heightFeet = it.filter { char -> char.isDigit() } },
                        label = { Text("Feet") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("5") }
                    )
                    OutlinedTextField(
                        value = heightInches,
                        onValueChange = { heightInches = it.filter { char -> char.isDigit() } },
                        label = { Text("Inches") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("10") }
                    )
                }
                OutlinedTextField(
                    value = weightKg,
                    onValueChange = { weightKg = it.filter { it.isDigit() } },
                    label = { Text("Weight (kg)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = rashi,
                    onValueChange = { rashi = it },
                    label = { Text("Rashi") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = profession,
                    onValueChange = { profession = it },
                    label = { Text("Profession") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = education,
                    onValueChange = { education = it },
                    label = { Text("Education") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rawIncome,
                    onValueChange = { rawIncome = it.filter { char -> char.isDigit() } },
                    label = { Text("Annual Income") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = { Text("e.g. 500000") }
                )
                OutlinedTextField(
                    value = motherTongue,
                    onValueChange = { motherTongue = it },
                    label = { Text("Mother Tongue") },
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
            EditSection.FAMILY_DETAILS -> {
                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text("Father Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fatherOccupation,
                    onValueChange = { fatherOccupation = it },
                    label = { Text("Father Occupation") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = motherName,
                    onValueChange = { motherName = it },
                    label = { Text("Mother Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = motherOccupation,
                    onValueChange = { motherOccupation = it },
                    label = { Text("Mother Occupation") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = numberOfBrothers,
                    onValueChange = { numberOfBrothers = it.filter { char -> char.isDigit() } },
                    label = { Text("Number of Brothers") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = numberOfSisters,
                    onValueChange = { numberOfSisters = it.filter { char -> char.isDigit() } },
                    label = { Text("Number of Sisters") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = familyType,
                    onValueChange = { familyType = it },
                    label = { Text("Family Type") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = familyLocations,
                    onValueChange = { familyLocations = it },
                    label = { Text("Family Locations") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = property,
                    onValueChange = { property = it },
                    label = { Text("Property") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            EditSection.LIFESTYLE -> {
                OutlinedTextField(
                    value = diet,
                    onValueChange = { diet = it },
                    label = { Text("Diet") },
                    modifier = Modifier.fillMaxWidth()
                )
                // Smoking Radio Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Smoking", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.width(80.dp))
                    RadioButton(
                        selected = smoking == "Yes",
                        onClick = { smoking = "Yes" }
                    )
                    Text("Yes", modifier = Modifier.padding(start = 8.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = smoking == "No",
                        onClick = { smoking = "No" }
                    )
                    Text("No", modifier = Modifier.padding(start = 8.dp))
                }
                // Drinking Radio Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Drinking", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.width(80.dp))
                    RadioButton(
                        selected = drinking == "Yes",
                        onClick = { drinking = "Yes" }
                    )
                    Text("Yes", modifier = Modifier.padding(start = 8.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(
                        selected = drinking == "No",
                        onClick = { drinking = "No" }
                    )
                    Text("No", modifier = Modifier.padding(start = 8.dp))
                }
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
                            name = name.takeIf { it.isNotBlank() } ?: user.name
                        )
                        EditSection.BASIC_INFO -> user.copy(
                            age = age.toIntOrNull(),
                            gender = gender.takeIf { it.isNotBlank() }
                        )
                        EditSection.PERSONAL_DETAILS -> {
                            val feet = heightFeet.toIntOrNull() ?: 0
                            val inches = heightInches.toIntOrNull() ?: 0
                            val height = if (feet > 0 || inches > 0) parseHeightFromFeetInches(feet, inches) else user.height
                            user.copy(
                                maritalStatus = maritalStatus.takeIf { it.isNotBlank() },
                                manglik = if (manglik.equals("Yes", ignoreCase = true)) true else if (manglik.equals("No", ignoreCase = true)) false else null,
                                dateOfBirth = dateOfBirth.takeIf { it.isNotBlank() },
                                height = height,
                                weightKg = weightKg.toIntOrNull(),
                                rashi = rashi.takeIf { it.isNotBlank() },
                                profession = profession.takeIf { it.isNotBlank() },
                                education = education.takeIf { it.isNotBlank() },
                                annualIncome = rawIncome.toLongOrNull(),
                                motherTongue = motherTongue.takeIf { it.isNotBlank() },
                                gotr = gotr.takeIf { it.isNotBlank() },
                                caste = caste.takeIf { it.isNotBlank() },
                                category = category.takeIf { it.isNotBlank() },
                                religion = religion.takeIf { it.isNotBlank() }
                            )
                        }
                        EditSection.LOCATION -> user.copy(
                            cityTown = cityTown.takeIf { it.isNotBlank() },
                            district = district.takeIf { it.isNotBlank() },
                            state = state.takeIf { it.isNotBlank() }
                        )
                        EditSection.FAMILY_DETAILS -> user.copy(
                            fatherName = fatherName.takeIf { it.isNotBlank() },
                            fatherOccupation = fatherOccupation.takeIf { it.isNotBlank() },
                            motherName = motherName.takeIf { it.isNotBlank() },
                            motherOccupation = motherOccupation.takeIf { it.isNotBlank() },
                            numberOfBrothers = numberOfBrothers.toIntOrNull(),
                            numberOfSisters = numberOfSisters.toIntOrNull(),
                            familyType = familyType.takeIf { it.isNotBlank() },
                            familyLocations = familyLocations.takeIf { it.isNotBlank() },
                            property = property.takeIf { it.isNotBlank() }
                        )
                        EditSection.LIFESTYLE -> user.copy(
                            diet = diet.takeIf { it.isNotBlank() },
                            smoking = if (smoking.equals("Yes", ignoreCase = true)) true else if (smoking.equals("No", ignoreCase = true)) false else null,
                            drinking = if (drinking.equals("Yes", ignoreCase = true)) true else if (drinking.equals("No", ignoreCase = true)) false else null
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
