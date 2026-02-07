package com.example.safespark.trust

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * Data Access Object für TrustedContact Entity
 */
@Dao
interface TrustedContactDao {
    
    @Query("SELECT * FROM trusted_contacts WHERE contactIdHash = :contactId")
    fun getContact(contactId: String): TrustedContact?
    
    @Query("SELECT * FROM trusted_contacts")
    fun getAllContacts(): List<TrustedContact>
    
    @Query("SELECT * FROM trusted_contacts WHERE manuallySet = 1")
    fun getManuallySetContacts(): List<TrustedContact>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(contact: TrustedContact): Long
    
    @Update
    fun update(contact: TrustedContact): Int
    
    @Query("DELETE FROM trusted_contacts WHERE contactIdHash = :contactId")
    fun delete(contactId: String): Int
    
    @Query("UPDATE trusted_contacts SET totalMessages = :totalMessages, " +
           "riskMessageCount = :riskMessageCount, safeMessageCount = :safeMessageCount, " +
           "averageRiskScore = :averageRiskScore, lastSeenTimestamp = :lastSeenTimestamp " +
           "WHERE contactIdHash = :contactId")
    fun updateStats(
        contactId: String,
        totalMessages: Int,
        riskMessageCount: Int,
        safeMessageCount: Int,
        averageRiskScore: Float,
        lastSeenTimestamp: Long
    ): Int
}
