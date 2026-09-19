package com.example.aichattask.presentation.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.aichattask.domain.model.AiTaskSuggestion
import com.example.aichattask.domain.model.Message
import com.example.aichattask.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPanelSheet(
    messages: List<Message>,
    onDismiss: () -> Unit,
    onScrollToMessage: (String) -> Unit = {},
    viewModel: AiPanelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(messages) {
        viewModel.setMessages(messages)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .background(SurfaceVariant)
            .navigationBarsPadding()
    ) {
        // Handle bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(OnSurfaceMuted.copy(alpha = 0.4f))
            )
        }

        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = PrimaryLight,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "AI Assistant",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = OnSurface
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { viewModel.reset(); onDismiss() }) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = OnSurfaceMuted)
            }
        }

        HorizontalDivider(color = SurfaceElevated)

        // Mode buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AiModeChip(
                label = "Summarize",
                icon = Icons.Default.Summarize,
                isSelected = uiState.mode == AiMode.SUMMARY,
                gradient = listOf(PrimaryPurple, PrimaryContainer),
                onClick = viewModel::summarize,
                modifier = Modifier.weight(1f)
            )
            AiModeChip(
                label = "Ask",
                icon = Icons.Default.QuestionAnswer,
                isSelected = uiState.mode == AiMode.ASK,
                gradient = listOf(SecondaryTeal, SecondaryContainer),
                onClick = { /* handled separately */ },
                modifier = Modifier.weight(1f)
            )
            AiModeChip(
                label = "Tasks",
                icon = Icons.Default.Task,
                isSelected = uiState.mode == AiMode.TASKS,
                gradient = listOf(AccentAmber, Color(0xFF92400E)),
                onClick = viewModel::extractTasks,
                modifier = Modifier.weight(1f)
            )
        }

        // Content
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> LoadingIndicator()
                uiState.error != null -> ErrorCard(message = uiState.error!!, onRetry = {
                    when (uiState.mode) {
                        AiMode.SUMMARY -> viewModel.summarize()
                        AiMode.ASK -> viewModel.askQuestion()
                        AiMode.TASKS -> viewModel.extractTasks()
                        else -> {}
                    }
                })
                else -> when (uiState.mode) {
                    AiMode.IDLE -> IdleHint()
                    AiMode.SUMMARY -> SummaryContent(
                        text = uiState.summaryText,
                        isStreaming = uiState.isSummaryStreaming
                    )
                    AiMode.ASK -> AskContent(
                        question = uiState.question,
                        answerData = uiState.answerData,
                        onQuestionChange = viewModel::onQuestionChange,
                        onAsk = viewModel::askQuestion,
                        onScrollToMessage = onScrollToMessage
                    )
                    AiMode.TASKS -> TasksContent(
                        suggestions = uiState.taskSuggestions,
                        confirmed = uiState.confirmedTasks,
                        onConfirm = viewModel::confirmTask
                    )
                }
            }
        }
    }
}

@Composable
private fun AiModeChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) Brush.linearGradient(gradient)
                else Brush.linearGradient(listOf(SurfaceCard, SurfaceCard))
            )
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Transparent else SurfaceElevated,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else OnSurfaceMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) Color.White else OnSurfaceMuted
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryLight)
            Spacer(Modifier.height(12.dp))
            Text("Thinking…", color = OnSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ErrorRed.copy(alpha = 0.15f))
            .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(message, color = ErrorRed, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
        ) { Text("Retry") }
    }
}

@Composable
private fun IdleHint() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.AutoAwesome, null, tint = PrimaryLight.copy(alpha = 0.4f), modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(16.dp))
        Text("Pick an action above", color = OnSurfaceMuted, style = MaterialTheme.typography.bodyLarge)
        Text("Summarize the chat, ask a question, or extract tasks", color = OnSurfaceMuted.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun SummaryContent(text: String, isStreaming: Boolean) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Summary", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = OnSurface)
            if (isStreaming) {
                Spacer(Modifier.width(8.dp))
                val inf = rememberInfiniteTransition(label = "")
                val alpha by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "")
                Box(Modifier.size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(PrimaryLight.copy(alpha = alpha)))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
            color = OnSurface
        )
    }
}

@Composable
private fun AskContent(
    question: String,
    answerData: com.example.aichattask.domain.model.AiAnswer?,
    onQuestionChange: (String) -> Unit,
    onAsk: () -> Unit,
    onScrollToMessage: (String) -> Unit
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = question,
            onValueChange = onQuestionChange,
            label = { Text("Ask about this conversation…") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SecondaryTeal,
                unfocusedBorderColor = SurfaceElevated,
                focusedTextColor = OnSurface, unfocusedTextColor = OnSurface,
                cursorColor = SecondaryTeal
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onAsk() }),
            trailingIcon = {
                IconButton(onClick = onAsk, enabled = question.isNotBlank()) {
                    Icon(Icons.Default.Search, null, tint = if (question.isNotBlank()) SecondaryTeal else OnSurfaceMuted)
                }
            }
        )

        answerData?.let { answer ->
            Spacer(Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceCard)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Answer", style = MaterialTheme.typography.labelMedium, color = SecondaryTeal)
                Text(answer.answer, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp), color = OnSurface)

                if (answer.referencedMessageId != null && answer.excerpt != null) {
                    HorizontalDivider(color = SurfaceElevated)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceElevated)
                            .clickable { onScrollToMessage(answer.referencedMessageId) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(3.dp).height(36.dp).background(SecondaryTeal, RoundedCornerShape(2.dp)))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Referenced message", style = MaterialTheme.typography.labelSmall, color = SecondaryTeal)
                            Text(answer.excerpt, style = MaterialTheme.typography.bodySmall, color = OnSurfaceMuted, maxLines = 2)
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowForward, null, tint = OnSurfaceMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksContent(
    suggestions: List<AiTaskSuggestion>,
    confirmed: Set<String>,
    onConfirm: (AiTaskSuggestion) -> Unit
) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (suggestions.isEmpty()) {
            Text("No tasks found in this conversation.", color = OnSurfaceMuted, style = MaterialTheme.typography.bodyMedium)
        } else {
            Text(
                "${suggestions.size} task${if (suggestions.size != 1) "s" else ""} found",
                style = MaterialTheme.typography.labelMedium, color = AccentAmber
            )
            suggestions.forEach { suggestion ->
                val isConfirmed = confirmed.contains(suggestion.title)
                TaskSuggestionCard(suggestion = suggestion, isConfirmed = isConfirmed, onConfirm = { onConfirm(suggestion) })
            }
        }
    }
}

@Composable
private fun TaskSuggestionCard(
    suggestion: AiTaskSuggestion,
    isConfirmed: Boolean,
    onConfirm: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isConfirmed) AccentGreen.copy(alpha = 0.1f) else SurfaceCard)
            .border(1.dp, if (isConfirmed) AccentGreen.copy(alpha = 0.4f) else SurfaceElevated, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(suggestion.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = OnSurface)
            if (suggestion.owner != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, tint = OnSurfaceMuted, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(suggestion.owner, style = MaterialTheme.typography.labelSmall, color = OnSurfaceMuted)
                }
            }
            if (suggestion.dueDate != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, tint = AccentAmber, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(suggestion.dueDate, style = MaterialTheme.typography.labelSmall, color = AccentAmber)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        if (isConfirmed) {
            Icon(Icons.Default.CheckCircle, null, tint = AccentGreen, modifier = Modifier.size(28.dp))
        } else {
            FilledTonalButton(
                onClick = onConfirm,
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = PrimaryContainer),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text("Save", style = MaterialTheme.typography.labelSmall, color = PrimaryLight)
            }
        }
    }
}
