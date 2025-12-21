package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.screen.*
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.viewmodel.LoginState
import com.example.myapplication.ui.viewmodel.LoginViewModel
import com.example.myapplication.ui.viewmodel.PlansViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppNavigation()
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
    val unreadNotificationCount by loginViewModel.unreadNotificationCount.collectAsState()
    val selectedUser by loginViewModel.selectedUser.collectAsState<User?>()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showFavourites by remember { mutableStateOf(false) }
    var showMessages by remember { mutableStateOf(false) }
    var showRegister by remember { mutableStateOf(false) }
    var registrationSuccessMessage by remember { mutableStateOf<String?>(null) }
    var registeredUser by remember { mutableStateOf<User?>(null) }
    var refreshMessages by remember { mutableStateOf(false) }
    var showPlans by remember { mutableStateOf(false) }
    var showChatDetail by remember { mutableStateOf<User?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var showBlockedProfiles by remember { mutableStateOf(false) }
    var showSubscriptionDetails by remember { mutableStateOf(false) } // Add this state
    val context = LocalContext.current

    // State for Search in Messages
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // State to control bottom bar visibility
    var isChatDetailVisible by remember { mutableStateOf(false) }

    // Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Navigation Item Definitions
    // 0: Home, 1: Matches, 2: Messages, 3: Profile
    val navItems = listOf(
        Triple("Home", Icons.Filled.Home, 0),
        Triple("Matches", Icons.Filled.ThumbUp, 1),
        Triple("Messages", Icons.Filled.Email, 2),
        Triple("Profile", null, 3) // Profile uses custom icon logic
    )

    when (loginState) {
        is LoginState.Success -> {
            val currentUser = (loginState as LoginState.Success).user
            val selectedUserValue = selectedUser

            // Fetch messages and notifications to keep badge counts updated
            LaunchedEffect(currentUser.id) {
                loginViewModel.fetchMessages(currentUser.id)
                loginViewModel.fetchUnreadNotificationCount(currentUser.id)
            }

            val unreadConversationCount = remember(messages, currentUser.id) {
                messages
                    .filter { it.receiver.id == currentUser.id && !it.read }
                    .distinctBy { it.sender.id }
                    .count()
            }

            // Check for missing profile fields
            val hasMissingFields = remember(currentUser) {
                currentUser.bio.isNullOrBlank() ||
                currentUser.age == null ||
                currentUser.gender.isNullOrBlank() ||
                currentUser.gotr.isNullOrBlank() ||
                currentUser.caste.isNullOrBlank() ||
                currentUser.category.isNullOrBlank() ||
                currentUser.religion.isNullOrBlank() ||
                currentUser.cityTown.isNullOrBlank() ||
                currentUser.district.isNullOrBlank() ||
                currentUser.state.isNullOrBlank()
            }

            val profileIcon: @Composable () -> Unit = {
                Box {
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

                    // Warning badge for missing fields
                    if (hasMissingFields) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Incomplete Profile",
                            tint = Color.Red, // Or MaterialTheme.colorScheme.error
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.TopEnd)
                                .background(MaterialTheme.colorScheme.surface, CircleShape)
                        )
                    }
                }
            }

            // Common Bottom Bar Composable to reduce duplication
            val bottomBarContent: @Composable () -> Unit = {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    navItems.forEach { (label, icon, index) ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = {
                                selectedTab = index
                                showFavourites = false
                                showMessages = false
                                showPlans = false
                                showSearch = false
                                showNotifications = false
                                isSearchActive = false // Reset search when switching tabs
                                searchQuery = ""
                                loginViewModel.selectUser(null)
                                if (index == 2) refreshMessages = !refreshMessages
                            },
                            icon = {
                                if (index == 3) {
                                    profileIcon()
                                } else if (index == 2) {
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
                // Chat detail takes precedence over user profile or other overlays
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
                    onChatClick = { user -> showChatDetail = user },
                    onAcceptRequest = {} 
                )
            } else if (showBlockedProfiles) {
                BackHandler { showBlockedProfiles = false }
                BlockedProfilesScreen(
                    modifier = modifier,
                    onBack = { showBlockedProfiles = false },
                    onUserClick = { userId ->
                        loginViewModel.fetchUserById(userId)
                    }
                )
            } else if (showSubscriptionDetails) {
                BackHandler { showSubscriptionDetails = false }
                SubscriptionDetailScreen(
                    modifier = modifier,
                    onBack = { showSubscriptionDetails = false },
                    viewModel = loginViewModel
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
            } else if (showSearch) {
                // This is the global search screen (not used for messages)
                BackHandler { showSearch = false }
                Scaffold(
                    bottomBar = bottomBarContent
                ) { padding ->
                    SearchScreen(
                        modifier = Modifier.padding(padding),
                        onUserProfileClick = { user ->
                            loginViewModel.fetchUserById(user.id)
                        }
                    )
                }
            } else if (showNotifications) {
                BackHandler { showNotifications = false }
                NotificationsScreen(
                    modifier = modifier,
                    onBack = { showNotifications = false },
                    viewModel = loginViewModel
                )
            } else {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                        drawerContent = {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                ModalDrawerSheet(
                                    modifier = Modifier.fillMaxWidth(0.7f) // Set width to 70% of screen
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Menu",
                                            style = MaterialTheme.typography.headlineMedium,
                                            modifier = Modifier.padding(bottom = 16.dp)
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("Blocked Profiles") },
                                            selected = false,
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                showBlockedProfiles = true
                                            },
                                            icon = { Icon(Icons.Filled.Block, contentDescription = null) }
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("Subscription Details") },
                                            selected = false,
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                showSubscriptionDetails = true
                                            },
                                            icon = { Icon(Icons.Filled.Payment, contentDescription = null) }
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("About Us") },
                                            selected = false,
                                            onClick = { /* Handle About click */ scope.launch { drawerState.close() } },
                                            icon = { Icon(Icons.Filled.Info, contentDescription = null) }
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("Contact Us") },
                                            selected = false,
                                            onClick = { /* Handle Contact click */ scope.launch { drawerState.close() } },
                                            icon = { Icon(Icons.Filled.Email, contentDescription = null) }
                                        )
                                        NavigationDrawerItem(
                                            label = { Text("Terms & Policy") },
                                            selected = false,
                                            onClick = { /* Handle Terms click */ scope.launch { drawerState.close() } },
                                            icon = { Icon(Icons.Filled.Description, contentDescription = null) }
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        NavigationDrawerItem(
                                            label = { Text("Logout") },
                                            selected = false,
                                            onClick = {
                                                scope.launch { drawerState.close() }
                                                loginViewModel.logout()
                                            },
                                            icon = { Icon(Icons.Filled.ExitToApp, contentDescription = null) }
                                        )
                                    }
                                }
                            }
                        },
                        gesturesEnabled = drawerState.isOpen
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Scaffold(
                                modifier = modifier,
                                topBar = {
                                    if (!isChatDetailVisible) {
                                        if (isSearchActive && selectedTab == 2) {
                                            // Show Search Bar in TopAppBar when active in Messages tab
                                            TopAppBar(
                                                title = {
                                                    TextField(
                                                        value = searchQuery,
                                                        onValueChange = { searchQuery = it },
                                                        placeholder = { Text("Search messages...") },
                                                        singleLine = true,
                                                        colors = TextFieldDefaults.colors(
                                                            focusedContainerColor = Color.Transparent,
                                                            unfocusedContainerColor = Color.Transparent,
                                                            disabledContainerColor = Color.Transparent
                                                        ),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                },
                                                navigationIcon = {
                                                    IconButton(onClick = { 
                                                        isSearchActive = false
                                                        searchQuery = ""
                                                    }) {
                                                        Icon(Icons.Filled.ArrowBack, contentDescription = "Close Search")
                                                    }
                                                },
                                                actions = {
                                                    // Optional: Clear button or search button if needed, but typing usually filters directly
                                                    if (searchQuery.isNotEmpty()) {
                                                        IconButton(onClick = { searchQuery = "" }) {
                                                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                                                        }
                                                    }
                                                }
                                            )
                                            BackHandler {
                                                isSearchActive = false
                                                searchQuery = ""
                                            }
                                        } else {
                                            // Regular TopAppBar
                                            TopAppBar(
                                                title = { Text("Shaadi App") },
                                                actions = {
                                                    if (selectedTab != 3) {
                                                        IconButton(onClick = {
                                                            if (selectedTab == 2) {
                                                                isSearchActive = true
                                                            } else {
                                                                showSearch = true
                                                            }
                                                        }) {
                                                            Icon(Icons.Filled.Search, contentDescription = "Search")
                                                        }
                                                    }
                                                    IconButton(onClick = {
                                                        loginViewModel.fetchFavourites(currentUser.id)
                                                        showFavourites = true
                                                    }) {
                                                        Icon(Icons.Filled.Favorite, contentDescription = "Favourites")
                                                    }
                                                    IconButton(onClick = {
                                                        showNotifications = true
                                                    }) {
                                                        if (unreadNotificationCount > 0) {
                                                            BadgedBox(
                                                                badge = {
                                                                    Badge {
                                                                        Text(unreadNotificationCount.toString())
                                                                    }
                                                                }
                                                            ) {
                                                                Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                                                            }
                                                        } else {
                                                            Icon(Icons.Filled.Notifications, contentDescription = "Notifications")
                                                        }
                                                    }
                                                    if (selectedTab == 3) {
                                                        IconButton(onClick = {
                                                            scope.launch {
                                                                if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                                            }
                                                        }) {
                                                            Icon(Icons.Filled.Menu, contentDescription = "Options")
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                },
                                bottomBar = {
                                    if (!isChatDetailVisible) {
                                        bottomBarContent()
                                    }
                                }
                            ) { padding ->
                                when (selectedTab) {
                                    0 -> HomeScreen(
                                        modifier = Modifier.padding(padding),
                                        viewModel = loginViewModel,
                                        onUserClick = { user ->
                                            loginViewModel.fetchUserById(user.id)
                                        }
                                    )
                                    1 -> UserListScreen(
                                        modifier = Modifier.padding(padding),
                                        viewModel = loginViewModel,
                                        onChatClick = { user ->
                                            showChatDetail = user
                                        },
                                        onUserProfileClick = { user ->
                                            loginViewModel.fetchUserById(user.id)
                                        }
                                    )
                                    // Search (2) removed, Messages becomes 2
                                    2 -> MessagesScreen(
                                        modifier = Modifier.padding(padding),
                                        viewModel = loginViewModel,
                                        refreshTrigger = refreshMessages,
                                        onChatStatusChanged = { isVisible -> isChatDetailVisible = isVisible },
                                        searchQuery = searchQuery // Pass the search query down
                                    )
                                    3 -> ProfileScreen(modifier = Modifier.padding(padding), viewModel = loginViewModel)
                                }
                            }
                        }
                    }
                }
            }
        }
        else -> {
            if (showRegister) {
                // Updated RegisterScreen takes over the full registration flow
                RegisterScreen(modifier = modifier, onBackToLogin = {
                    registrationSuccessMessage = it
                    showRegister = false
                }, onRegisterSuccess = { user ->
                    registeredUser = user
                    showRegister = false
                    // After full registration success, we can treat it as login or whatever the flow requires
                    // Currently RegisterScreen handles the steps internally.
                    // If we need to login automatically, we can do it here or just show success.
                    // Assuming user needs to login:
                    registrationSuccessMessage = "Registration complete. Please Login."
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
