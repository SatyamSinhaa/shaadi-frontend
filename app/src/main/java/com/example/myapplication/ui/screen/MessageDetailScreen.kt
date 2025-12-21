package com.example.myapplication.ui.screen

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.viewmodel.LoginViewModel
import com.example.myapplication.ui.viewmodel.LoginState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(modifier: Modifier = Modifier, receiver: User, onBack: () -> Unit, viewModel: LoginViewModel = viewModel()) {
    val loginState by viewModel.loginState.collectAsState()
    val allMessages by viewModel.messages.collectAsState() // Observe messages in real-time
    val sendMessageError by viewModel.sendMessageError.collectAsState()
    var newMessage by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showBlockConfirmation by remember { mutableStateOf(false) }

    val currentUser = (loginState as? LoginState.Success)?.user

    // LazyListState for scroll management
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Filter messages for this conversation and sort by time
    val conversationMessages = remember(allMessages, currentUser, receiver) {
        allMessages.filter {
            (it.sender.id == currentUser?.id && it.receiver.id == receiver.id) ||
            (it.sender.id == receiver.id && it.receiver.id == currentUser?.id)
        }.sortedBy { it.sentAt }
    }

    // Load initial messages and mark as read when entering chat
    LaunchedEffect(currentUser, receiver) {
        currentUser?.let {
            Log.d("MessageDetailScreen", "🔄 Loading initial messages for conversation")
            viewModel.fetchMessages(it.id)
            viewModel.markMessagesAsRead(it.id, receiver.id)
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(conversationMessages.size) {
        if (conversationMessages.isNotEmpty()) {
            Log.d("MessageDetailScreen", "📜 Auto-scrolling to message ${conversationMessages.size}")
            coroutineScope.launch {
                listState.animateScrollToItem(conversationMessages.size - 1)
            }
        }
    }

    // Show error dialog when sendMessageError is set
    LaunchedEffect(sendMessageError) {
        if (sendMessageError != null) {
            showErrorDialog = true
        }
    }

    if (showErrorDialog && sendMessageError != null) {
        AlertDialog(
            onDismissRequest = {
                showErrorDialog = false
                viewModel.clearSendMessageError()
            },
            title = { Text("Chat Limit Exhausted") },
            text = { Text(sendMessageError!!) },
            confirmButton = {
                Button(onClick = {
                    showErrorDialog = false
                    viewModel.clearSendMessageError()
                }) {
                    Text("OK")
                }
            }
        )
    }

    // Block User Confirmation Dialog
    if (showBlockConfirmation) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmation = false },
            title = { Text("Block User") },
            text = { Text("Are you sure you want to block ${receiver.name}? You won't receive messages from them anymore.") },
            confirmButton = {
                Button(onClick = {
                    if (currentUser != null) {
                        viewModel.blockUser(currentUser.id, receiver.id) {
                            showBlockConfirmation = false
                            onBack()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Block")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBlockConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (receiver.photoUrl != null) {
                                AsyncImage(
                                    model = receiver.photoUrl,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = receiver.name.firstOrNull()?.toString() ?: "?",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = receiver.name)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Block User") },
                            onClick = {
                                showMenu = false
                                showBlockConfirmation = true
                            }
                        )
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newMessage,
                        onValueChange = { newMessage = it },
                        label = { Text("Type a message") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        if (newMessage.isNotBlank() && currentUser != null) {
                            Log.d("MessageDetailScreen", "📤 Sending message: $newMessage")
                            viewModel.sendMessage(currentUser, receiver, newMessage)
                            newMessage = ""
                            // No need to refresh - WebSocket will deliver the message
                        }
                    }) {
                        Text("Send")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            // Messages list with auto-scroll
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                reverseLayout = false
            ) {
                items(conversationMessages) { message ->
                    ChatBubble(message = message, currentUserId = currentUser?.id ?: 0)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: Message, currentUserId: Int) {
    val isFromCurrentUser = message.sender.id == currentUserId

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 250.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isFromCurrentUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.content,
                    color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val displayTime = try {
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val date = dateFormat.parse(message.sentAt)
                    timeFormat.format(date)
                } catch (e: Exception) {
                    message.sentAt // fallback to original if parsing fails
                }
                Text(
                    text = displayTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isFromCurrentUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
