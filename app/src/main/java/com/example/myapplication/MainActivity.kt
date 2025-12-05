package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.screen.*
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.viewmodel.LoginState
import com.example.myapplication.ui.viewmodel.LoginViewModel
import com.example.myapplication.ui.viewmodel.PlansViewModel

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
    val messages by loginViewModel.messages.collectAsState()
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
    var showHistory by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // State to control bottom bar visibility
    var isChatDetailVisible by remember { mutableStateOf(false) }

    // Handle registration flow screens
    if (showPersonalDetails && registeredUser != null) {
        BackHandler { showPersonalDetails = false }
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = { Text("Personal Details") },
                    actions = {
                        IconButton(onClick = {
                            loginViewModel.fetchFavourites(registeredUser!!.id)
                            showFavourites = true
                        }) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Favourites")
                        }
                        IconButton(onClick = { /* Menu action */ }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            PersonalDetailsScreen(
                modifier = Modifier.padding(innerPadding),
                user = registeredUser!!,
                onNext = { updatedUser ->
                    registeredUser = updatedUser
                    showPersonalDetails = false
                    showLocationDetails = true
                }
            )
        }
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
                loginViewModel.updateUser(updatedUser)
                loginViewModel.fetchAllUsers()
            }
        )
        return
    }

    // Navigation Item Definitions
    // 0: Home, 1: Matches, 2: Search, 3: Messages, 4: Profile
    val navItems = listOf(
        Triple("Home", Icons.Filled.Home, 0),
        Triple("Matches", Icons.Filled.ThumbUp, 1),
        Triple("Search", Icons.Filled.Search, 2),
        Triple("Messages", Icons.Filled.Email, 3),
        Triple("Profile", null, 4) // Profile uses custom icon logic
    )

    when (loginState) {
        is LoginState.Success -> {
            val currentUser = (loginState as LoginState.Success).user
            val selectedUserValue = selectedUser

            // Fetch messages to keep badge count updated
            LaunchedEffect(currentUser.id) {
                loginViewModel.fetchMessages(currentUser.id)
            }

            val unreadConversationCount = remember(messages, currentUser.id) {
                messages
                    .filter { it.receiver.id == currentUser.id && !it.read }
                    .distinctBy { it.sender.id }
                    .count()
            }

            val profileIcon: @Composable () -> Unit = {
                if (currentUser.photoUrl != null) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .then(
                                if (selectedTab == 4) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
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

            // Common Bottom Bar Composable to reduce duplication
            val bottomBarContent: @Composable () -> Unit = {
                NavigationBar {
                    navItems.forEach { (label, icon, index) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                showFavourites = false
                                showMessages = false
                                showPlans = false
                                loginViewModel.selectUser(null)
                                if (index == 3) refreshMessages = !refreshMessages
                            },
                            icon = {
                                if (index == 4) {
                                    profileIcon()
                                } else if (index == 3) {
                                    // Messages with Badge
                                    if (unreadConversationCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(unreadConversationCount.toString())
                                                }
                                            }
                                        ) {
                                            Icon(icon!!, contentDescription = label)
                                        }
                                    } else {
                                        Icon(icon!!, contentDescription = label)
                                    }
                                } else {
                                    Icon(icon!!, contentDescription = label)
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                    NavigationBarItem(
                        selected = showPlans,
                        onClick = {
                            showPlans = true
                            selectedTab = -1
                        },
                        icon = { Icon(Icons.Filled.Star, contentDescription = "Premium Membership") },
                        label = { Text("Premium") }
                    )
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
                UserProfileScreen(
                    modifier = modifier,
                    user = selectedUserValue,
                    onBack = { loginViewModel.selectUser(null) },
                    onChatClick = { user -> showChatDetail = user }
                )
            } else if (showHistory) {
                BackHandler { showHistory = false }
                SubscriptionHistoryScreen(
                    modifier = modifier,
                    onBack = { showHistory = false },
                    viewModel = plansViewModel
                )
            } else if (showPlans) {
                BackHandler { showPlans = false }
                Scaffold(
                    modifier = modifier,
                    topBar = {
                        TopAppBar(
                            title = { Text("Shaadi App") },
                            actions = {
                                IconButton(onClick = {
                                    plansViewModel.fetchSubscriptionHistory(currentUser.id)
                                    showHistory = true
                                }) {
                                    Icon(Icons.Filled.History, contentDescription = "History")
                                }
                            }
                        )
                    },
                    bottomBar = bottomBarContent
                ) { padding ->
                    PlansScreen(modifier = Modifier.padding(padding), onBack = { showPlans = false }, viewModel = plansViewModel)
                }
            } else if (showFavourites) {
                BackHandler { showFavourites = false }
                Scaffold(
                    bottomBar = bottomBarContent
                ) { padding ->
                    FavouritesScreen(modifier = Modifier.padding(padding), onBack = { showFavourites = false })
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
                                    if (selectedTab == 4) {
                                        IconButton(onClick = { /* Menu action */ }) {
                                            Icon(Icons.Filled.Menu, contentDescription = "Options")
                                        }
                                    }
                                }
                            )
                        }
                    },
                    bottomBar = {
                        if (!isChatDetailVisible) {
                            bottomBarContent()
                        }
                    }
                ) { padding ->
                    when (selectedTab) {
                        0 -> UserListScreen(
                            modifier = Modifier.padding(padding),
                            viewModel = loginViewModel,
                            onChatClick = { user ->
                                showChatDetail = user
                            },
                            onUserProfileClick = { user ->
                                loginViewModel.fetchUserById(user.id)
                            }
                        )
                        1 -> MatchesScreen(modifier = Modifier.padding(padding))
                        2 -> SearchScreen(
                            modifier = Modifier.padding(padding),
                            onUserProfileClick = { user ->
                                loginViewModel.fetchUserById(user.id)
                            }
                        )
                        3 -> MessagesScreen(
                            modifier = Modifier.padding(padding),
                            viewModel = loginViewModel,
                            refreshTrigger = refreshMessages,
                            onChatStatusChanged = { isVisible -> isChatDetailVisible = isVisible }
                        )
                        4 -> ProfileScreen(modifier = Modifier.padding(padding), viewModel = loginViewModel)
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
