package com.example.safespark.trust

import android.content.Context
import android.util.Log
import com.example.safespark.config.DetectionConfig
import com.example.safespark.database.KidGuardDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Trust Level Enum
 * 
 * Fix 2: Less aggressive multipliers to prevent false negatives
 */
enum class TrustLevel(val scoreMultiplier: Float) {
    FAMILY(0.4f),    // Eltern-Whitelist: Score × 0.4 (−60%) - was 0.3f
    TRUSTED(0.7f),   // >100 msgs, 0 risks: Score × 0.7 (−30%) - was 0.5f
    KNOWN(0.85f),    // >20 msgs, <2 risks: Score × 0.85 (−15%) - was 0.8f
    UNKNOWN(1.0f)    // Default: keine Reduktion
}

/**
 * Contact Trust Manager - Singleton für Trust-Level-Verwaltung
 *
 * Kombiniert:
 * 1. Eltern-Whitelist (manuell gesetzt)
 * 2. Automatisches Lernen basierend auf Historie
 *
 * Fix 1: In-memory cache to prevent runBlocking ANR
 *
 * WICHTIG: BYPASS_PATTERNS müssen IMMER vollen Score behalten!
 */
object ContactTrustManager {
    
    private const val TAG = "ContactTrustManager"
    
    /**
     * Fix 1: In-memory trust cache (thread-safe)
     * Prevents ANR by avoiding runBlocking on main thread
     */
    private val trustCache = ConcurrentHashMap<String, TrustLevel>()
    
    /**
     * Fix 1: In-memory trust cache (thread-safe)
     * Prevents ANR by avoiding runBlocking on main thread
     */
    private val trustCache = ConcurrentHashMap<String, TrustLevel>()
    
    /**
     * Fix 1: Synchronous cache-only trust level lookup
     * Returns UNKNOWN on cache miss (safest default)
     * 
     * @param contactId Contact-ID
     * @return Trust-Level from cache or UNKNOWN
     */
    fun getTrustLevelSync(contactId: String): TrustLevel {
        return trustCache[contactId] ?: TrustLevel.UNKNOWN
    }
    
    /**
     * Fix 1: Refresh trust cache from database (suspend function)
     * 
     * @param contactId Contact-ID
     * @param context Android Context
     */
    suspend fun refreshTrustCache(contactId: String, context: Context) {
        val level = getTrustLevel(contactId, context)
        trustCache[contactId] = level
    }
    
    /**
     * Fix 1: Update cache directly (for inline updates)
     * 
     * @param contactId Contact-ID
     * @param level Trust-Level
     */
    fun updateCacheDirectly(contactId: String, level: TrustLevel) {
        trustCache[contactId] = level
        Log.d(TAG, "📦 Cache updated: $contactId → $level")
    }
    
    /**
     * Bestimmt das Trust-Level für einen Kontakt
     *
     * Priorität:
     * 1. Eltern-Whitelist (manuallySet = true)
     * 2. Automatisches Lernen basierend auf Historie
     * 3. Default = UNKNOWN
     *
     * Fix 1: Also updates cache
     *
     * @param contactId Pseudonymisierte Contact-ID
     * @param context Android Context für DB-Zugriff
     * @return Trust-Level
     */
    suspend fun getTrustLevel(contactId: String, context: Context): TrustLevel {
        return withContext(Dispatchers.IO) {
            try {
                val dao = KidGuardDatabase.getDatabase(context).trustedContactDao()
                val contact = dao.getContact(contactId)
                
                if (contact == null) {
                    Log.d(TAG, "📊 Contact $contactId: UNKNOWN (not in DB)")
                    trustCache[contactId] = TrustLevel.UNKNOWN
                    return@withContext TrustLevel.UNKNOWN
                }
                
                // Priorität 1: Manuell gesetzte Whitelist
                if (contact.manuallySet) {
                    val level = TrustLevel.valueOf(contact.trustLevel)
                    Log.d(TAG, "📊 Contact $contactId: $level (manually set)")
                    trustCache[contactId] = level
                    return@withContext level
                }
                
                // Priorität 2: Automatisches Lernen basierend auf Historie
                val level = calculateAutoTrustLevel(contact)
                Log.d(TAG, "📊 Contact $contactId: $level (auto-learned from ${contact.totalMessages} msgs, ${contact.riskMessageCount} risks)")
                trustCache[contactId] = level
                
                return@withContext level
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error getting trust level for $contactId", e)
                trustCache[contactId] = TrustLevel.UNKNOWN
                return@withContext TrustLevel.UNKNOWN
            }
        }
    }
    
    /**
     * Berechnet Trust-Level basierend auf Konversations-Historie
     */
    private fun calculateAutoTrustLevel(contact: TrustedContact): TrustLevel {
        val totalMessages = contact.totalMessages
        val riskMessageCount = contact.riskMessageCount
        
        return when {
            // >100 Msgs, 0 Risks → TRUSTED
            totalMessages > 100 && riskMessageCount == 0 -> TrustLevel.TRUSTED
            
            // >20 Msgs, <2 Risks → KNOWN
            totalMessages > 20 && riskMessageCount < 2 -> TrustLevel.KNOWN
            
            // Sonst → UNKNOWN
            else -> TrustLevel.UNKNOWN
        }
    }
    
    /**
     * Aktualisiert die Statistiken für einen Kontakt
     * 
     * Fix 4: Risk-spike degradation
     *
     * @param contactId Contact-ID
     * @param messageRiskScore Risk-Score der neuen Nachricht (RAW score, before trust modifier!)
     * @param context Android Context
     */
    suspend fun updateContactStats(
        contactId: String,
        messageRiskScore: Float,
        context: Context
    ) {
        withContext(Dispatchers.IO) {
            try {
                val dao = KidGuardDatabase.getDatabase(context).trustedContactDao()
                val existing = dao.getContact(contactId)
                
                val contact = if (existing != null) {
                    // Fix 4: Risk-spike detection
                    val currentTrustLevel = TrustLevel.valueOf(existing.trustLevel)
                    val shouldDegradeFromSpike = messageRiskScore > 0.75f &&
                        (currentTrustLevel == TrustLevel.TRUSTED || currentTrustLevel == TrustLevel.KNOWN) &&
                        !existing.manuallySet
                    
                    if (shouldDegradeFromSpike) {
                        Log.w(TAG, "🚨 TRUST DEGRADATION: $contactId demoted from $currentTrustLevel → UNKNOWN (risk spike: ${(messageRiskScore*100).toInt()}%)")
                        updateCacheDirectly(contactId, TrustLevel.UNKNOWN)
                    }
                    
                    // Update existing contact
                    val isRisk = messageRiskScore > 0.5f
                    val newTotalMessages = existing.totalMessages + 1
                    val newRiskCount = if (isRisk) existing.riskMessageCount + 1 else existing.riskMessageCount
                    val newSafeCount = if (!isRisk) existing.safeMessageCount + 1 else existing.safeMessageCount
                    
                    // Berechne neuen Durchschnitt
                    val newAvgRiskScore = ((existing.averageRiskScore * existing.totalMessages) + messageRiskScore) / newTotalMessages
                    
                    // Wenn manuell gesetzt, nur Stats updaten, nicht Trust-Level
                    // Fix 4: Apply degradation immediately
                    val newTrustLevel = if (existing.manuallySet) {
                        existing.trustLevel
                    } else if (shouldDegradeFromSpike) {
                        TrustLevel.UNKNOWN.name
                    } else {
                        val tempContact = existing.copy(
                            totalMessages = newTotalMessages,
                            riskMessageCount = newRiskCount
                        )
                        calculateAutoTrustLevel(tempContact).name
                    }
                    
                    existing.copy(
                        totalMessages = newTotalMessages,
                        riskMessageCount = newRiskCount,
                        safeMessageCount = newSafeCount,
                        averageRiskScore = newAvgRiskScore,
                        lastSeenTimestamp = System.currentTimeMillis(),
                        trustLevel = newTrustLevel
                    )
                } else {
                    // Create new contact
                    val isRisk = messageRiskScore > 0.5f
                    TrustedContact(
                        contactIdHash = contactId,
                        trustLevel = TrustLevel.UNKNOWN.name,
                        totalMessages = 1,
                        riskMessageCount = if (isRisk) 1 else 0,
                        safeMessageCount = if (!isRisk) 1 else 0,
                        averageRiskScore = messageRiskScore,
                        lastSeenTimestamp = System.currentTimeMillis(),
                        manuallySet = false
                    )
                }
                
                dao.insert(contact)
                // Update cache after DB insert
                updateCacheDirectly(contactId, TrustLevel.valueOf(contact.trustLevel))
                Log.d(TAG, "✅ Updated stats for $contactId: ${contact.totalMessages} msgs, ${contact.riskMessageCount} risks, trust=${contact.trustLevel}")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating contact stats for $contactId", e)
            }
        }
    }
    
    /**
     * Wendet Trust-Level-Modifikator auf einen Score an
     *
     * Fix 2: Minimum score floor to prevent false negatives
     *
     * WICHTIG: BYPASS_PATTERNS bleiben immer bei vollem Score!
     *
     * @param baseScore Original-Score vor Trust-Modifikation
     * @param trustLevel Trust-Level des Kontakts
     * @param text Ursprünglicher Text (für BYPASS_PATTERNS-Check)
     * @return Modifizierter Score
     */
    fun applyTrustModifier(baseScore: Float, trustLevel: TrustLevel, text: String): Float {
        // Fix 2: BYPASS_PATTERNS dürfen NICHT reduziert werden!
        if (DetectionConfig.matchesBypassPattern(text)) {
            Log.w(TAG, "🚨 BYPASS PATTERN detected - keeping full score regardless of trust level")
            return baseScore
        }
        
        val modifiedScore = baseScore * trustLevel.scoreMultiplier
        
        // Fix 2: Minimum score floor - high-risk content must not be fully suppressed
        // If raw score is clearly dangerous (>= 0.80), keep it above isRisk threshold (0.5)
        val finalScore = if (baseScore >= 0.80f && modifiedScore < 0.55f) {
            Log.w(TAG, "🚨 SCORE FLOOR applied: ${(modifiedScore * 100).toInt()}% → 55% (base was ${(baseScore * 100).toInt()}%)")
            0.55f
        } else {
            modifiedScore
        }
        
        if (trustLevel != TrustLevel.UNKNOWN) {
            Log.d(TAG, "📊 Trust modifier applied: ${(baseScore * 100).toInt()}% → ${(finalScore * 100).toInt()}% (Trust: $trustLevel)")
        }
        
        return finalScore
    }
    
    /**
     * Setzt einen Kontakt manuell als FAMILY (Eltern-Whitelist)
     *
     * @param contactId Contact-ID
     * @param displayName Optional: Anzeigename für UI (z.B. "Mama")
     * @param context Android Context
     */
    suspend fun setAsFamily(contactId: String, displayName: String = "", context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val dao = KidGuardDatabase.getDatabase(context).trustedContactDao()
                val existing = dao.getContact(contactId)
                
                val contact = if (existing != null) {
                    existing.copy(
                        trustLevel = TrustLevel.FAMILY.name,
                        displayNameHint = displayName,
                        manuallySet = true
                    )
                } else {
                    TrustedContact(
                        contactIdHash = contactId,
                        trustLevel = TrustLevel.FAMILY.name,
                        displayNameHint = displayName,
                        manuallySet = true,
                        lastSeenTimestamp = System.currentTimeMillis()
                    )
                }
                
                dao.insert(contact)
                Log.i(TAG, "✅ Contact $contactId set as FAMILY: $displayName")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error setting contact as FAMILY", e)
            }
        }
    }
    
    /**
     * Entfernt einen Kontakt aus der Whitelist (zurück zu automatischem Lernen)
     */
    suspend fun removeFromWhitelist(contactId: String, context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val dao = KidGuardDatabase.getDatabase(context).trustedContactDao()
                val existing = dao.getContact(contactId)
                
                if (existing != null && existing.manuallySet) {
                    val updated = existing.copy(
                        manuallySet = false,
                        trustLevel = calculateAutoTrustLevel(existing).name
                    )
                    dao.insert(updated)
                    Log.i(TAG, "✅ Contact $contactId removed from whitelist, auto trust level: ${updated.trustLevel}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error removing contact from whitelist", e)
            }
        }
    }
}
