package com.example.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

data class ChatMessage(
    val id: String = "msg_${System.currentTimeMillis()}_${(100..999).random()}",
    val sender: String, // "user" or "model"
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

object LectureLinkAiService {
    private const val TAG = "LectureLinkAi"

    private val systemInstructionContent: Content by lazy {
        content {
            text(
                "You are LectureLink AI Academic Assistant, a dedicated university assistant helping " +
                "students, course representatives, and lecturers. You assist with lecture timetables, " +
                "clarifying course details, summarizing study tips, organizing schedules, and drafting announcements."
            )
        }
    }

    // Lazy generative models for different complexity tiers
    private val modelFlash by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = systemInstructionContent
        )
    }

    private val modelFast by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.1-flash-lite",
            systemInstruction = systemInstructionContent
        )
    }

    private val modelArgon by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-4-argon",
            systemInstruction = systemInstructionContent
        )
    }

    private val modelTranscribe by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.5-transcribe"
        )
    }

    fun getModel(choice: String) = when (choice) {
        "gemini-3.1-flash-lite" -> modelFast
        "gemini-4-argon" -> modelArgon
        else -> modelFlash
    }

    fun streamChatResponse(
        history: List<ChatMessage>,
        userMessage: String,
        modelChoice: String = "gemini-3.8-flash"
    ): Flow<String> = flow {
        val model = getModel(modelChoice)
        val formattedHistory = history.map { msg ->
            content(if (msg.sender == "user") "user" else "model") {
                text(msg.text)
            }
        }
        val chat = model.startChat(history = formattedHistory)

        withTimeout(60_000L) {
            chat.sendMessageStream(userMessage).collect { chunk ->
                val chunkText = chunk.text.orEmpty()
                if (chunkText.isNotEmpty()) {
                    emit(chunkText)
                }
            }
        }
    }

    suspend fun transcribeAudio(audioBytes: ByteArray, mimeType: String = "audio/mp4"): String {
        return try {
            withTimeout(60_000L) {
                val inputContent = content {
                    inlineData(audioBytes, mimeType)
                    text("Transcribe this spoken lecture recording or voice note accurately into plain text.")
                }
                val response = try {
                    modelTranscribe.generateContent(inputContent)
                } catch (e: Exception) {
                    Log.w(TAG, "gemini-3.5-transcribe fallback to gemini-3.8-flash: ${e.message}")
                    modelFlash.generateContent(inputContent)
                }
                response.text ?: "No transcription text returned."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Audio transcription failed", e)
            throw e
        }
    }
}
