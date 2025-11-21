package com.example.myapplication.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.data.model.Message
import com.example.myapplication.data.model.User
import com.example.myapplication.ui.viewmodel.LoginViewModel
import com.example.myapplication.ui.viewmodel.LoginState
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(modifier: Modifier = Modifier, receiver: User, onBack: () -> Unit, viewModel: LoginViewModel = viewModel()) {
    val loginState by viewModel.loginState.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val sendMessageError by viewModel.sendMessageError.collectAsState()
    var newMessage by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }

    val currentUser = (loginState as? LoginState.Success)?.user

    // Refresh messages when entering chat
    LaunchedEffect(currentUser, receiver) {
        currentUser?.let { viewModel.fetchMessages(it.id) }
    }

    // Show error dialog when sendMessageError is set
    LaunchedEffect(sendMessageError) {
        if (sendMessageError != null) {
            showErrorDialog = true
        }
    }

    val conversationMessages = messages.filter {
        (it.sender.id == currentUser?.id && it.receiver.id == receiver.id) ||
        (it.sender.id == receiver.id && it.receiver.id == currentUser?.id)
    }.sortedBy { it.sentAt }

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

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Chat with ${receiver.name}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("< Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Messages list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                items(conversationMessages) { message ->
                    ChatBubble(message = message, currentUserId = currentUser?.id ?: 0)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Message input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                        viewModel.sendMessage(currentUser, receiver, newMessage)
                        newMessage = ""
                        // Refresh messages after sending
                        currentUser.let { viewModel.refreshMessages(it.id) }
                    }
                }) {
                    Text("Send")
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
