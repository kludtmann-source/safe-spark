package com.example.safespark.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Conversation Message Entity - Persistente Speicherung aller Chat-Nachrichten
 *
 * Ermöglicht:
 * - Osprey-kompatible Konversationsanalyse
 * - Automatisches Trust-Learning
 * - Überleben von App-Neustarts
 *
 * DSGVO: Alle Daten pseudonymisiert.
 * TODO: Text verschlüsseln mit EncryptedSharedPreferences-Pattern
 */
@Entity(tableName = "conversation_messages")
data class ConversationMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contactIdHash: String,      // Pseudonymisiert (z.B. "CA1234567")
    val appPackage: String,         // Quell-App
    val text: String,               // Nachrichtentext (TODO: verschlüsselt speichern)
    val authorId: String,           // Wer hat geschrieben (Hash)
    val isLocalUser: Boolean,       // KIND (true) oder KONTAKT (false)
    val timestamp: Long,
    val riskScore: Float = 0f,      // Score dieser Nachricht (auch 0 = safe)
    val detectedStage: String = "SAFE"  // Echte Stage vom ML/Osprey (NICHT score-basiert!)
)
