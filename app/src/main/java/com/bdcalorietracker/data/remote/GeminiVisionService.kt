package com.bdcalorietracker.data.remote

import android.graphics.Bitmap
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GeminiDetectedPlate(
    val items: List<GeminiDetectedFoodItem>
)

@Serializable
data class GeminiDetectedFoodItem(
    val name_en: String,
    val name_bn: String,
    val estimated_grams: Double,
    val serving_description: String,
    val confidence: Double
)

@Singleton
class GeminiVisionService @Inject constructor() {

    private val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    private val plateSchema = Schema.obj(
        mapOf(
            "items" to Schema.array(
                Schema.obj(
                    mapOf(
                        "name_en" to Schema.string("English name of the Bangladeshi dish"),
                        "name_bn" to Schema.string("Bangla name in Bengali script"),
                        "estimated_grams" to Schema.double("Estimated cooked weight in grams"),
                        "serving_description" to Schema.string("Portion description like '1 cup rice'"),
                        "confidence" to Schema.double("Confidence score between 0.0 and 1.0")
                    )
                )
            )
        )
    )

    private val generativeModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-2.5-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = plateSchema
                temperature = 0.2f
            }
        )
    }

    suspend fun analyzePlatePhoto(bitmap: Bitmap): Result<List<GeminiDetectedFoodItem>> {
        val prompt = """
            You are an expert Bangladeshi nutritionist. Inspect this meal photo carefully.
            Identify every distinct Bangladeshi food item on the plate (bhaat, dal, bhorta, shutki, mach/fish, mangsho/meat, khichuri, biryani, ruti, paratha, bhaji, pitha, mishti, etc.).
            Estimate the cooked edible weight in grams for each item based on standard plate proportions.
            Return strictly valid JSON matching the schema.
        """.trimIndent()

        var lastException: Exception? = null
        repeat(2) { attempt ->
            try {
                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }
                val response = generativeModel.generateContent(inputContent)
                val rawText = response.text ?: throw IllegalStateException("Empty Gemini response")
                val parsed = jsonParser.decodeFromString<GeminiDetectedPlate>(rawText)
                if (parsed.items.isEmpty() && attempt == 0) {
                    throw IllegalStateException("No items detected on attempt 1")
                }
                return Result.success(parsed.items)
            } catch (e: Exception) {
                lastException = e
            }
        }
        return Result.failure(lastException ?: Exception("Failed to recognize food items"))
    }
}
