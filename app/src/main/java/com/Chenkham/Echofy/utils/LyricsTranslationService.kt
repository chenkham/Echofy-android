package com.Chenkham.Echofy.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.net.URLEncoder

/**
 * Lyrics Translation Service using Google Translate API
 * Based on py-googletrans approach: https://github.com/ssut/py-googletrans
 * 
 * Uses translate.googleapis.com/translate_a/single endpoint with client=gtx
 * which doesn't require authentication tokens.
 */
object LyricsTranslationService {
    
    private const val API_URL = "https://translate.googleapis.com/translate_a/single"
    
    private val client = HttpClient {
        expectSuccess = false
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * Supported languages for translation (compact list for UI)
     */
    val supportedLanguages = listOf(
        "en" to "English",
        "hi" to "Hindi",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "ja" to "Japanese",
        "ko" to "Korean",
        "zh-CN" to "Chinese",
        "ar" to "Arabic",
        "ru" to "Russian",
        "pt" to "Portuguese",
        "it" to "Italian",
        "ta" to "Tamil",
        "te" to "Telugu"
    )
    
    /**
     * Translate text using Google Translate API
     */
    suspend fun translate(
        text: String,
        targetLang: String,
        sourceLang: String = "auto"
    ): Result<String> = runCatching {
        if (text.isBlank()) return@runCatching text
        
        Timber.d("Starting translation to $targetLang, text length: ${text.length}")
        
        // Split text into chunks if too long
        val chunks = splitTextIntoChunks(text, 4000)
        val translatedChunks = mutableListOf<String>()
        
        for ((index, chunk) in chunks.withIndex()) {
            Timber.d("Translating chunk ${index + 1}/${chunks.size}")
            val translatedChunk = translateChunk(chunk, targetLang, sourceLang)
            translatedChunks.add(translatedChunk)
        }
        
        translatedChunks.joinToString("\n")
    }
    
    /**
     * Translate a single chunk of text
     */
    private suspend fun translateChunk(
        text: String,
        targetLang: String,
        sourceLang: String
    ): String {
        val sl = if (sourceLang == "auto") "auto" else sourceLang
        val encodedText = URLEncoder.encode(text, "UTF-8")
        
        // Build URL with parameters
        val url = "$API_URL?client=gtx&sl=$sl&tl=$targetLang&dt=t&q=$encodedText"
        
        Timber.d("Request URL: ${url.take(200)}...")
        
        try {
            val responseText = client.get(url) {
                headers {
                    append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    append("Accept", "*/*")
                }
            }.body<String>()
            
            Timber.d("Response received, length: ${responseText.length}")
            Timber.d("Response preview: ${responseText.take(500)}")
            
            return parseTranslationResponse(responseText)
        } catch (e: Exception) {
            Timber.e(e, "Translation request failed")
            throw Exception("Translation request failed: ${e.message}")
        }
    }
    
    /**
     * Parse Google Translate API response
     * Response format: [[["translated","original",null,null,10],...],null,"en",...]
     */
    private fun parseTranslationResponse(responseText: String): String {
        try {
            val jsonElement = json.parseToJsonElement(responseText)
            val rootArray = jsonElement.jsonArray
            
            if (rootArray.isEmpty()) {
                throw Exception("Empty response from translation API")
            }
            
            // First element contains translation segments
            val translationSegments = rootArray[0]
            
            if (translationSegments !is JsonArray) {
                throw Exception("Unexpected response format")
            }
            
            // Concatenate all translated segments
            val translatedText = StringBuilder()
            for (segment in translationSegments) {
                if (segment is JsonArray && segment.isNotEmpty()) {
                    val firstElement = segment[0]
                    if (firstElement is JsonElement) {
                        try {
                            val translatedPart = firstElement.jsonPrimitive.content
                            translatedText.append(translatedPart)
                        } catch (e: Exception) {
                            Timber.w("Skipping non-string segment")
                        }
                    }
                }
            }
            
            val result = translatedText.toString()
            if (result.isBlank()) {
                throw Exception("No translation found in response")
            }
            
            Timber.d("Translation result: ${result.take(200)}...")
            return result
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse response: ${responseText.take(200)}")
            throw Exception("Failed to parse translation: ${e.message}")
        }
    }
    
    /**
     * Split text into smaller chunks preserving line breaks
     */
    private fun splitTextIntoChunks(text: String, maxLength: Int): List<String> {
        if (text.length <= maxLength) return listOf(text)
        
        val chunks = mutableListOf<String>()
        val lines = text.split("\n")
        var currentChunk = StringBuilder()
        
        for (line in lines) {
            if (currentChunk.length + line.length + 1 <= maxLength) {
                if (currentChunk.isNotEmpty()) currentChunk.append("\n")
                currentChunk.append(line)
            } else {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                    currentChunk = StringBuilder()
                }
                if (line.length > maxLength) {
                    var remaining = line
                    while (remaining.length > maxLength) {
                        chunks.add(remaining.take(maxLength))
                        remaining = remaining.drop(maxLength)
                    }
                    if (remaining.isNotEmpty()) {
                        currentChunk.append(remaining)
                    }
                } else {
                    currentChunk.append(line)
                }
            }
        }
        
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }
        
        return chunks.ifEmpty { listOf(text.take(maxLength)) }
    }
    
    /**
     * Translate lyrics preserving line structure
     */
    suspend fun translateLyrics(
        lyrics: List<String>,
        targetLang: String,
        sourceLang: String = "auto"
    ): Result<List<String>> = runCatching {
        val combinedText = lyrics.joinToString("\n")
        val translatedText = translate(combinedText, targetLang, sourceLang).getOrThrow()
        translatedText.split("\n")
    }
    
    /**
     * Transliterate/Romanize text - Convert non-Latin scripts to English letters
     * Keeps the original pronunciation (like Hinglish, Romaji, etc.)
     * 
     * Examples:
     * - Hindi: मैं तुमसे प्यार करता हूँ → "main tumse pyaar karta hoon"
     * - Japanese: こんにちは → "konnichiwa"
     * - Korean: 안녕하세요 → "annyeonghaseyo"
     */
    suspend fun transliterate(
        text: String,
        sourceLang: String = "auto"
    ): Result<String> = runCatching {
        if (text.isBlank()) return@runCatching text
        
        Timber.d("Starting transliteration, text length: ${text.length}")
        
        val chunks = splitTextIntoChunks(text, 4000)
        val transliteratedChunks = mutableListOf<String>()
        
        for ((index, chunk) in chunks.withIndex()) {
            Timber.d("Transliterating chunk ${index + 1}/${chunks.size}")
            val transliteratedChunk = transliterateChunk(chunk, sourceLang)
            transliteratedChunks.add(transliteratedChunk)
        }
        
        transliteratedChunks.joinToString("\n")
    }
    
    /**
     * Transliterate each line individually to preserve line-by-line structure
     * Returns a map of line index to romanized text
     */
    suspend fun transliterateLines(
        lines: List<String>,
        sourceLang: String = "auto"
    ): Result<Map<Int, String>> = runCatching {
        val result = mutableMapOf<Int, String>()
        
        for ((index, line) in lines.withIndex()) {
            if (line.isBlank()) {
                result[index] = ""
                continue
            }
            
            try {
                val romanized = transliterateChunk(line, sourceLang)
                result[index] = romanized
            } catch (e: Exception) {
                Timber.w("Failed to transliterate line $index: ${e.message}")
                result[index] = line // Keep original if failed
            }
        }
        
        result
    }

    
    /**
     * Transliterate a single chunk
     */
    private suspend fun transliterateChunk(
        text: String,
        sourceLang: String
    ): String {
        val sl = if (sourceLang == "auto") "auto" else sourceLang
        val encodedText = URLEncoder.encode(text, "UTF-8")
        
        // Use dt=rm for romanization/transliteration, dt=t for translation text
        // tl=en to get English transliteration
        val url = "$API_URL?client=gtx&sl=$sl&tl=en&dt=rm&dt=t&q=$encodedText"
        
        Timber.d("Transliteration URL: ${url.take(200)}...")
        
        try {
            val responseText = client.get(url) {
                headers {
                    append("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    append("Accept", "*/*")
                }
            }.body<String>()
            
            Timber.d("Transliteration response: ${responseText.take(500)}")
            
            return parseTransliterationResponse(responseText, text)
        } catch (e: Exception) {
            Timber.e(e, "Transliteration request failed")
            throw Exception("Transliteration failed: ${e.message}")
        }
    }
    
    /**
     * Parse transliteration response
     * Response format includes romanization at index [0][1][3] or similar positions
     */
    private fun parseTransliterationResponse(responseText: String, originalText: String): String {
        try {
            val jsonElement = json.parseToJsonElement(responseText)
            val rootArray = jsonElement.jsonArray
            
            if (rootArray.isEmpty()) {
                return originalText
            }
            
            // Try to extract romanization from the response
            // Google Translate returns romanization in various positions depending on the response
            val translationSegments = rootArray[0]
            
            if (translationSegments !is JsonArray) {
                return originalText
            }
            
            val romanizedText = StringBuilder()
            
            for (segment in translationSegments) {
                if (segment is JsonArray && segment.size > 3) {
                    // Position [3] contains the romanization/pronunciation
                    try {
                        val element = segment[3]
                        // Check if the element is not null and not a literal "null" string
                        if (element.toString() != "null") {
                            val romanizedPart = element.jsonPrimitive.content
                            if (romanizedPart.isNotBlank() && romanizedPart != "null") {
                                romanizedText.append(romanizedPart)
                            }
                        }
                    } catch (e: Exception) {
                        Timber.w("No romanization found in segment")
                    }
                }
                // NOTE: Removed fallback to position [0] (English translation)
                // If no romanization is available, we keep original text instead of showing English
            }
            
            val result = romanizedText.toString()
            
            // If we got a result, return it; otherwise return original
            return if (result.isNotBlank() && result != "null") {
                Timber.d("Romanization result: ${result.take(200)}")
                result
            } else {
                Timber.w("No romanization found, returning original")
                originalText
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to parse transliteration response")
            return originalText
        }
    }
    
    /**
     * Check if text is primarily in Latin script
     */
    private fun isLatinScript(text: String): Boolean {
        val latinCount = text.count { it in 'A'..'Z' || it in 'a'..'z' || it.isWhitespace() || it in ".,!?'-" }
        return latinCount > text.length * 0.5
    }
}

