
import { GoogleGenerativeAI } from "@google/generative-ai";

// We'll allow passing the key dynamically or from env
const getGenAI = (apiKey) => new GoogleGenerativeAI(apiKey);

export const SYSTEM_INSTRUCTION = `
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
`;

const appendContextToPrompt = (prompt, context) => {
    let newPrompt = prompt;

    // History
    if (context && context.history && context.history.length > 0) {
        newPrompt += `\n\n[CONVERSATION HISTORY - Most Recent Last]:\n`;
        context.history.forEach(msg => {
            // Simplify for token efficiency
            const content = msg.content.length > 200 ? msg.content.substring(0, 200) + '...' : msg.content;
            newPrompt += `${msg.role}: ${content}\n`;
        });
    }

    // Macros/Stats
    if (context && context.limit) {
        newPrompt += `\n\n[USER CONTEXT]: 
        Calories: ${context.current} / ${context.limit} kcal.
        Macros (Current/Goal): 
        - Protein: ${context.macros?.protein?.current || 0} / ${context.macros?.protein?.target || 0} g
        - Carbs: ${context.macros?.carbs?.current || 0} / ${context.macros?.carbs?.target || 0} g
        - Fat: ${context.macros?.fat?.current || 0} / ${context.macros?.fat?.target || 0} g`;
    }

    // Current Day's Logs
    if (context && context.currentLogs && context.currentLogs.length > 0) {
        newPrompt += `\n\n[LOGS ALREADY SAVED TODAY]:\n`;
        context.currentLogs.forEach(log => {
            newPrompt += `- ID: ${log.id}, Name: ${log.name}, Calories: ${log.calories}kcal\n`;
        });
        newPrompt += `\nIf the user is providing more details or correcting one of these, use "type": "update" and specify the exact "update_target_id".`;
    }

    return newPrompt;
};

export const analyzeImage = async (apiKey, modelName, input, context = {}) => {
    try {
        const genAI = getGenAI(apiKey);
        const model = genAI.getGenerativeModel({
            model: modelName || "gemini-2.0-flash-exp",
            systemInstruction: SYSTEM_INSTRUCTION
        });

        const isObject = typeof input === 'object' && input.image;
        const base64Image = isObject ? input.image : input;
        const userPrompt = isObject && input.text
            ? input.text
            : "Identify every food item visible in this photo. " +
              "Estimate portion sizes using visual reference points — plate diameter, utensils, " +
              "packaging labels, or hand size if visible. " +
              "List each item separately in the 'items' array with your best calorie and macro estimate. " +
              "Set confidence to 'low' if the portion is hard to judge.";

        const imagePart = {
            inlineData: {
                data: base64Image.split(',')[1],
                mimeType: "image/jpeg"
            },
        };

        let finalPrompt = userPrompt;
        finalPrompt = appendContextToPrompt(finalPrompt, context);

        const inputs = [finalPrompt, imagePart];

        const result = await model.generateContent(inputs);
        const response = await result.response;
        const text = response.text();
        return JSON.parse(cleanJson(text));
    } catch (error) {
        if (error.message.includes('404') && !modelName?.includes('gemini-2.0-flash-exp')) {
            console.warn(`Model ${modelName} not found. Retrying with fallback: gemini-2.0-flash-exp`);
            return analyzeImage(apiKey, 'gemini-2.0-flash-exp', input, context);
        }
        console.error("AI Image Error:", error);
        return { error: "Failed to process image. Please check your API key or try again." };
    }
};

export const transcribeAudio = async (apiKey, modelName, audioData, mimeType = 'audio/webm') => {
    try {
        const genAI = getGenAI(apiKey);
        const model = genAI.getGenerativeModel({
            model: modelName || "gemini-2.0-flash-exp",
        });

        const audioPart = {
            inlineData: {
                data: audioData.split(',')[1],
                mimeType: mimeType
            },
        };

        const result = await model.generateContent([
            "Transcribe the spoken audio into text data. Return ONLY the text, no conversational filler or markdown.",
            audioPart
        ]);
        const response = await result.response;
        return { text: response.text().trim() };
    } catch (error) {
        console.error("AI Transcription Error:", error);
        return { error: "Failed to transcribe audio." };
    }
};

const fetchUrlContent = async (url) => {
    try {
        // Try using a CORS proxy. 
        const proxyUrl = `https://corsproxy.io/?${encodeURIComponent(url)}`;
        const response = await fetch(proxyUrl);

        if (!response.ok) return null;

        const html = await response.text();

        // Basic HTML cleanup
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');

        // Remove scripts/styles
        const scripts = doc.querySelectorAll('script, style, nav, footer, header, iframe');
        scripts.forEach(script => script.remove());

        const text = doc.body.textContent || "";
        return text.replace(/\s+/g, ' ').trim().substring(0, 15000);
    } catch (error) {
        return null; // Silent fail
    }
};

export const analyzeText = async (apiKey, modelName, textInput, context = {}) => {
    try {
        const genAI = getGenAI(apiKey);
        // Force valid model if somehow invalid
        const activeModel = (modelName === 'gemini-1.5-flash') ? "gemini-2.0-flash-exp" : (modelName || "gemini-2.0-flash-exp");

        const model = genAI.getGenerativeModel({
            model: activeModel,
            systemInstruction: SYSTEM_INSTRUCTION
        });

        let promptText = `Analyze this text input: "${textInput}". Estimate calories and protein.`;

        if (textInput.trim().startsWith('http')) {
            const urlContent = await fetchUrlContent(textInput.trim());
            if (urlContent) {
                promptText = `Analyze this recipe/page content from ${textInput}: \n\n ${urlContent}`;
            }
        }

        promptText = appendContextToPrompt(promptText, context);

        const result = await model.generateContent([promptText]);
        const response = await result.response;
        const text = response.text();
        return JSON.parse(cleanJson(text));
    } catch (error) {
        // simplified error handling
        console.error("AI Text Error:", error);
        return { error: "Failed to process text. Please check your API key or try again." };
    }
};

// Helper to remove markdown wrapping if AI adds it despite instructions
const cleanJson = (text) => {
    const raw = text.replace(/```json/g, '').replace(/```/g, '').trim();
    // Sometimes it might wrap in extra newlines
    const firstBrace = raw.indexOf('{');
    const lastBrace = raw.lastIndexOf('}');
    if (firstBrace !== -1 && lastBrace !== -1) {
        return raw.substring(firstBrace, lastBrace + 1);
    }
    return raw;
};

export const listAvailableModels = async (apiKey) => {
    try {
        const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}`);
        const data = await response.json();
        console.log("=== AVAILABLE MODELS ===", data);
        return data;
    } catch (error) {
        console.error("Failed to list models:", error);
    }
};
