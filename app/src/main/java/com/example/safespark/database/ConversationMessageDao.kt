package com.example.safespark.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * DAO für ConversationMessageEntity
 */
@Dao
interface ConversationMessageDao {
    
    @Query("SELECT * FROM conversation_messages WHERE contactIdHash = :contactId ORDER BY timestamp ASC")
    fun getConversation(contactId: String): List<ConversationMessageEntity>
    
    @Query("SELECT * FROM conversation_messages WHERE contactIdHash = :contactId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMessages(contactId: String, limit: Int = 50): List<ConversationMessageEntity>
    
    @Insert
    fun insert(message: ConversationMessageEntity): Long
    
    @Query("DELETE FROM conversation_messages WHERE timestamp < :before")
    fun deleteOldMessages(before: Long): Int
    
    @Query("SELECT COUNT(*) FROM conversation_messages WHERE contactIdHash = :contactId")
    fun getMessageCount(contactId: String): Int
    
    @Query("SELECT COUNT(*) FROM conversation_messages WHERE contactIdHash = :contactId AND riskScore > 0.5")
    fun getRiskMessageCount(contactId: String): Int
    
    @Query("SELECT AVG(riskScore) FROM conversation_messages WHERE contactIdHash = :contactId")
    fun getAverageRiskScore(contactId: String): Float?
}
