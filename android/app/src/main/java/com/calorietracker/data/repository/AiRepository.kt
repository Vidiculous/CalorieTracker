package com.calorietracker.data.repository

import com.calorietracker.data.model.ChatMessage
import com.calorietracker.data.model.FoodLogEntry
import com.calorietracker.data.remote.GeminiApi
import com.calorietracker.data.remote.dto.*
import com.calorietracker.util.JsonParser
import javax.inject.Inject
import javax.inject.Singleton

data class AiContext(
    val limit: Int,
    val current: Int,
    val proteinCurrent: Float,
    val proteinTarget: Int,
    val carbsCurrent: Float,
    val carbsTarget: Int,
    val fatCurrent: Float,
    val fatTarget: Int,
    val currentLogs: List<FoodLogEntry>,
    val history: List<ChatMessage>
)

@Singleton
class AiRepository @Inject constructor(
    private val api: GeminiApi,
    private val jsonParser: JsonParser
) {
    companion object {
        private const val FALLBACK_MODEL = "gemini-2.0-flash"

        const val SYSTEM_INSTRUCTION = """
You are a smart Nutrition Assistant. Your goal is to analyze food inputs (text or images) and return structured JSON data.

1. **Classify the Intent**:
   - If the user wants to log what they just ate -> "type": "log"
   - If the user provides a recipe to save for later -> "type": "recipe"
   - If the user provides more info about a RECENT log entry, or wants to CORRECT a log -> "type": "update"
   - If the user input is ambiguous or missing critical info -> "type": "clarification"
   - If the user asks a question about their diet, limits, or general nutrition advice -> "type": "conversation"

2. **Schema**:
{
  "type": "log" | "recipe" | "update" | "clarification" | "conversation",
  "status_message": "string (A polite success message in the USER'S LANGUAGE. MANDATORY: Explicitly state the total calories logged, e.g., 'Logged: Pizza (450 kcal)')",
  "update_target_id": "string (the ID of the log entry to update, if type is 'update')",
  "transcription": "string (The verbatim text of what the user said in the audio)",
  "explanation": "string (Briefly explain assumptions in the USER'S LANGUAGE)",
  "meal_type": "Breakfast" | "Lunch" | "Dinner" | "Snacks" | "Other" (required for log),
  "meal_name": "string (optional name if logging a composite meal)",
  "question": "string (if type is clarification, ask why missing info e.g. 'Which meal is this for?')",
  "answer": "string (if type is conversation, provide a helpful answer based on user stats)",
  "items": [
    {
      "food_name": "string (short user friendly name)",
      "calories": number (estimated TOTAL for this item/meal),
      "protein": number (estimated grams),
      "carbs": number (estimated grams),
      "fat": number (estimated grams),
      "quantity_desc": "string (e.g. 1 breast, 200g)",
      "confidence": "high" | "medium" | "low"
    }
  ],
  "recipe_details": {
    "servings": number,
    "prep_time": "string (e.g. 15 mins)",
    "instructions": "string (brief summary)"
  }
}

3. **Rules**:
- **Meal Classification**:
  - Determine "meal_type" ("Breakfast", "Lunch", "Dinner", "Snacks").
  - If user says "for dinner" or "my lunch", use that.
  - If the item is clearly a snack (e.g. "cookie", "chips", "apple", "protein bar"), default to "Snacks".
  - If it is a full meal (e.g. "steak and potatoes") and the user DID NOT specify a meal:
    - Set "type": "clarification".
    - Set "question": "Is this for Lunch or Dinner?".
  - Do NOT assume time of day. If ambiguous, ASK.

- **Ambiguity & Specificity**:
  - If the user says "I ate cereal" or "I had pizza" without specifying how much:
    - Set "type": "clarification".
    - Set "question": "How much did you have? (e.g., 2 slices, 1 large bowl)".
  - If the user provides a moderately specific name but no size (e.g., "Vesuvio"):
    - You MAY log it, but you MUST set "explanation" to state your size assumption (e.g., "Assuming one whole standard pizza").
  - Always provide an "explanation" for every "log" or "recipe" type to build trust.

- **Conversation History**:
  - If user confirms "I'll have one" -> Check history -> If it was a Cookie -> Log "Cookie" -> Set "meal_type": "Snacks" (since cookie is a snack).

- **Recipes**:
  - You MUST know the number of servings. If unknown, use "type": "clarification".
  - **CRITICAL**: Return the TOTAL nutrition and ALL ingredients for the ENTIRE BATCH. Do not divide by servings yourself.

- **Language Matching**:
  - **CRITICAL**: Detect the user's language and respond in the SAME language for all text fields: "status_message", "explanation", "question", "answer", "food_name", and "meal_name".
  - If the user speaks Swedish, EVERYTHING in your JSON text must be Swedish.
  - **DEFAULT**: If the input is too short to detect a language or if you are unsure, default to **English**. Do NOT default to French unless the user explicitly speaks French.

- **Context & History**:
  - Use conversation history to resolve "it", "one", "that".

Return ONLY valid JSON. Start with '{' and end with '}'.
"""
    }

    suspend fun analyzeText(
        apiKey: String,
        modelName: String,
        textInput: String,
        context: AiContext
    ): AiAnalysisResult = execute(apiKey, modelName) { model ->
        val prompt = buildTextPrompt(textInput, context)
        val request = GeminiRequest(
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = SYSTEM_INSTRUCTION))),
            contents = listOf(GeminiContent(role = "user", parts = listOf(GeminiPart(text = prompt))))
        )
        api.generateContent(model, apiKey, request)
    }

    suspend fun analyzeImage(
        apiKey: String,
        modelName: String,
        base64Image: String,
        userText: String?,
        context: AiContext
    ): AiAnalysisResult = execute(apiKey, modelName) { model ->
        val imagePrompt = userText
            ?: "Identify every food item visible in this photo. Estimate portion sizes using visual reference points — plate diameter, utensils, packaging labels, or hand size if visible. List each item separately in the 'items' array with your best calorie and macro estimate. Set confidence to 'low' if the portion is hard to judge."

        val fullPrompt = appendContextToPrompt(imagePrompt, context)
        val request = GeminiRequest(
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = SYSTEM_INSTRUCTION))),
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(
                        GeminiPart(text = fullPrompt),
                        GeminiPart(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                    )
                )
            )
        )
        api.generateContent(model, apiKey, request)
    }

    suspend fun transcribeAudio(
        apiKey: String,
        modelName: String,
        base64Audio: String,
        mimeType: String = "audio/mp4"
    ): String {
        return try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(
                        role = "user",
                        parts = listOf(
                            GeminiPart(text = "Transcribe the spoken audio into text data. Return ONLY the text, no conversational filler or markdown."),
                            GeminiPart(inlineData = InlineData(mimeType = mimeType, data = base64Audio))
                        )
                    )
                )
            )
            val response = api.generateContent(modelName, apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private suspend fun execute(
        apiKey: String,
        modelName: String,
        block: suspend (model: String) -> GeminiResponse
    ): AiAnalysisResult {
        val activeModel = if (modelName == "gemini-1.5-flash") FALLBACK_MODEL else (modelName.ifEmpty { FALLBACK_MODEL })
        return try {
            val response = block(activeModel)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            jsonParser.parseAiResult(rawText)
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 404 && activeModel != FALLBACK_MODEL) {
                execute(apiKey, FALLBACK_MODEL, block)
            } else {
                AiAnalysisResult(error = "AI error: ${e.message()}")
            }
        } catch (e: java.net.UnknownHostException) {
            AiAnalysisResult(error = "No internet connection. Please check your network and try again.")
        } catch (e: java.net.SocketTimeoutException) {
            AiAnalysisResult(error = "Request timed out. Please try again.")
        } catch (e: Exception) {
            AiAnalysisResult(error = "Something went wrong. Please try again.")
        }
    }

    private fun buildTextPrompt(textInput: String, context: AiContext): String {
        val base = "Analyze this text input: \"$textInput\". Estimate calories and protein."
        return appendContextToPrompt(base, context)
    }

    private fun appendContextToPrompt(prompt: String, context: AiContext): String {
        val sb = StringBuilder(prompt)

        if (context.history.isNotEmpty()) {
            sb.append("\n\n[CONVERSATION HISTORY - Most Recent Last]:\n")
            context.history.takeLast(6).forEach { msg ->
                val content = if (msg.content.length > 200) msg.content.take(200) + "..." else msg.content
                sb.append("${msg.role}: $content\n")
            }
        }

        sb.append("\n\n[USER CONTEXT]: \n")
        sb.append("        Calories: ${context.current} / ${context.limit} kcal.\n")
        sb.append("        Macros (Current/Goal): \n")
        sb.append("        - Protein: ${context.proteinCurrent.toInt()} / ${context.proteinTarget} g\n")
        sb.append("        - Carbs: ${context.carbsCurrent.toInt()} / ${context.carbsTarget} g\n")
        sb.append("        - Fat: ${context.fatCurrent.toInt()} / ${context.fatTarget} g")

        if (context.currentLogs.isNotEmpty()) {
            sb.append("\n\n[LOGS ALREADY SAVED TODAY]:\n")
            context.currentLogs.forEach { log ->
                sb.append("- ID: ${log.id}, Name: ${log.foodName}, Calories: ${log.calories}kcal\n")
            }
            sb.append("\nIf the user is providing more details or correcting one of these, use \"type\": \"update\" and specify the exact \"update_target_id\".")
        }

        return sb.toString()
    }
}
