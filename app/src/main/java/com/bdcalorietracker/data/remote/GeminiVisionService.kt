package com.bdcalorietracker.data.remote

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GeminiDetectedPlate(
    val items: List<GeminiDetectedFoodItem> = emptyList()
)

@Serializable
data class GeminiDetectedFoodItem(
    val name_en: String = "Unknown Food",
    val name_bn: String = "অজানা খাবার",
    val estimated_grams: Double = 100.0,
    val serving_description: String = "1 serving",
    val confidence: Double = 0.8
)

@Singleton
class GeminiVisionService @Inject constructor() {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun getModel(modelName: String) =
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = modelName,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            }
        )

    suspend fun analyzePlatePhoto(bitmap: Bitmap): Result<List<GeminiDetectedFoodItem>> {
        val prompt = """
            Identify all Bangladeshi food items in this photo.
            Return ONLY valid JSON in this exact format:
            {
              "items": [
                {
                  "name_en": "Steamed White Rice (Parboiled)",
                  "name_bn": "সিদ্ধ ভাত",
                  "estimated_grams": 200.0,
                  "serving_description": "1 cup",
                  "confidence": 0.9
                }
              ]
            }
        """.trimIndent()

        // Ordered from fastest/lightest prefill queue to heaviest:
        val modelsToTry = listOf(
            "gemini-3.5-flash-lite",
            "gemini-3.7-flash",
            "gemini-3.8-flash"
        )

        val errors = mutableListOf<String>()

        for (modelName in modelsToTry) {
            try {
                return withTimeout(60_000L) {
                    val inputContent = content {
                        image(bitmap)
                        text(prompt)
                    }
                    val response = getModel(modelName).generateContent(inputContent)
                    val rawText = response.text
                        ?.trim()
                        ?.removePrefix("```json")
                        ?.removePrefix("```")
                        ?.removeSuffix("```")
                        ?.trim()
                        ?: throw IllegalStateException("Empty response from AI")

                    val jsonStart = rawText.indexOf('{')
                    val jsonEnd = rawText.lastIndexOf('}')
                    val cleanJson = if (jsonStart != -1 && jsonEnd > jsonStart) {
                        rawText.substring(jsonStart, jsonEnd + 1)
                    } else {
                        rawText
                    }

                    val parsed = jsonParser.decodeFromString<GeminiDetectedPlate>(cleanJson)
                    if (parsed.items.isEmpty()) {
                        throw IllegalStateException("No food items detected")
                    }
                    Result.success(parsed.items)
                }
            } catch (e: Exception) {
                val rootCause = e.cause?.localizedMessage ?: e.localizedMessage ?: e.javaClass.simpleName
                errors.add("$modelName: $rootCause")
                // Brief pause before trying the next model queue
                delay(1500L)
            }
        }

        return Result.failure(Exception(errors.joinToString("\n")))
    }
}
