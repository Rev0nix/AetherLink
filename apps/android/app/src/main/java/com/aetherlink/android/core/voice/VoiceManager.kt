package com.aetherlink.android.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

class VoiceManager(
    private val context: Context,
    private val onPartial: (String) -> Unit = {},
    private val onFinalResult: (String) -> Unit = {}
) {
    companion object {
        private const val TAG = "VoiceManager"
    }

    var isListening by mutableStateOf(false)
        private set

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                        }

                        override fun onBeginningOfSpeech() {
                            isListening = true
                        }

                        override fun onRmsChanged(rmsdB: Float) {}

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            isListening = false
                        }

                        override fun onError(error: Int) {
                            isListening = false
                            val message = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out"
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                                SpeechRecognizer.ERROR_NETWORK -> "Network required for voice search"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                                else -> null
                            }
                            if (message != null) {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            isListening = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()
                            if (!text.isNullOrBlank()) {
                                onFinalResult(text)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull()
                            if (!text.isNullOrBlank()) {
                                onPartial(text)
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing SpeechRecognizer", e)
        }
    }

    fun startListening() {
        if (isListening) {
            stopListening()
            return
        }

        if (speechRecognizer == null) {
            initRecognizer()
        }

        if (speechRecognizer != null) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }
                speechRecognizer?.startListening(intent)
                isListening = true
                Toast.makeText(context, "Listening... speak now", Toast.LENGTH_SHORT).show()
                return
            } catch (e: Exception) {
                Log.e(TAG, "startListening error", e)
            }
        }

        Toast.makeText(context, "Speech recognition is not available on this device", Toast.LENGTH_LONG).show()
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        isListening = false
    }

    fun destroy() {
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        isListening = false
    }
}
