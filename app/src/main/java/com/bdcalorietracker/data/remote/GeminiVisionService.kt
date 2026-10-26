package com.bdcalorietracker.data.remote

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
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
    val confidence: Double = 0.7
)

@Singleton
class GeminiVisionService @Inject constructor() {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun buildModel(modelName: String) =
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

        val modelsToTry = listOf(
            "gemini-3.8-flash",
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-3.1-flash-lite"
        )

        val errors = mutableListOf<String>()

        for (modelName in modelsToTry) {
            try {
                return withTimeout(35_000L) {
                    val inputContent = content {
                        image(bitmap)
                        text(prompt)
                    }
                    val response = buildModel(modelName).generateContent(inputContent)
                    val rawText = response.text
                        ?.trim()
                        ?.removePrefix("```json")
                        ?.removePrefix("```")
                        ?.removeSuffix("```")
                        ?.trim()
                        ?: throw IllegalStateException("Empty response text")

                    // Extract JSON object even if extra text surrounds it
                    val jsonStart = rawText.indexOf('{')
                    val jsonEnd = rawText.lastIndexOf('}')
                    val cleanJson = if (jsonStart != -1 && jsonEnd > jsonStart) {
                        rawText.substring(jsonStart, jsonEnd + 1)
                    } else {
                        rawText
                    }

                    val parsed = jsonParser.decodeFromString<GeminiDetectedPlate>(cleanJson)
                    if (parsed.items.isEmpty()) {
                        throw IllegalStateException("0 items detected")
                    }
                    Result.success(parsed.items)
                }
            } catch (e: Exception) {
                // Unwrap the real cause hidden inside Firebase's "Something unexpected happened."
                val rootCause = e.cause?.localizedMessage ?: e.localizedMessage ?: e.javaClass.simpleName
                errors.add("$modelName -> $rootCause")
            }
        }

        return Result.failure(Exception(errors.joinToString("\n")))
    }
}
