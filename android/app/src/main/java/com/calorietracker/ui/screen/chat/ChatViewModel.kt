package com.calorietracker.ui.screen.chat

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calorietracker.data.model.ChatMessage
import com.calorietracker.data.model.FoodItem
import com.calorietracker.data.model.FoodLogEntry
import com.calorietracker.data.model.Recipe
import com.calorietracker.data.preferences.AppSettings
import com.calorietracker.data.preferences.SettingsDataStore
import com.calorietracker.data.remote.dto.AiAnalysisResult
import com.calorietracker.data.remote.dto.AiFoodItem
import com.calorietracker.data.repository.*
import com.calorietracker.util.DateUtils
import com.calorietracker.util.NutritionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = listOf(welcomeMessage()),
    val isLoading: Boolean = false,
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val audioLevel: Float = 0f,
    val partialTranscript: String = "",
    val pendingImageBase64: String? = null,
    val pendingConfirmItems: List<AiFoodItem>? = null,
    val pendingConfirmMealType: String = "",
    val settings: AppSettings = AppSettings(),
    val todayLogs: List<FoodLogEntry> = emptyList()
)

private fun welcomeMessage() = ChatMessage(
    id = "welcome",
    role = "ai",
    content = "Hello! I'm your nutrition assistant. Tell me what you ate, share a photo, or ask me anything about your diet!"
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val foodLogRepository: FoodLogRepository,
    private val recipeRepository: RecipeRepository,
    private val chatRepository: ChatRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        viewModelScope.launch {
            combine(
                settingsDataStore.settings,
                foodLogRepository.getLogsForDate(DateUtils.startOfDay(System.currentTimeMillis())),
                chatRepository.getAllMessages()
            ) { settings, todayLogs, chatMessages ->
                Triple(settings, todayLogs, chatMessages)
            }.collect { (settings, todayLogs, chatMessages) ->
                _uiState.update { state ->
                    state.copy(
                        settings = settings,
                        todayLogs = todayLogs,
                        messages = if (chatMessages.isEmpty()) listOf(welcomeMessage())
                        else chatMessages
                    )
                }
            }
        }
    }

    fun sendText(text: String, imageBase64: String? = null) {
        val s = _uiState.value.settings
        if (s.apiKey.isBlank()) {
            appendAiMessage("Please add your Gemini API key in Settings.")
            return
        }

        val userMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = "user",
            content = text,
            imageBase64 = imageBase64
        )
        insertMessage(userMsg)
        _uiState.update { it.copy(isLoading = true, pendingImageBase64 = null) }

        viewModelScope.launch {
            try {
                val context = buildAiContext()
                val result = if (imageBase64 != null) {
                    aiRepository.analyzeImage(s.apiKey, s.selectedModel, imageBase64, text.ifBlank { null }, context)
                } else {
                    aiRepository.analyzeText(s.apiKey, s.selectedModel, text, context)
                }
                handleAiResponse(result)
            } catch (e: Exception) {
                appendAiMessage("Sorry, something went wrong. Please try again.")
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun handleAiResponse(result: AiAnalysisResult) {
        if (result.error != null) {
            appendAiMessage("Error: ${result.error}")
            return
        }

        when (result.type) {
            "log" -> {
                val items = result.items ?: emptyList()
                val mealType = result.mealType ?: DateUtils.inferMealType(System.currentTimeMillis())
                val needsConfirm = items.size > 1 || items.any { it.confidence != "high" }

                if (needsConfirm) {
                    _uiState.update { it.copy(pendingConfirmItems = items, pendingConfirmMealType = mealType) }
                } else {
                    items.forEach { logAiItem(it, mealType) }
                    appendAiMessage(result.statusMessage ?: "Logged successfully!")
                }
            }
            "recipe" -> {
                val items = result.items ?: emptyList()
                val details = result.recipeDetails
                val totalCal = items.sumOf { it.calories }.toFloat()
                val recipe = Recipe(
                    id = UUID.randomUUID().toString(),
                    name = result.mealName ?: "Recipe",
                    calories = totalCal,
                    protein = items.sumOf { it.protein.toDouble() }.toFloat(),
                    carbs = items.sumOf { it.carbs.toDouble() }.toFloat(),
                    fat = items.sumOf { it.fat.toDouble() }.toFloat(),
                    servings = details?.servings ?: 1f,
                    description = details?.instructions,
                    items = items.map { it.toFoodItem() },
                    createdAt = System.currentTimeMillis()
                )
                viewModelScope.launch { recipeRepository.insert(recipe) }
                appendAiMessage(result.statusMessage ?: "Recipe saved!")
            }
            "update" -> {
                val targetId = result.updateTargetId
                val items = result.items
                if (targetId != null && !items.isNullOrEmpty()) {
                    val item = items.first()
                    viewModelScope.launch {
                        val existing = foodLogRepository.getById(targetId)
                        existing?.let {
                            foodLogRepository.update(
                                it.copy(
                                    calories = item.calories,
                                    protein = item.protein,
                                    carbs = item.carbs,
                                    fat = item.fat,
                                    quantity = item.quantityDesc
                                )
                            )
                        }
                    }
                }
                appendAiMessage(result.statusMessage ?: "Updated!")
            }
            "clarification" -> appendAiMessage(result.question ?: "Could you clarify?")
            "conversation" -> appendAiMessage(result.answer ?: result.statusMessage ?: "")
            else -> appendAiMessage(result.statusMessage ?: "")
        }
    }

    fun confirmLog(items: List<AiFoodItem>) {
        val mealType = _uiState.value.pendingConfirmMealType
        items.forEach { logAiItem(it, mealType) }
        _uiState.update { it.copy(pendingConfirmItems = null, pendingConfirmMealType = "") }
        appendAiMessage("Logged ${items.size} item${if (items.size != 1) "s" else ""}! Total: ${items.sumOf { it.calories }} kcal")
    }

    fun discardConfirm() {
        _uiState.update { it.copy(pendingConfirmItems = null, pendingConfirmMealType = "") }
    }

    fun setPendingImage(base64: String) {
        _uiState.update { it.copy(pendingImageBase64 = base64) }
    }

    fun clearPendingImage() {
        _uiState.update { it.copy(pendingImageBase64 = null) }
    }

    fun clearChat() {
        viewModelScope.launch {
            chatRepository.clearAll()
            _uiState.update { it.copy(messages = listOf(welcomeMessage())) }
        }
    }

    fun startRecording(context: Context) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            appendAiMessage("Speech recognition is not available on this device.")
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _uiState.update { it.copy(isRecording = true, partialTranscript = "") }
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {
                    val level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    _uiState.update { it.copy(audioLevel = level) }
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _uiState.update { it.copy(isRecording = false, isTranscribing = true, audioLevel = 0f) }
                }
                override fun onError(error: Int) {
                    _uiState.update { it.copy(isRecording = false, isTranscribing = false, audioLevel = 0f, partialTranscript = "") }
                }
                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    _uiState.update { it.copy(isRecording = false, isTranscribing = false, audioLevel = 0f, partialTranscript = "") }
                    if (text.isNotBlank()) {
                        if (_uiState.value.settings.autoSubmit) {
                            sendText(text)
                        } else {
                            _uiState.update {
                                it.copy(messages = it.messages + ChatMessage(
                                    id = UUID.randomUUID().toString(),
                                    role = "transcript",
                                    content = text
                                ))
                            }
                        }
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                    if (partial.isNotBlank()) {
                        _uiState.update { it.copy(partialTranscript = partial) }
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopRecording() {
        speechRecognizer?.stopListening()
        _uiState.update { it.copy(isRecording = false, isTranscribing = false, audioLevel = 0f, partialTranscript = "") }
    }

    private fun logAiItem(item: AiFoodItem, mealType: String) {
        viewModelScope.launch {
            foodLogRepository.insert(
                FoodLogEntry(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    foodName = item.foodName,
                    calories = item.calories,
                    protein = item.protein,
                    carbs = item.carbs,
                    fat = item.fat,
                    quantity = item.quantityDesc,
                    source = "ai",
                    mealType = mealType
                )
            )
        }
    }

    private fun appendAiMessage(content: String) {
        val msg = ChatMessage(id = UUID.randomUUID().toString(), role = "ai", content = content)
        insertMessage(msg)
    }

    private fun insertMessage(msg: ChatMessage) {
        viewModelScope.launch { chatRepository.insert(msg) }
        _uiState.update { it.copy(messages = it.messages + msg) }
    }

    private fun buildAiContext(): AiContext {
        val state = _uiState.value
        val totals = NutritionUtils.getTotals(state.todayLogs)
        return AiContext(
            limit = state.settings.dailyGoal,
            current = totals.calories,
            proteinCurrent = totals.protein,
            proteinTarget = state.settings.proteinGoal,
            carbsCurrent = totals.carbs,
            carbsTarget = state.settings.carbsGoal,
            fatCurrent = totals.fat,
            fatTarget = state.settings.fatGoal,
            currentLogs = state.todayLogs,
            history = state.messages.takeLast(6)
        )
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}

private fun AiFoodItem.toFoodItem() = FoodItem(
    id = UUID.randomUUID().toString(),
    foodName = foodName,
    calories = calories,
    protein = protein,
    carbs = carbs,
    fat = fat,
    quantityDesc = quantityDesc,
    confidence = confidence
)
