package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.api.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.screen.FavouritesScreen
import com.example.myapplication.ui.screen.LocationDetailsScreen
import com.example.myapplication.ui.screen.LoginScreen
import com.example.myapplication.ui.screen.MessageDetailScreen
import com.example.myapplication.ui.screen.MessagesScreen
import com.example.myapplication.ui.screen.PersonalDetailsScreen
import com.example.myapplication.ui.screen.ProfileScreen
import com.example.myapplication.ui.screen.RegisterScreen
import com.example.myapplication.ui.screen.SearchScreen
import com.example.myapplication.ui.screen.PlansScreen
import com.example.myapplication.ui.screen.UserListScreen
import com.example.myapplication.ui.screen.UserProfileScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.viewmodel.LoginState
import com.example.myapplication.ui.viewmodel.LoginViewModel
import androidx.compose.material3.ExperimentalMaterial3Api

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(modifier: Modifier = Modifier, viewModel: LoginViewModel = viewModel()) {
    val loginState by viewModel.loginState.collectAsState()
    val selectedUser by viewModel.selectedUser.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    var showFavourites by remember { mutableStateOf(false) }
    var showMessages by remember { mutableStateOf(false) }
    var showRegister by remember { mutableStateOf(false) }
    var registrationSuccessMessage by remember { mutableStateOf<String?>(null) }
    var registeredUser by remember { mutableStateOf<User?>(null) }
    var showPersonalDetails by remember { mutableStateOf(false) }
    var showLocationDetails by remember { mutableStateOf(false) }
    var refreshMessages by remember { mutableStateOf(false) }
    var showPlans by remember { mutableStateOf(false) }
    var showChatDetail by remember { mutableStateOf<User?>(null) }

    // State to control bottom bar visibility
    var isChatDetailVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Handle registration flow screens
    if (showPersonalDetails && registeredUser != null) {
        BackHandler { showPersonalDetails = false }
        PersonalDetailsScreen(
            modifier = modifier,
            user = registeredUser!!,
            onNext = { updatedUser ->
                registeredUser = updatedUser
                showPersonalDetails = false
                showLocationDetails = true
            }
        )
        return
    }

    if (showLocationDetails && registeredUser != null) {
        BackHandler { showLocationDetails = false }
        LocationDetailsScreen(
            modifier = modifier,
            user = registeredUser!!,
            onComplete = { updatedUser ->
                registeredUser = updatedUser
                showLocationDetails = false
                // Auto-login after completing details
                // Since we don't have password stored, we need to modify login to accept user object or adjust
                // For now, set login state directly
                viewModel._loginState.value = LoginState.Success(updatedUser)
                viewModel.fetchAllUsers()
            }
        )
        return
    }

    when (loginState) {
        is LoginState.Success -> {
            val currentUser = (loginState as LoginState.Success).user
            val selectedUserValue = selectedUser
            if (showFavourites) {
                BackHandler { showFavourites = false }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = {
                                    selectedTab = 0
                                    showFavourites = false
                                    showMessages = false
                                    viewModel.selectUser(null)
                                },
                                icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = {
                                    selectedTab = 1
                                    showFavourites = false
                                    showMessages = false
                                    viewModel.selectUser(null)
                                },
                                icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                                label = { Text("Search") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = {
                                    selectedTab = 2
                                    showFavourites = false
                                    showMessages = false
                                    viewModel.selectUser(null)
                                    refreshMessages = !refreshMessages
                                },
                                icon = { Icon(Icons.Filled.Email, contentDescription = "Messages") },
                                label = { Text("Messages") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = {
                                    selectedTab = 3
                                    showFavourites = false
                                    showMessages = false
                                    viewModel.selectUser(null)
                                },
                                icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                                label = { Text("Profile") }
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = {
                                    scope.launch {
                                        try {
                                            val response = RetrofitClient.apiService.getAllPlans()
                                            if (response.isSuccessful) {
                                                // Handle successful response, e.g., log or show plans
                                                println("Plans: ${response.body()}")
                                            } else {
                                                println("Error: ${response.errorBody()?.string()}")
                                            }
                                        } catch (e: Exception) {
                                            println("Exception: ${e.message}")
                                        }
                                    }
                                },
                                icon = { Icon(Icons.Filled.Star, contentDescription = "Premium Membership") },
                                label = { Text("Premium") }
                            )
                        }
                    }
                ) { padding ->
                    FavouritesScreen(modifier = Modifier.padding(padding), onBack = { showFavourites = false })
                }
            } else if (showMessages) {
                BackHandler { showMessages = false }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = {
                                    selectedTab = 0
                                    showFavourites = false
                                    showMessages = false
                                    viewModel.selectUser(null)
                                },
                                icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = {
                                    selectedTab = 1
                                    showFavourites = false
                                    showMessages = false
                                    viewModel.selectUser(null)
                                },
                                icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                                label = { Text("Search") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = {
                                    selectedTab = 2
                                    showFavourites = false
                                    showMessages = false
                                    viewModel.selectUser(null)
                                    refreshMessages = !refreshMessages
                                },
                                icon = { Icon(Icons.Filled.Email, contentDescription = "Messages") },
                                label = { Text("Messages") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = {
                                    selectedTab = 3
                                    showFavourites = false
                                    showMessages = false
                                    viewModel.selectUser(null)
                                },
                                icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                                label = { Text("Profile") }
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = { /* TODO: Implement premium membership */ },
                                icon = { Icon(Icons.Filled.Star, contentDescription = "Premium Membership") },
                                label = { Text("Premium") }
                            )
                        }
                    }
                ) { padding ->
                    MessagesScreen(
                        modifier = Modifier.padding(padding),
                        onBack = { showMessages = false },
                        viewModel = viewModel,
                        onChatStatusChanged = { isVisible -> isChatDetailVisible = isVisible }
                    )
                }
            } else if (showPlans) {
                BackHandler { showPlans = false }
                Scaffold(
                    modifier = modifier,
                    topBar = {
                        TopAppBar(
                            title = { Text("Shaadi App") },
                            actions = {
                                IconButton(onClick = {
                                    viewModel.fetchFavourites(currentUser.id)
                                    showFavourites = true
                                }) {
                                    Icon(Icons.Filled.Favorite, contentDescription = "Favourites")
                                }
                            }
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = {
                                    selectedTab = 0
                                    showFavourites = false
                                    showMessages = false
                                    showPlans = false
                                    viewModel.selectUser(null)
                                },
                                icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = {
                                    selectedTab = 1
                                    showFavourites = false
                                    showMessages = false
                                    showPlans = false
                                    viewModel.selectUser(null)
                                },
                                icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                                label = { Text("Search") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = {
                                    selectedTab = 2
                                    showFavourites = false
                                    showMessages = false
                                    showPlans = false
                                    viewModel.selectUser(null)
                                    refreshMessages = !refreshMessages
                                },
                                icon = { Icon(Icons.Filled.Email, contentDescription = "Messages") },
                                label = { Text("Messages") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = {
                                    selectedTab = 3
                                    showFavourites = false
                                    showMessages = false
                                    showPlans = false
                                    viewModel.selectUser(null)
                                },
                                icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                                label = { Text("Profile") }
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = { showPlans = true },
                                icon = { Icon(Icons.Filled.Star, contentDescription = "Premium Membership") },
                                label = { Text("Premium") }
                            )
                        }
                    }
                ) { padding ->
                    PlansScreen(modifier = Modifier.padding(padding), onBack = { showPlans = false })
                }
            } else if (showChatDetail != null) {
                BackHandler { showChatDetail = null }
                MessageDetailScreen(
                    modifier = modifier,
                    receiver = showChatDetail!!,
                    onBack = { showChatDetail = null },
                    viewModel = viewModel
                )
            } else if (selectedUserValue != null) {
                BackHandler { viewModel.selectUser(null) }
                UserProfileScreen(modifier = modifier, user = selectedUserValue, onBack = { viewModel.selectUser(null) })
            } else {
                Scaffold(
                    modifier = modifier,
                    topBar = {
                        if (!isChatDetailVisible) {
                            TopAppBar(
                                title = { Text("Shaadi App") },
                                actions = {
                                    IconButton(onClick = {
                                        viewModel.fetchFavourites(currentUser.id)
                                        showFavourites = true
                                    }) {
                                        Icon(Icons.Filled.Favorite, contentDescription = "Favourites")
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        if (!isChatDetailVisible) {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = {
                                        selectedTab = 0
                                        showFavourites = false
                                        showMessages = false
                                        viewModel.selectUser(null)
                                    },
                                    icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                                    label = { Text("Home") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = {
                                        selectedTab = 1
                                        showFavourites = false
                                        showMessages = false
                                        viewModel.selectUser(null)
                                    },
                                    icon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                                    label = { Text("Search") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 2,
                                    onClick = {
                                        selectedTab = 2
                                        showFavourites = false
                                        showMessages = false
                                        viewModel.selectUser(null)
                                        refreshMessages = !refreshMessages
                                    },
                                    icon = { Icon(Icons.Filled.Email, contentDescription = "Messages") },
                                    label = { Text("Messages") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 3,
                                    onClick = {
                                        selectedTab = 3
                                        showFavourites = false
                                        showMessages = false
                                        viewModel.selectUser(null)
                                    },
                                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                                    label = { Text("Profile") }
                                )
                                NavigationBarItem(
                                    selected = false,
                                    onClick = { showPlans = true },
                                    icon = { Icon(Icons.Filled.Star, contentDescription = "Premium Membership") },
                                    label = { Text("Premium") }
                                )
                            }
                        }
                    }
                ) { padding ->
                    when (selectedTab) {
                        0 -> UserListScreen(
                            modifier = Modifier.padding(padding),
                            viewModel = viewModel,
                            onChatClick = { user ->
                                showChatDetail = user
                            }
                        )
                        1 -> SearchScreen(modifier = Modifier.padding(padding))
                        2 -> MessagesScreen(
                            modifier = Modifier.padding(padding),
                            viewModel = viewModel,
                            refreshTrigger = refreshMessages,
                            onChatStatusChanged = { isVisible -> isChatDetailVisible = isVisible }
                        )
                        3 -> ProfileScreen(modifier = Modifier.padding(padding))
                    }
                }
            }
        }
        else -> {
            if (showRegister) {
                RegisterScreen(modifier = modifier, onBackToLogin = {
                    registrationSuccessMessage = it
                    showRegister = false
                }, onRegisterSuccess = { user ->
                    registeredUser = user
                    showRegister = false
                    showPersonalDetails = true
                })
            } else {
                LoginScreen(
                    modifier = modifier,
                    onRegisterClick = { showRegister = true },
                    registrationSuccessMessage = registrationSuccessMessage,
                    onMessageShown = { registrationSuccessMessage = null }
                )
            }
        }
    }
}
