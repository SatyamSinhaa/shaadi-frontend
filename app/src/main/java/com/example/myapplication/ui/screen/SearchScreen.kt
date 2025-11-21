package com.example.myapplication.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.viewmodel.SearchViewModel
import com.example.myapplication.ui.viewmodel.LoginViewModel
import com.example.myapplication.ui.viewmodel.LoginState

enum class FilterSection {
    AGE, LOCATION, RELIGION_CASTE
}

@Composable
fun FilterSectionButton(text: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    ) {
        Text(text)
    }
}

@Composable
fun SearchScreen(modifier: Modifier = Modifier, viewModel: SearchViewModel = viewModel(), loginViewModel: LoginViewModel = viewModel()) {
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val loginState by loginViewModel.loginState.collectAsState()

    val currentUserId = (loginState as? LoginState.Success)?.user?.id
    val filteredSearchResults = searchResults.filter { it.id != currentUserId }

    var name by remember { mutableStateOf("") }
    var showFilterDrawer by remember { mutableStateOf(false) }
    var selectedFilterSection by remember { mutableStateOf<FilterSection?>(null) }

    // Filter states
    var minAge by remember { mutableStateOf("") }
    var maxAge by remember { mutableStateOf("") }
    var religion by remember { mutableStateOf("") }
    var gotr by remember { mutableStateOf("") }
    var caste by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var cityTown by remember { mutableStateOf("") }
    var district by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }

    val performSearch = {
        val minAgeInt = minAge.toIntOrNull()
        val maxAgeInt = maxAge.toIntOrNull()
        val currentUserGender = (loginState as? LoginState.Success)?.user?.gender
        val oppositeGender = when (currentUserGender) {
            "male" -> "Male"
            "female" -> "Female"
            else -> null
        }
        viewModel.searchUsers(
            minAge = minAgeInt,
            maxAge = maxAgeInt,
            name = name.takeIf { it.isNotBlank() },
            location = cityTown.takeIf { it.isNotBlank() },
            religion = religion.takeIf { it.isNotBlank() },
            gender = oppositeGender
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp), // Standard padding
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Search Users",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = { showFilterDrawer = true }) {
                    Icon(Icons.Filled.FilterList, contentDescription = "Filters")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Name or Surname") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = performSearch,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Search")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Results
            if (filteredSearchResults.isNotEmpty()) {
                Text(
                    text = "Search Results (${filteredSearchResults.size})",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(filteredSearchResults) { user ->
                        UserItem(user) { /* TODO: Handle click */ }
                    }
                }
            } else if (!isLoading && error == null) {
                Text("No results found. Try adjusting your filters.")
            }
        }

        // Right-side sliding filter drawer
        AnimatedVisibility(
            visible = showFilterDrawer,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.weight(1f).clickable { showFilterDrawer = false })
                    Card(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(300.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Filters",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                IconButton(onClick = { showFilterDrawer = false }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Close")
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))

                            // Filter Section Headings
                            Column(modifier = Modifier.fillMaxWidth()) {
                                HorizontalDivider()
                                Text(
                                    text = "Age",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selectedFilterSection == FilterSection.AGE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedFilterSection = if (selectedFilterSection == FilterSection.AGE) null else FilterSection.AGE }
                                        .padding(16.dp)
                                )
                                HorizontalDivider()
                                Text(
                                    text = "Location",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selectedFilterSection == FilterSection.LOCATION) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedFilterSection = if (selectedFilterSection == FilterSection.LOCATION) null else FilterSection.LOCATION }
                                        .padding(16.dp)
                                )
                                HorizontalDivider()
                                Text(
                                    text = "Caste & Religion",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (selectedFilterSection == FilterSection.RELIGION_CASTE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedFilterSection = if (selectedFilterSection == FilterSection.RELIGION_CASTE) null else FilterSection.RELIGION_CASTE }
                                        .padding(16.dp)
                                )
                                HorizontalDivider()
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Dynamic Filter Fields
                            when (selectedFilterSection) {
                                FilterSection.AGE -> {
                                    Text(
                                        text = "Age",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = minAge,
                                        onValueChange = { minAge = it },
                                        label = { Text("Min Age") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = maxAge,
                                        onValueChange = { maxAge = it },
                                        label = { Text("Max Age") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                FilterSection.LOCATION -> {
                                    Text(
                                        text = "Location",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = cityTown,
                                        onValueChange = { cityTown = it },
                                        label = { Text("City/Village") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = district,
                                        onValueChange = { district = it },
                                        label = { Text("District") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = state,
                                        onValueChange = { state = it },
                                        label = { Text("State") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                FilterSection.RELIGION_CASTE -> {
                                    Text(
                                        text = "Religion & Caste",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = religion,
                                        onValueChange = { religion = it },
                                        label = { Text("Religion") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = gotr,
                                        onValueChange = { gotr = it },
                                        label = { Text("Gotr") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = caste,
                                        onValueChange = { caste = it },
                                        label = { Text("Caste") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = category,
                                        onValueChange = { category = it },
                                        label = { Text("Category") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                null -> {
                                    Text(
                                        text = "Select a filter section to view options",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        showFilterDrawer = false
                                        performSearch()
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Apply")
                                }
                                OutlinedButton(
                                    onClick = {
                                        minAge = ""
                                        maxAge = ""
                                        religion = ""
                                        gotr = ""
                                        caste = ""
                                        category = ""
                                        cityTown = ""
                                        district = ""
                                        state = ""
                                        selectedFilterSection = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Clear")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
