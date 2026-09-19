package com.example.aichattask.presentation.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aichattask.domain.model.Message
import com.example.aichattask.domain.repository.ConnectionState
import com.example.aichattask.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    userId: String,
    userName: String,
    onOpenAiPanel: (List<Message>) -> Unit,
    onOpenTasks: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(userId) {
        viewModel.initialize(userId, userName)
    }

    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            ChatTopBar(
                connectionState = uiState.connectionState,
                onlineUserCount = uiState.onlineUsers.size,
                onOpenAiPanel = { onOpenAiPanel(uiState.messages) },
                onOpenTasks = onOpenTasks
            )
        },
        bottomBar = {
            ChatInput(
                text = uiState.inputText,
                onTextChange = viewModel::onInputChange,
                onSend = viewModel::sendMessage,
                typingUsers = uiState.typingUsers
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Connection banner
            AnimatedVisibility(
                visible = uiState.connectionState != ConnectionState.CONNECTED,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ConnectionBanner(state = uiState.connectionState)
            }

            // Messages
            if (uiState.messages.isEmpty()) {
                EmptyChat(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically { it / 2 }
                        ) {
                            MessageBubble(message = message)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
    connectionState: ConnectionState,
    onlineUserCount: Int,
    onOpenAiPanel: () -> Unit,
    onOpenTasks: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "General Chat",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = OnSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val dotColor = when (connectionState) {
                        ConnectionState.CONNECTED -> ConnectedGreen
                        ConnectionState.RECONNECTING -> ReconnectingOrange
                        else -> ErrorRed
                    }
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val statusText = when (connectionState) {
                        ConnectionState.CONNECTED -> "$onlineUserCount online"
                        ConnectionState.CONNECTING -> "Connecting…"
                        ConnectionState.RECONNECTING -> "Reconnecting…"
                        ConnectionState.DISCONNECTED -> "Offline"
                        ConnectionState.ERROR -> "Error"
                    }
                    Text(text = statusText, style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                }
            }
        },
        actions = {
            IconButton(onClick = onOpenTasks) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Tasks", tint = SecondaryTeal)
            }
            IconButton(onClick = onOpenAiPanel) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI Panel", tint = PrimaryLight)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = SurfaceDark,
            titleContentColor = OnSurface
        )
    )
}

@Composable
private fun ConnectionBanner(state: ConnectionState) {
    val (bg, text) = when (state) {
        ConnectionState.RECONNECTING -> ReconnectingOrange to "Reconnecting…"
        ConnectionState.DISCONNECTED -> ErrorRed to "No connection"
        ConnectionState.CONNECTING -> PrimaryPurple to "Connecting…"
        ConnectionState.ERROR -> ErrorRed to "Connection error"
        else -> return
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg.copy(alpha = 0.9f))
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

@Composable
private fun MessageBubble(message: Message) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeStr = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isOwn) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isOwn) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(PrimaryPurple, SecondaryTeal))
                    )
                    .align(Alignment.Bottom),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.senderName.take(1).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (message.isOwn) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            if (!message.isOwn) {
                Text(
                    text = message.senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryLight,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
            }
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (message.isOwn) 18.dp else 4.dp,
                            bottomEnd = if (message.isOwn) 4.dp else 18.dp
                        )
                    )
                    .background(if (message.isOwn) BubbleOwn else BubbleOther)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface,
                    lineHeight = 20.sp
                )
            }
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceMuted,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        if (message.isOwn) {
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun ChatInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    typingUsers: List<String>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
    ) {
        // Typing indicator
        AnimatedVisibility(visible = typingUsers.isNotEmpty()) {
            val typingText = when {
                typingUsers.size == 1 -> "${typingUsers[0]} is typing…"
                typingUsers.size == 2 -> "${typingUsers[0]} and ${typingUsers[1]} are typing…"
                else -> "Several people are typing…"
            }
            Text(
                text = typingText,
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceMuted,
                modifier = Modifier.padding(start = 20.dp, top = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Message…", color = OnSurfaceMuted) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryPurple,
                    unfocusedBorderColor = SurfaceElevated,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface,
                    cursorColor = PrimaryLight
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (text.isNotBlank())
                            Brush.linearGradient(listOf(PrimaryPurple, SecondaryTeal))
                        else
                            Brush.linearGradient(listOf(SurfaceCard, SurfaceCard))
                    )
                    .clickable(enabled = text.isNotBlank(), onClick = onSend),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (text.isNotBlank()) Color.White else OnSurfaceMuted,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyChat(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ChatBubbleOutline,
            contentDescription = null,
            tint = OnSurfaceMuted,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No messages yet",
            style = MaterialTheme.typography.titleMedium,
            color = OnSurfaceMuted
        )
        Text(
            text = "Say hi to start the conversation!",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceMuted,
            textAlign = TextAlign.Center
        )
    }
}
