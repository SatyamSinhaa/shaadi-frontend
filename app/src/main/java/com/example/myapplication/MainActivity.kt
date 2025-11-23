package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.widget.Toast
import coil.compose.AsyncImage
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
import com.example.myapplication.ui.viewmodel.PlansViewModel
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
fun AppNavigation(
    modifier: Modifier = Modifier,
    loginViewModel: LoginViewModel = viewModel(),
    plansViewModel: PlansViewModel = viewModel()
) {
    val loginState by loginViewModel.loginState.collectAsState()
    val selectedUser by loginViewModel.selectedUser.collectAsState<User?>()
    var selectedTab by remember { mutableIntStateOf(0) }
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
    var showUserProfile by remember { mutableStateOf<User?>(null) }

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
                loginViewModel.updateUser(updatedUser)
                loginViewModel.fetchAllUsers()
            }
        )
        return
    }

    when (loginState) {
        is LoginState.Success -> {
            val currentUser = (loginState as LoginState.Success).user
            val selectedUserValue = selectedUser

            val profileIcon: @Composable () -> Unit = {
                if (currentUser.photoUrl != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .then(
                                if (selectedTab == 3) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                else Modifier
                            )
                    ) {
                        AsyncImage(
                            model = currentUser.photoUrl,
                            contentDescription = "Profile",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    Icon(Icons.Filled.Person, contentDescription = "Profile")
                }
            }

            if (showChatDetail != null) {
                BackHandler { showChatDetail = null }
                MessageDetailScreen(
                    modifier = modifier,
                    receiver = showChatDetail!!,
                    onBack = { showChatDetail = null },
                    viewModel = loginViewModel
                )
            } else if (selectedUserValue != null) {
                BackHandler { loginViewModel.selectUser(null) }
                UserProfileScreen(modifier = modifier, user = selectedUserValue, onBack = { loginViewModel.selectUser(null) })
            } else if (showPlans) {
                BackHandler { showPlans = false }
                Scaffold(
                    modifier = modifier,
                    topBar = {
                        TopAppBar(
                            title = { Text("Shaadi App") },
                            actions = {
                                IconButton(onClick = {
                                    loginViewModel.fetchFavourites(currentUser.id)
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
                                    loginViewModel.selectUser(null)
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
                                    loginViewModel.selectUser(null)
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
                                    loginViewModel.selectUser(null)
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
                                    loginViewModel.selectUser(null)
                                },
                                icon = profileIcon,
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
                    PlansScreen(modifier = Modifier.padding(padding), onBack = { showPlans = false }, viewModel = plansViewModel)
                }
            } else if (showFavourites) {
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
                                    loginViewModel.selectUser(null)
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
                                    loginViewModel.selectUser(null)
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
                                    loginViewModel.selectUser(null)
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
                                    loginViewModel.selectUser(null)
                                },
                                icon = profileIcon,
                                label = { Text("Profile") }
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = {
                                   showPlans = true
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
                                    loginViewModel.selectUser(null)
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
                                    loginViewModel.selectUser(null)
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
                                    loginViewModel.selectUser(null)
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
                                    loginViewModel.selectUser(null)
                                },
                                icon = profileIcon,
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
                        viewModel = loginViewModel,
                        onChatStatusChanged = { isVisible -> isChatDetailVisible = isVisible }
                    )
                }
            } else {
                Scaffold(
                    modifier = modifier,
                    topBar = {
                        if (!isChatDetailVisible) {
                            TopAppBar(
                                title = { Text("Shaadi App") },
                                actions = {
                                    IconButton(onClick = {
                                        loginViewModel.fetchFavourites(currentUser.id)
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
                                        loginViewModel.selectUser(null)
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
                                        loginViewModel.selectUser(null)
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
                                        loginViewModel.selectUser(null)
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
                                        loginViewModel.selectUser(null)
                                    },
                                    icon = profileIcon,
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
                            viewModel = loginViewModel,
                            onChatClick = { user ->
                                showChatDetail = user
                            }
                        )
                        1 -> SearchScreen(modifier = Modifier.padding(padding))
                        2 -> MessagesScreen(
                            modifier = Modifier.padding(padding),
                            viewModel = loginViewModel,
                            refreshTrigger = refreshMessages,
                            onChatStatusChanged = { isVisible -> isChatDetailVisible = isVisible }
                        )
                        3 -> ProfileScreen(modifier = Modifier.padding(padding), viewModel = loginViewModel)
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
