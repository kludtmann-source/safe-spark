package com.example.safespark.ml

import android.util.Log

/**
 * Adult/Child Message Detector
 *
 * Basierend auf ArXiv Paper 2409.07958v1:
 * "Enhanced Online Grooming Detection Employing Context Determination
 * and Message-Level Analysis"
 *
 * Authors: Jake Street, Isibor Kennedy Ihianle, Funminiyi Olajide, Ahmad Lotfi
 * Institution: Nottingham Trent University, September 2024
 *
 * Key Concepts:
 * - Message Significance Threshold (MST): Wie viel Information gibt eine Message?
 * - Actor Significance Threshold (AST): Wie sicher müssen wir bei Adult/Child sein?
 * - Adult-Child Context: Wenn Adult + Child kommunizieren → höheres Grooming-Risiko
 */
class AdultChildDetector {

    companion object {
        private const val TAG = "AdultChildDetector"

        // Message Significance Threshold (0-1)
        // Messages unter diesem Wert werden ignoriert (zu wenig Information)
        const val MST = 0.25f

        // Actor Significance Threshold (0-1)
        // Für Kinderschutz: Höher = mehr Sensitivität, mehr False Positives
        // Niedriger = weniger False Positives, aber mehr False Negatives
        const val AST = 0.5f
    }

    /**
     * Ergebnis der Message-Analyse
     */
    data class MessageAnalysis(
        val isLikelyAdult: Boolean,
        val isLikelyChild: Boolean,
        val adultScore: Float,
        val childScore: Float,
        val isSignificant: Boolean,
        val dominantActor: String  // "ADULT", "CHILD", or "UNKNOWN"
    )

    /**
     * Ergebnis der Conversation-Analyse
     */
    data class ConversationContext(
        val isAdultChildContext: Boolean,
        val hasAdultActor: Boolean,
        val hasChildActor: Boolean,
        val riskMultiplier: Float,  // 1.0 = normal, 1.5 = erhöht
        val actorBreakdown: Map<String, String>
    )

    // ==================== ADULT INDICATORS ====================

    // Komplexe/formelle Sprache (typisch für Erwachsene)
    private val adultComplexLanguage = listOf(
        "therefore", "nevertheless",
        "consequently", "furthermore", "moreover", "regarding",
        "deshalb",
        "außerdem", "bezüglich", "hinsichtlich"
    )

    // Manipulative Phrasen (Grooming-Sprache von Erwachsenen)
    private val adultManipulativeLanguage = listOf(
        "mature", "special", "understand you", "trust me", "our secret",
        "don't tell", "just between us", "you're different", "older than",
        "reif", "besonders", "verstehe dich", "vertrau mir", "unser geheimnis",
        "sag nicht", "nur zwischen uns", "du bist anders", "älter als"
    )

    // Formelle Fragen (typisch für Erwachsene)
    private val adultFormalQuestions = listOf(
        "would you", "could you", "may i", "shall we", "might i",
        "würdest du", "könntest du", "darf ich", "sollen wir"
    )

    // Assessment-Fragen (Groomer checken ob Kind allein ist)
    private val adultAssessmentLanguage = listOf(
        "are you alone", "where are your parents", "home alone",
        "anyone there", "by yourself", "nobody home",
        "bist du allein", "wo sind deine eltern", "allein zuhause",
        "jemand bei dir", "für dich selbst", "niemand zuhause"
    )

    // ==================== CHILD INDICATORS ====================

    // Textspeak/Abkürzungen (typisch für Kinder/Jugendliche)
    private val childTextspeak = listOf(
        "lol", "omg", "idk", "brb", "gtg", "nvm", "tbh", "imo", "ikr",
        "wtf", "smh", "fyi", "btw", "rofl", "lmao", "yolo", "fomo"
    )

    // Informelle Abkürzungen
    private val childAbbreviations = listOf(
        "u", "ur", "r", "y", "2", "4", "b4", "cuz", "gonna", "wanna",
        "gotta", "kinda", "sorta", "dunno", "lemme", "gimme"
    )

    // Kurze Antworten (typisch für Kinder)
    private val childShortResponses = listOf(
        "hi", "hey", "ok", "okay", "yeah", "yep", "nope", "cool",
        "nice", "sure", "idc", "whatever", "fine", "k", "kk",
        "ja", "nein", "jo", "joa", "ne", "nö", "geil", "krass"
    )

    // Gaming/Internet-Slang (typisch für jüngere User)
    private val childGamingSlang = listOf(
        "noob", "gg", "ez", "rekt", "poggers", "sus", "cap", "no cap",
        "slay", "based", "cringe", "simp", "ratio", "L", "W",
        "robux", "vbucks", "fortnite", "roblox", "minecraft"
    )

    // ==================== EMOJI PATTERNS ====================

    private val emojiPattern = Regex("[\\uD83C-\\uDBFF\\uDC00-\\uDFFF]+|[\\u2600-\\u27BF]")

    /**
     * Analysiert eine einzelne Message
     */
    fun analyzeMessage(text: String): MessageAnalysis {
        val textLower = text.lowercase().trim()

        var adultScore = 0f
        var childScore = 0f

        // ===== ADULT SCORING =====

        // Komplexe Sprache (+0.15 pro Match)
        adultComplexLanguage.forEach { indicator ->
            if (textLower.contains(indicator)) {
                adultScore += 0.15f
            }
        }

        // Manipulative Sprache (+0.25 pro Match - WICHTIG für Grooming!)
        adultManipulativeLanguage.forEach { indicator ->
            if (textLower.contains(indicator)) {
                adultScore += 0.25f
            }
        }

        // Formelle Fragen (+0.12 pro Match)
        adultFormalQuestions.forEach { indicator ->
            if (textLower.contains(indicator)) {
                adultScore += 0.12f
            }
        }

        // Assessment-Fragen (+0.30 pro Match - HÖCHSTES RISIKO!)
        adultAssessmentLanguage.forEach { indicator ->
            if (textLower.contains(indicator)) {
                adultScore += 0.30f
            }
        }

        // Längere Sätze = eher Adult
        if (text.length > 80) adultScore += 0.10f
        if (text.length > 150) adultScore += 0.10f

        // Korrekte Interpunktion = eher Adult
        if (text.contains(". ") || text.endsWith(".")) adultScore += 0.08f
        if (text.contains(", ")) adultScore += 0.05f

        // ===== CHILD SCORING =====

        // Textspeak (+0.20 pro Match)
        childTextspeak.forEach { indicator ->
            if (textLower.contains(indicator)) {
                childScore += 0.20f
            }
        }

        // Abkürzungen (+0.15 pro Match)
        childAbbreviations.forEach { indicator ->
            // Nur als eigenständiges Wort (nicht Teil eines längeren Worts)
            if (Regex("\\b$indicator\\b").containsMatchIn(textLower)) {
                childScore += 0.15f
            }
        }

        // Kurze Antworten (+0.12 pro Match)
        childShortResponses.forEach { indicator ->
            if (textLower == indicator || textLower.startsWith("$indicator ")) {
                childScore += 0.12f
            }
        }

        // Gaming-Slang (+0.18 pro Match)
        childGamingSlang.forEach { indicator ->
            if (textLower.contains(indicator)) {
                childScore += 0.18f
            }
        }

        // Viele Emojis = eher Child
        val emojiCount = emojiPattern.findAll(text).count()
        childScore += (emojiCount * 0.08f)

        // Sehr kurze Messages = eher Child
        if (text.length < 15) childScore += 0.08f
        if (text.length < 8) childScore += 0.05f

        // Keine Großschreibung am Satzanfang = eher Child
        if (text.isNotEmpty() && text[0].isLowerCase()) childScore += 0.05f

        // ===== FINAL CALCULATION =====

        // Normalisieren auf 0-1
        adultScore = adultScore.coerceIn(0f, 1f)
        childScore = childScore.coerceIn(0f, 1f)

        // Significance Check
        val maxScore = maxOf(adultScore, childScore)
        val isSignificant = maxScore >= MST

        // Dominant Actor bestimmen
        val dominantActor = when {
            !isSignificant -> "UNKNOWN"
            adultScore > childScore && adultScore >= AST -> "ADULT"
            childScore > adultScore && childScore >= AST -> "CHILD"
            else -> "UNKNOWN"
        }

        val result = MessageAnalysis(
            isLikelyAdult = adultScore > childScore && adultScore >= AST,
            isLikelyChild = childScore > adultScore && childScore >= AST,
            adultScore = adultScore,
            childScore = childScore,
            isSignificant = isSignificant,
            dominantActor = dominantActor
        )

        Log.d(TAG, "📊 Message Analysis: '${text.take(30)}...' → $dominantActor (A=${(adultScore*100).toInt()}%, C=${(childScore*100).toInt()}%)")

        return result
    }

    /**
     * Analysiert eine Conversation auf Adult-Child Context
     *
     * @param messages Liste von (actorId, messageText) Paaren
     * @return ConversationContext mit Risiko-Bewertung
     */
    fun analyzeConversation(messages: List<Pair<String, String>>): ConversationContext {
        if (messages.isEmpty()) {
            return ConversationContext(
                isAdultChildContext = false,
                hasAdultActor = false,
                hasChildActor = false,
                riskMultiplier = 1.0f,
                actorBreakdown = emptyMap()
            )
        }

        // Gruppiere Messages nach Actor
        val actorMessages = messages.groupBy { it.first }
        val actorAnalyses = mutableMapOf<String, MutableList<MessageAnalysis>>()

        // Analysiere alle Messages pro Actor
        actorMessages.forEach { (actorId, msgs) ->
            val analyses = msgs.map { analyzeMessage(it.second) }
            actorAnalyses[actorId] = analyses.toMutableList()
        }

        // Bestimme Actor Context pro Actor
        val actorContexts = mutableMapOf<String, String>()

        actorAnalyses.forEach { (actorId, analyses) ->
            // Nur signifikante Messages berücksichtigen
            val significantAnalyses = analyses.filter { it.isSignificant }

            if (significantAnalyses.isEmpty()) {
                actorContexts[actorId] = "UNKNOWN"
                return@forEach
            }

            // Durchschnittliche Scores
            val avgAdult = significantAnalyses.map { it.adultScore }.average().toFloat()
            val avgChild = significantAnalyses.map { it.childScore }.average().toFloat()

            // Bestimme Actor-Typ
            val actorType = when {
                avgAdult > avgChild && avgAdult >= AST -> "ADULT"
                avgChild > avgAdult && avgChild >= AST -> "CHILD"
                else -> "UNKNOWN"
            }

            actorContexts[actorId] = actorType
            Log.d(TAG, "👤 Actor '$actorId': $actorType (avgA=${(avgAdult*100).toInt()}%, avgC=${(avgChild*100).toInt()}%)")
        }

        // Bestimme Full Conversation Context
        val hasAdult = actorContexts.values.contains("ADULT")
        val hasChild = actorContexts.values.contains("CHILD")
        val isAdultChildContext = hasAdult && hasChild

        // Risk Multiplier berechnen
        val riskMultiplier = when {
            isAdultChildContext -> 1.5f  // Adult + Child = 50% höheres Risiko!
            hasAdult -> 1.2f             // Nur Adult erkannt = leicht erhöht
            else -> 1.0f                 // Normal
        }

        val result = ConversationContext(
            isAdultChildContext = isAdultChildContext,
            hasAdultActor = hasAdult,
            hasChildActor = hasChild,
            riskMultiplier = riskMultiplier,
            actorBreakdown = actorContexts
        )

        if (isAdultChildContext) {
            Log.w(TAG, "⚠️ ADULT-CHILD CONTEXT DETECTED! Risk Multiplier: $riskMultiplier")
        }

        return result
    }

    /**
     * Schnelle Methode: Ist diese Message wahrscheinlich von einem Adult?
     */
    fun isLikelyAdultMessage(text: String): Boolean {
        return analyzeMessage(text).isLikelyAdult
    }

    /**
     * Schnelle Methode: Ist diese Message wahrscheinlich von einem Child?
     */
    fun isLikelyChildMessage(text: String): Boolean {
        return analyzeMessage(text).isLikelyChild
    }

    /**
     * Berechnet einen Risk-Boost basierend auf Adult-Language
     * Kann direkt zum Grooming-Score addiert werden
     */
    fun calculateAdultLanguageBoost(text: String): Float {
        val analysis = analyzeMessage(text)

        return when {
            analysis.isLikelyAdult && analysis.adultScore > 0.7f -> 0.20f
            analysis.isLikelyAdult && analysis.adultScore > 0.5f -> 0.15f
            analysis.isLikelyAdult -> 0.10f
            else -> 0f
        }
    }
}
