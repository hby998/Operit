package com.ai.assistance.operit.ui.features.game.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ai.assistance.operit.api.chat.enhance.MultiServiceManager
import com.ai.assistance.operit.core.chat.hooks.PromptTurn
import com.ai.assistance.operit.core.chat.hooks.PromptTurnKind
import com.ai.assistance.operit.data.model.FunctionType
import com.ai.assistance.operit.util.ChatUtils
import kotlinx.coroutines.launch

/**
 * 游戏设定填写界面：游戏介绍（必填）/ 游戏设定（可选）/ 人物设定（可选）
 * 支持手动输入或上传txt文件。填写游戏介绍后自动调用AI识别难度选项展示在底部，
 * 未识别到时显示"读取失败，请手动选择难度"，用户选择难度后点"下一步"进入游戏对话。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameSetupScreen(
        onGoBack: () -> Unit = {},
        onStartGame: (gameIntro: String, gameSettings: String, characterSettings: String, difficulty: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var gameIntro by remember { mutableStateOf("") }
    var gameSettings by remember { mutableStateOf("") }
    var characterSettings by remember { mutableStateOf("") }
    // 识别出的难度选项列表
    var difficultyOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedDifficulty by remember { mutableStateOf("") }
    var isRecognizing by remember { mutableStateOf(false) }
    // 识别失败标记
    var recognizeFailed by remember { mutableStateOf(false) }

    // 执行难度识别的封装函数
    fun runRecognize(introText: String) {
        if (introText.isBlank()) return
        scope.launch {
            isRecognizing = true
            recognizeFailed = false
            val options = aiRecognizeDifficulty(context, introText)
            isRecognizing = false
            if (options.isNotEmpty()) {
                difficultyOptions = options
                selectedDifficulty = options.first()
                recognizeFailed = false
            } else {
                difficultyOptions = emptyList()
                selectedDifficulty = ""
                recognizeFailed = true
            }
        }
    }

    // 文件选择器（上传txt）
    val introFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { u ->
            scope.launch {
                val text = readTextFromUri(context, u)
                if (text.isNotBlank()) {
                    gameIntro = text
                    // 文件上传后直接触发识别
                    kotlinx.coroutines.delay(200)
                    runRecognize(text)
                }
            }
        }
    }
    val settingsFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { u ->
            scope.launch {
                val text = readTextFromUri(context, u)
                if (text.isNotBlank()) gameSettings = text
            }
        }
    }
    val characterFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { u ->
            scope.launch {
                val text = readTextFromUri(context, u)
                if (text.isNotBlank()) characterSettings = text
            }
        }
    }

    // 自动识别（防抖：输入停止800ms后触发）
    LaunchedEffect(gameIntro) {
        if (gameIntro.isBlank()) return@LaunchedEffect
        kotlinx.coroutines.delay(800)
        runRecognize(gameIntro)
    }

    Scaffold(
            topBar = {
                TopAppBar(
                        title = { Text("游戏设定") },
                        navigationIcon = {
                            IconButton(onClick = onGoBack) {
                                Text("←", style = MaterialTheme.typography.titleLarge)
                            }
                        }
                )
            }
    ) { padding ->
        LazyColumn(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                        "游戏介绍（必填）",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedTextField(
                        value = gameIntro,
                        onValueChange = { gameIntro = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 6,
                        placeholder = { Text("粘贴或输入游戏完整介绍/规则文本…") }
                )
                Row {
                    TextButton(onClick = { introFileLauncher.launch("text/plain") }) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("上传txt")
                    }
                }
            }
            item {
                Text(
                        "游戏设定（可不填）",
                        style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                        value = gameSettings,
                        onValueChange = { gameSettings = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        placeholder = { Text("可选：粘贴或输入游戏设定…") }
                )
                Row {
                    TextButton(onClick = { settingsFileLauncher.launch("text/plain") }) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("上传txt")
                    }
                }
            }
            item {
                Text(
                        "人物设定（可不填）",
                        style = MaterialTheme.typography.titleMedium
                )
                OutlinedTextField(
                        value = characterSettings,
                        onValueChange = { characterSettings = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        placeholder = { Text("可选：粘贴或输入人物介绍…") }
                )
                Row {
                    TextButton(onClick = { characterFileLauncher.launch("text/plain") }) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("上传txt")
                    }
                }
            }
            // 难度识别区域
            item {
                if (isRecognizing) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("正在识别游戏难度…", style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (difficultyOptions.isNotEmpty()) {
                    Text(
                            "选择游戏难度",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 4.dp)
                    )
                    difficultyOptions.forEachIndexed { index, option ->
                        val selected = selectedDifficulty == option
                        Surface(
                                onClick = { selectedDifficulty = option },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                        ) {
                            Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                if (selected) {
                                    Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(option, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                                "识别不对?",
                                style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = { runRecognize(gameIntro) }) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("重新识别")
                        }
                    }
                } else if (recognizeFailed) {
                    Text(
                            "读取失败，请手动选择难度",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                    )
                    OutlinedTextField(
                            value = selectedDifficulty,
                            onValueChange = { selectedDifficulty = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            placeholder = { Text("手动输入难度（如：爽玩/沉浸/硬核…）") },
                            singleLine = true
                    )
                } else {
                    Text(
                            "填写游戏介绍后将自动识别难度",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // 下一步按钮
            item {
                Button(
                        onClick = {
                            if (gameIntro.isNotBlank()) {
                                onStartGame(
                                        gameIntro,
                                        gameSettings,
                                        characterSettings,
                                        selectedDifficulty
                                )
                            }
                        },
                        enabled = gameIntro.isNotBlank(),
                        modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("下一步")
                }
            }
        }
    }
}

/**
 * 读取 Uri 指向的文本文件内容。
 */
private fun readTextFromUri(context: android.content.Context, uri: Uri): String {
    return try {
        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
    } catch (e: Exception) {
        ""
    }
}

/**
 * 调用AI识别游戏介绍中的难度选项，返回难度列表（识别失败返回空列表）。
 */
private suspend fun aiRecognizeDifficulty(
        context: android.content.Context,
        introText: String
): List<String> {
    return try {
        val systemPrompt = "你是一个游戏设定解析助手。请从用户提供的游戏介绍文本中，提取出游戏内的难度选项清单。" +
                "难度选项通常是游戏中可供玩家选择的难度等级，例如：爽玩、沉浸、硬核、修罗、天煞、现实等。" +
                "请严格按照以下格式输出，不要输出任何其他内容：\n" +
                "DIFFICULTY_LIST:\n" +
                "- 难度1\n" +
                "- 难度2\n" +
                "...\n" +
                "如果没有找到任何难度选项，请只输出：DIFFICULTY_LIST:"
        val userPrompt = "游戏介绍文本如下：\n$introText\n\n请提取难度选项列表。"

        val multiServiceManager = MultiServiceManager(context)
        val summaryService = multiServiceManager.getServiceForFunction(FunctionType.SUMMARY)
        val modelParameters = multiServiceManager.getModelParametersForFunction(FunctionType.SUMMARY)

        val chatHistory = listOf(
                PromptTurn(kind = PromptTurnKind.SYSTEM, content = systemPrompt)
        )
        val contentBuilder = StringBuilder()
        val stream = summaryService.sendMessage(
                context = context,
                chatHistory = chatHistory + PromptTurn(kind = PromptTurnKind.USER, content = userPrompt),
                modelParameters = modelParameters,
        )
        stream.collect { content -> contentBuilder.append(content) }
        val result = ChatUtils.removeThinkingContent(contentBuilder.toString().trim())

        // 解析 DIFFICULTY_LIST 格式
        parseDifficultyList(result)
    } catch (e: Exception) {
        emptyList()
    }
}

/**
 * 解析AI返回的难度列表。
 */
private fun parseDifficultyList(result: String): List<String> {
    val lines = result.lines()
    val options = mutableListOf<String>()
    var inList = false
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("DIFFICULTY_LIST")) {
            inList = true
            continue
        }
        if (inList) {
            if (trimmed.isEmpty()) continue
            val cleaned = trimmed.removePrefix("-").trim()
            if (cleaned.isNotEmpty()) {
                options.add(cleaned)
            }
        }
    }
    return options
}