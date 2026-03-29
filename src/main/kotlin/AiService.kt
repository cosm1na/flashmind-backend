package com.example

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.InputStream
import java.util.Properties

object AiService {

    private val API_KEY: String = run {
        val properties = Properties()
        val file = File("local.properties")
        if (file.exists()) {
            file.inputStream().use { properties.load(it) }
        }
        properties.getProperty("GEMINI_API_KEY")
            ?: throw IllegalStateException("Eroare CRITICA: Nu am gasit GEMINI_API_KEY in fisierul local.properties!")
    }

    private val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$API_KEY"

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    fun extractTextFromPdf(inputStream: InputStream): String {
        return try {
            PDDocument.load(inputStream).use { document ->
                val stripper = PDFTextStripper()
                stripper.getText(document)
            }
        } catch (e: Exception) {
            println("Eroare PDF: ${e.message}")
            ""
        }
    }

    suspend fun generateFlashcards(text: String): List<AiFlashcard> {
        if (text.isBlank()) return emptyList()

        val prompt = """
            Creează flashcards din textul de mai jos. 
            Răspunde DOAR cu un array JSON: [{"fata": "întrebare", "spate": "răspuns"}].
            Text: $text
        """.trimIndent()

        val requestBody = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt))))
        )

        return try {
            val response: HttpResponse = client.post(GEMINI_URL) {
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }

            val responseBody = response.bodyAsText()
            println("--- RĂSPUNS COMPLET DE LA GOOGLE AI ---")
            println(responseBody)
            println("---------------------------------------")

            val geminiResponse = Json { ignoreUnknownKeys = true }.decodeFromString<GeminiResponse>(responseBody)

            val rawText = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""

            if (rawText.isBlank()) {
                println("Eroare: Răspunsul text de la AI a fost complet gol!")
                return emptyList()
            }

            val cleanJson = rawText.replace("```json", "").replace("```", "").trim()
            println("JSON pregătit pentru parsare: $cleanJson")

            Json { ignoreUnknownKeys = true }.decodeFromString<List<AiFlashcard>>(cleanJson)

        } catch (e: Exception) {
            println("Eroare AI: ${e.message}")
            emptyList()
        }
    }
}