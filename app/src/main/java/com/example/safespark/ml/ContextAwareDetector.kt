package com.example.safespark.ml

import android.util.LruCache
import java.util.*

/**
 * Context-Aware Grooming Detector
 * Basierend auf Springer Paper 2024 (ISBN 978-3-031-62083-6)
 *
 * Features:
 * - Conversation History Tracking
 * - Stage Progression Detection
 * - Temporal Risk Analysis
 * - Emoji Risk Detection
 * - Social Engineering Detection
 */
class ContextAwareDetector {

    companion object {
        private const val MAX_HISTORY_SIZE = 10
        private const val CONTEXT_WINDOW_MS = 30 * 60 * 1000L // 30 Minuten
    }

    // LRU Cache für Conversation History pro App
    private val conversationCache = LruCache<String, ConversationHistory>(50)

    data class Message(
        val text: String,
        val timestamp: Long,
        val stage: String? = null,
        val score: Float = 0f
    )

    data class ConversationHistory(
        val messages: MutableList<Message> = mutableListOf(),
        var startTime: Long = System.currentTimeMillis()
    )

    /**
     * Analysiert Message mit Kontext
     */
    fun analyzeWithContext(
        appPackage: String,
        text: String,
        baseScore: Float,
        baseStage: String,
        timestamp: Long = System.currentTimeMillis()
    ): ContextualResult {

        // Hole oder erstelle History
        val history = conversationCache.get(appPackage) ?: ConversationHistory()

        // Füge neue Message hinzu
        val message = Message(text, timestamp, baseStage, baseScore)
        history.messages.add(message)

        // Cleanup alte Messages (> 30 Min)
        cleanupOldMessages(history, timestamp)

        // Behalte nur letzte N Messages
        if (history.messages.size > MAX_HISTORY_SIZE) {
            history.messages.removeAt(0)
        }

        // Speichere zurück
        conversationCache.put(appPackage, history)

        // Analysiere Kontext
        val progressionScore = detectStageProgression(history)
        val conversationDuration = timestamp - history.startTime
        val messageFrequency = calculateMessageFrequency(history, timestamp)

        // Berechne Context Bonus
        val contextBonus = calculateContextBonus(
            progressionScore,
            conversationDuration,
            messageFrequency
        )

        // Finaler Score
        val finalScore = minOf(baseScore + contextBonus, 1.0f)

        return ContextualResult(
            score = finalScore,
            baseScore = baseScore,
            contextBonus = contextBonus,
            progressionScore = progressionScore,
            conversationDuration = conversationDuration,
            messageCount = history.messages.size,
            hasRapidMessages = messageFrequency > 2f, // > 2 Messages/Min
            detectedProgression = progressionScore > 0.5f
        )
    }

    /**
     * Erkennt Stage-Progression (z.B. TRUST → NEEDS → ASSESSMENT)
     */
    private fun detectStageProgression(history: ConversationHistory): Float {
        if (history.messages.size < 2) return 0f

        val recentStages = history.messages
            .takeLast(5)
            .mapNotNull { it.stage }
            .filter { it != "STAGE_SAFE" }

        if (recentStages.isEmpty()) return 0f

        // Zähle verschiedene Grooming-Stages
        val uniqueStages = recentStages.toSet().size

        // Progression Score basierend auf Anzahl verschiedener Stages
        return when (uniqueStages) {
            0, 1 -> 0.0f
            2 -> 0.3f    // Zwei verschiedene Stages
            3 -> 0.6f    // Drei Stages = Eskalation
            else -> 0.9f // Vier+ Stages = KRITISCH
        }
    }

    /**
     * Berechnet Message-Frequenz (Messages pro Minute)
     */
    private fun calculateMessageFrequency(
        history: ConversationHistory,
        currentTime: Long
    ): Float {
        if (history.messages.size < 2) return 0f

        // Nur Messages der letzten 5 Minuten
        val recentWindow = 5 * 60 * 1000L
        val recentMessages = history.messages.filter {
            currentTime - it.timestamp < recentWindow
        }

        if (recentMessages.isEmpty()) return 0f

        val timeSpan = currentTime - recentMessages.first().timestamp
        val minutes = maxOf(timeSpan / 60000f, 0.1f) // Min 0.1 Min

        return recentMessages.size / minutes
    }

    /**
     * Berechnet Context Bonus
     */
    private fun calculateContextBonus(
        progressionScore: Float,
        conversationDuration: Long,
        messageFrequency: Float
    ): Float {
        var bonus = 0f

        // Progression Bonus
        bonus += progressionScore * 0.2f

        // Duration Bonus (längere Gespräche = mehr Investment)
        val durationMinutes = conversationDuration / 60000f
        if (durationMinutes > 30f) {
            bonus += 0.1f
        }

        // Frequency Bonus (Rapid-Fire Messages)
        if (messageFrequency > 2f) {
            bonus += 0.15f
        }

        return minOf(bonus, 0.4f) // Max +0.4 Context Bonus
    }

    /**
     * Cleanup alte Messages
     */
    private fun cleanupOldMessages(history: ConversationHistory, currentTime: Long) {
        history.messages.removeAll { message ->
            currentTime - message.timestamp > CONTEXT_WINDOW_MS
        }

        // Update Start-Time wenn Messages entfernt wurden
        if (history.messages.isNotEmpty()) {
            history.startTime = history.messages.first().timestamp
        }
    }

    /**
     * Hole Conversation History für Debugging/Logging
     */
    fun getConversationHistory(appPackage: String): List<Message> {
        return conversationCache.get(appPackage)?.messages ?: emptyList()
    }

    /**
     * Clear History für App (z.B. bei User-Request)
     */
    fun clearHistory(appPackage: String) {
        conversationCache.remove(appPackage)
    }

    data class ContextualResult(
        val score: Float,
        val baseScore: Float,
        val contextBonus: Float,
        val progressionScore: Float,
        val conversationDuration: Long,
        val messageCount: Int,
        val hasRapidMessages: Boolean,
        val detectedProgression: Boolean
    )
}

/**
 * Temporal Risk Analyzer
 * Erkennt zeitbasierte Risiko-Patterns
 */
class TemporalRiskAnalyzer {

    /**
     * Analysiert zeitbasiertes Risiko
     */
    fun analyzeTemporalRisk(
        text: String,
        timestamp: Long = System.currentTimeMillis()
    ): TemporalRisk {

        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
        }

        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // Late-Night Detection (23:00 - 06:00)
        val isLateNight = hour >= 23 || hour <= 6
        val lateNightRisk = if (isLateNight) 0.3f else 0f

        // Urgency Keywords
        val urgencyKeywords = listOf(
            "jetzt", "schnell", "sofort", "heute", "gleich",
            "now", "quick", "fast", "today", "immediately"
        )
        val hasUrgency = urgencyKeywords.any { it in text.lowercase() }
        val urgencyRisk = if (hasUrgency) 0.2f else 0f

        val totalRisk = lateNightRisk + urgencyRisk

        return TemporalRisk(
            risk = totalRisk,
            isLateNight = isLateNight,
            hasUrgency = hasUrgency,
            hour = hour
        )
    }

    data class TemporalRisk(
        val risk: Float,
        val isLateNight: Boolean,
        val hasUrgency: Boolean,
        val hour: Int
    )
}

/**
 * Emoji Risk Analyzer
 * Erkennt risikoreiche Emoji-Patterns
 */
class EmojiRiskAnalyzer {

    companion object {
        // Emoji-Kategorien mit Risiko-Scores
        private val ROMANTIC_EMOJIS = listOf("😍", "😘", "💕", "💋", "😻", "❤️", "💗")
        private val SECRECY_EMOJIS = listOf("🤫", "🙊", "🔒", "🤐", "🚫")
        private val MONEY_EMOJIS = listOf("💰", "💸", "💵", "💎", "🎁", "💳")
        private val SEXUAL_EMOJIS = listOf("🍆", "🍑", "💦", "👅")
    }

    /**
     * Analysiert Emoji-Risiko
     */
    fun analyzeEmojiRisk(text: String): EmojiRisk {
        var risk = 0f
        val detectedCategories = mutableListOf<String>()

        // Romantic Emojis (0.3 per emoji)
        val romanticCount = ROMANTIC_EMOJIS.count { it in text }
        if (romanticCount > 0) {
            risk += romanticCount * 0.3f
            detectedCategories.add("romantic")
        }

        // Secrecy Emojis (0.4 per emoji - höheres Risiko)
        val secrecyCount = SECRECY_EMOJIS.count { it in text }
        if (secrecyCount > 0) {
            risk += secrecyCount * 0.4f
            detectedCategories.add("secrecy")
        }

        // Money Emojis (0.35 per emoji)
        val moneyCount = MONEY_EMOJIS.count { it in text }
        if (moneyCount > 0) {
            risk += moneyCount * 0.35f
            detectedCategories.add("money")
        }

        // Sexual Emojis (0.5 per emoji - HÖCHSTES RISIKO)
        val sexualCount = SEXUAL_EMOJIS.count { it in text }
        if (sexualCount > 0) {
            risk += sexualCount * 0.5f
            detectedCategories.add("sexual")
        }

        return EmojiRisk(
            risk = minOf(risk, 1.0f),
            categories = detectedCategories,
            emojiCount = romanticCount + secrecyCount + moneyCount + sexualCount
        )
    }

    data class EmojiRisk(
        val risk: Float,
        val categories: List<String>,
        val emojiCount: Int
    )
}

/**
 * Social Engineering Detector
 * Erkennt Manipulations-Taktiken
 */
class SocialEngineeringDetector {

    companion object {
        private val TACTICS = mapOf(
            "reciprocity" to listOf(
                "ich habe dir geholfen", "du schuldest mir", "jetzt bist du dran",
                "i helped you", "you owe me", "your turn"
            ),
            "scarcity" to listOf(
                "nur heute", "letzte chance", "schnell entscheiden", "jetzt oder nie",
                "only today", "last chance", "decide quickly", "now or never"
            ),
            "authority" to listOf(
                "ich bin älter", "vertrau mir", "ich weiß es besser", "als erwachsener",
                "i'm older", "trust me", "i know better", "as an adult"
            ),
            "social_proof" to listOf(
                "alle machen das", "deine freunde auch", "das ist normal", "jeder macht das",
                "everyone does it", "your friends too", "it's normal", "everybody does it"
            ),
            "liking" to listOf(
                "wir sind besonders", "niemand versteht dich wie ich", "du bist anders",
                "we are special", "nobody understands you like i do", "you are different"
            )
        )
    }

    /**
     * Erkennt Social Engineering Tactics
     */
    fun detectTactics(text: String): SocialEngineeringResult {
        val textLower = text.lowercase()
        val detectedTactics = mutableListOf<String>()
        var risk = 0f

        for ((tactic, patterns) in TACTICS) {
            for (pattern in patterns) {
                if (pattern in textLower) {
                    detectedTactics.add(tactic)
                    risk += 0.25f
                    break // Nur einmal pro Taktik
                }
            }
        }

        return SocialEngineeringResult(
            risk = minOf(risk, 1.0f),
            tactics = detectedTactics
        )
    }

    data class SocialEngineeringResult(
        val risk: Float,
        val tactics: List<String>
    )
}
