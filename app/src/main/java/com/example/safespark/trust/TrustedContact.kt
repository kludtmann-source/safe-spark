package com.example.safespark.trust

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity für vertrauenswürdige Kontakte (Parent Whitelist + Automatic Learning)
 *
 * DSGVO: Nur pseudonymisierte Hashes, keine Klarnamen außer optionalem displayNameHint
 */
@Entity(tableName = "trusted_contacts")
data class TrustedContact(
    @PrimaryKey
    val contactIdHash: String,    // Pseudonymisiert (aus ConversationBuffer.generateContactId)
    val trustLevel: String,       // "FAMILY", "TRUSTED", "KNOWN", "UNKNOWN"
    val displayNameHint: String = "",  // Nur für UI: "Mama", "Max" (optional, Eltern setzen das)
    val totalMessages: Int = 0,
    val riskMessageCount: Int = 0,
    val safeMessageCount: Int = 0,
    val averageRiskScore: Float = 0f,
    val lastSeenTimestamp: Long = 0,
    val manuallySet: Boolean = false  // true = Eltern-Whitelist, false = automatisch gelernt
)
