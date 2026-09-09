package com.ai.assistance.operit.ui.features.game.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ai.assistance.operit.ui.features.chat.screens.AIChatScreen
import com.ai.assistance.operit.ui.features.chat.viewmodel.ChatViewModel

/**
 * 游戏对话界面：进入后自动把游戏设定发送为第一条消息，AI 开始扮演游戏。
 * 输入区受限为仅 txt 上传 + 文字，保留模型切换、思考程度与上下文设置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameChatScreen(
        gameIntro: String,
        gameSettings: String,
        characterSettings: String,
        difficulty: String,
        onGoBack: () -> Unit = {}
) {
    // 使用路由级 viewModel（与 AIChatScreen 共享同一 ChatViewModel 实例）
    val chatViewModel: ChatViewModel = viewModel { ChatViewModel(androidx.compose.ui.platform.LocalContext.current.applicationContext) }
    var hasSentInitial by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    // 首次进入时发送游戏初始化消息并开启最大上下文
    LaunchedEffect(Unit) {
        if (hasSentInitial) return@LaunchedEffect
        hasSentInitial = true
        // 创建新对话
        chatViewModel.createNewChat()
        // 开启最大上下文模式（防止玩太久忘记设定）
        chatViewModel.toggleEnableMaxContextMode()
        val initialMessage = buildGameInitialMessage(
                gameIntro = gameIntro,
                gameSettings = gameSettings,
                characterSettings = characterSettings,
                difficulty = difficulty
        )
        if (initialMessage.isNotBlank()) {
            chatViewModel.sendTextMessage(initialMessage)
        }
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("游戏") },
                        navigationIcon = {
                            IconButton(onClick = onGoBack) {
                                Text("←", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                            }
                        }
                )
            }
    ) { padding ->
        Box(
                modifier =
                        Modifier
                                .fillMaxSize()
                                .padding(padding)
        ) {
            AIChatScreen(
                    padding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    viewModel = chatViewModel,
                    isFloatingMode = false,
                    embedded = false,
                    restrictedGameMode = true
            )
        }
    }
}

/**
 * 构建游戏初始化消息（游戏介绍 + 游戏设定 + 人物设定 + 难度）。
 */
private fun buildGameInitialMessage(
        gameIntro: String,
        gameSettings: String,
        characterSettings: String,
        difficulty: String
): String {
    val sb = StringBuilder()
    sb.append("【游戏开始】\n")
    sb.append("===== 游戏介绍 =====\n")
    sb.append(gameIntro)
    if (gameSettings.isNotBlank()) {
        sb.append("\n\n===== 游戏设定 =====\n")
        sb.append(gameSettings)
    }
    if (characterSettings.isNotBlank()) {
        sb.append("\n\n===== 人物设定 =====\n")
        sb.append(characterSettings)
    }
    if (difficulty.isNotBlank()) {
        sb.append("\n\n===== 选择难度 =====\n")
        sb.append("本局难度：$difficulty")
    }
    sb.append("\n\n请阅读以上内容并开始游戏。")
    return sb.toString()
}