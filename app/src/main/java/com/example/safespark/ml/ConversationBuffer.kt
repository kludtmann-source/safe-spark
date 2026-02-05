package com.example.safespark.ml

import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Konversations-Buffer für Osprey-Integration
 *
 * DSGVO-konform: Alle Daten nur im RAM, keine Persistenz.
 * Ermöglicht Grooming-Erkennung über Konversationsverläufe hinweg.
 *
 * Osprey arbeitet auf Konversationsebene und erkennt Eskalationsmuster:
 * Trust → Isolation → Desensitization → Sexual Content → Maintenance
 */
object ConversationBuffer {

    private const val TAG = "ConversationBuffer"

    // Konfiguration
    private const val MAX_MESSAGES_PER_CONTACT = 50
    private const val MAX_AGE_MINUTES = 60L
    private const val MAX_AGE_MS = MAX_AGE_MINUTES * 60 * 1000

    /**
     * Nachricht in einer Konversation
     */
    data class ConversationMessage(
        val text: String,
        val authorId: String,
        val timestamp: Long,
        val isLocalUser: Boolean
    )

    /**
     * Thread-safe Map: contactId → Liste von Nachrichten
     */
    private val conversations = ConcurrentHashMap<String, MutableList<ConversationMessage>>()

    /**
     * Fügt eine Nachricht zum Konversations-Buffer hinzu
     *
     * @param contactId Pseudonymisierte Kontakt-ID (z.B. Hash aus Package+ChatTitle)
     * @param message Die Nachricht
     */
    @Synchronized
    fun addMessage(contactId: String, message: ConversationMessage) {
        val messages = conversations.getOrPut(contactId) { mutableListOf() }

        // Nachricht hinzufügen
        messages.add(message)

        // Sliding Window anwenden
        cleanupConversation(contactId)

        // Nur Metadaten loggen (DSGVO!)
        val msgCount = messages.size
        val durationMin = getConversationDurationMinutes(contactId)
        Log.d(TAG, "Buffer $contactId: $msgCount msgs, ${durationMin}min")
    }

    /**
     * Gibt den aktuellen Konversations-Buffer für einen Kontakt zurück
     */
    @Synchronized
    fun getConversation(contactId: String): List<ConversationMessage> {
        cleanupConversation(contactId)
        return conversations[contactId]?.toList() ?: emptyList()
    }

    /**
     * Formatiert die Konversation für Osprey-Input
     *
     * Format: [CHILD] Text [SEP] [CONTACT] Text [SEP] ...
     */
    @Synchronized
    fun getOspreyInput(contactId: String): String {
        val messages = getConversation(contactId)
        if (messages.isEmpty()) return ""

        return messages.joinToString(" [SEP] ") { msg ->
            val authorTag = if (msg.isLocalUser) "[CHILD]" else "[CONTACT]"
            "$authorTag ${msg.text}"
        }
    }

    /**
     * Berechnet kontextuelle Features für die Analyse
     */
    @Synchronized
    fun getContextFeatures(contactId: String): Map<String, Float> {
        val messages = getConversation(contactId)

        if (messages.isEmpty()) {
            return mapOf(
                "message_count" to 0f,
                "contact_msg_ratio" to 0f,
                "avg_msg_length" to 0f,
                "conversation_duration_min" to 0f,
                "messages_per_minute" to 0f
            )
        }

        val contactMessages = messages.filter { !it.isLocalUser }
        val contactMsgRatio = if (messages.isNotEmpty()) {
            contactMessages.size.toFloat() / messages.size
        } else 0f

        val avgContactMsgLength = if (contactMessages.isNotEmpty()) {
            contactMessages.map { it.text.length }.average().toFloat()
        } else 0f

        val durationMin = getConversationDurationMinutes(contactId)
        val msgsPerMinute = if (durationMin > 0) {
            messages.size.toFloat() / durationMin
        } else messages.size.toFloat()

        return mapOf(
            "message_count" to messages.size.toFloat(),
            "contact_msg_ratio" to contactMsgRatio,
            "avg_msg_length" to avgContactMsgLength,
            "conversation_duration_min" to durationMin,
            "messages_per_minute" to msgsPerMinute
        )
    }

    /**
     * Berechnet die Dauer der Konversation in Minuten
     */
    private fun getConversationDurationMinutes(contactId: String): Float {
        val messages = conversations[contactId] ?: return 0f
        if (messages.size < 2) return 0f

        val oldest = messages.minOfOrNull { it.timestamp } ?: return 0f
        val newest = messages.maxOfOrNull { it.timestamp } ?: return 0f

        return (newest - oldest) / 60000f  // ms → min
    }

    /**
     * Löscht den Buffer für einen spezifischen Kontakt
     */
    @Synchronized
    fun clearConversation(contactId: String) {
        conversations.remove(contactId)
        Log.d(TAG, "Buffer cleared for: $contactId")
    }

    /**
     * Löscht alle Konversations-Buffer (z.B. bei App-Neustart)
     */
    @Synchronized
    fun clearAll() {
        val count = conversations.size
        conversations.clear()
        Log.d(TAG, "All buffers cleared ($count conversations)")
    }

    /**
     * Bereinigt alte Nachrichten (Sliding Window)
     */
    private fun cleanupConversation(contactId: String) {
        val messages = conversations[contactId] ?: return
        val now = System.currentTimeMillis()
        val cutoff = now - MAX_AGE_MS

        // Entferne alte Nachrichten
        messages.removeAll { it.timestamp < cutoff }

        // Behalte nur die letzten MAX_MESSAGES_PER_CONTACT
        while (messages.size > MAX_MESSAGES_PER_CONTACT) {
            messages.removeAt(0)
        }
    }

    /**
     * Gibt die Anzahl aktiver Konversationen zurück
     */
    fun getActiveConversationCount(): Int = conversations.size

    /**
     * Prüft ob für einen Kontakt bereits Nachrichten existieren
     */
    fun hasConversation(contactId: String): Boolean {
        return conversations.containsKey(contactId) &&
               (conversations[contactId]?.isNotEmpty() ?: false)
    }

    /**
     * Generiert eine pseudonymisierte Contact-ID aus Package und Chat-Titel
     *
     * DSGVO: Kein Klarname, nur Hash
     */
    fun generateContactId(packageName: String, chatIdentifier: String): String {
        val combined = "$packageName:$chatIdentifier"
        return "C${combined.hashCode().toString(16).takeLast(8).uppercase()}"
    }
}
