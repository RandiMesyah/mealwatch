package com.ratu.mealwatch

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.QUEUE_FLUSH
import java.util.Locale

/**
 * Mengelola suara peringatan (Text-to-Speech) dengan variasi kalimat
 * supaya tidak monoton, plus cooldown agar tidak spam bicara terus-menerus.
 */
class ReminderManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var lastSpokenAt = 0L
    private val cooldownMs = 8000L // jarak minimal antar peringatan

    private val idlePhrases = listOf(
        "Ayo dikunyah makanannya, sayang.",
        "Jangan berhenti makan ya, ayo dikunyah lagi.",
        "Makanannya masih ada tuh, ayo dikunyah pelan-pelan.",
        "Kunyah dulu makanannya sebelum ngomong ya."
    )

    private val faceLostPhrases = listOf(
        "Duduk yang manis di tempat makan ya.",
        "Ayo kembali duduk untuk makan.",
        "Kamu ke mana? Ayo lanjut makan dulu.",
        "Selesaikan makannya dulu ya, duduk di sini."
    )

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("id", "ID")
                isReady = true
            }
        }
    }

    fun sayIdleReminder() = speakOneOf(idlePhrases)

    fun sayFaceLostReminder() = speakOneOf(faceLostPhrases)

    private fun speakOneOf(phrases: List<String>) {
        val now = System.currentTimeMillis()
        if (!isReady || now - lastSpokenAt < cooldownMs) return
        lastSpokenAt = now
        val phrase = phrases.random()
        tts?.speak(phrase, QUEUE_FLUSH, null, "meal_watch_reminder")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
