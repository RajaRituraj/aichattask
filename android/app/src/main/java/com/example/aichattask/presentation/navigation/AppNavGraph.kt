package com.example.aichattask.presentation.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.aichattask.domain.model.Message
import com.example.aichattask.presentation.ai.AiPanelSheet
import com.example.aichattask.presentation.chat.ChatScreen
import com.example.aichattask.presentation.onboarding.OnboardingScreen
import com.example.aichattask.presentation.tasks.TasksScreen
import com.example.aichattask.theme.SurfaceVariant
import kotlinx.serialization.Serializable

@Serializable data object OnboardingRoute : NavKey
@Serializable data class ChatRoute(val userId: String, val userName: String) : NavKey
@Serializable data object TasksRoute : NavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph() {
    val backStack = rememberNavBackStack(OnboardingRoute)

    // AI Panel sheet state (lives outside nav so it persists across nav transitions)
    var showAiPanel by remember { mutableStateOf(false) }
    var aiMessages by remember { mutableStateOf<List<Message>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = { key ->
            when (key) {
                is OnboardingRoute -> NavEntry(key = key) {
                    OnboardingScreen(
                        onEnterChat = { userId, userName ->
                            backStack.add(ChatRoute(userId = userId, userName = userName))
                        }
                    )
                }
                is ChatRoute -> NavEntry(key = key) {
                    ChatScreen(
                        userId = key.userId,
                        userName = key.userName,
                        onOpenAiPanel = { messages ->
                            aiMessages = messages
                            showAiPanel = true
                        },
                        onOpenTasks = { backStack.add(TasksRoute) }
                    )
                }
                is TasksRoute -> NavEntry(key = key) {
                    TasksScreen(onBack = { backStack.removeLastOrNull() })
                }
                else -> NavEntry(key = key as NavKey) {}
            }
        }
    )

    if (showAiPanel) {
        ModalBottomSheet(
            onDismissRequest = { showAiPanel = false },
            sheetState = sheetState,
            containerColor = SurfaceVariant
        ) {
            AiPanelSheet(
                messages = aiMessages,
                onDismiss = { showAiPanel = false }
            )
        }
    }
}

