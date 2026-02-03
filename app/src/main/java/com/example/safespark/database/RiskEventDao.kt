package com.example.safespark.database

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * RiskEvent Data Access Object (DAO)
 */
@Dao
interface RiskEventDao {

    @Query("SELECT * FROM risk_events ORDER BY timestamp DESC")
    fun getAllEvents(): LiveData<List<RiskEvent>>

    @Query("SELECT * FROM risk_events WHERE dismissed = 0 ORDER BY timestamp DESC")
    fun getActiveEvents(): LiveData<List<RiskEvent>>

    @Query("SELECT * FROM risk_events WHERE timestamp > :since ORDER BY timestamp DESC")
    fun getEventsSince(since: Long): List<RiskEvent>

    @Query("SELECT COUNT(*) FROM risk_events WHERE timestamp > :since AND dismissed = 0")
    fun getActiveCountSince(since: Long): Int

    @Query("SELECT * FROM risk_events WHERE appPackage = :appPackage ORDER BY timestamp DESC")
    fun getEventsByApp(appPackage: String): LiveData<List<RiskEvent>>

    @Query("SELECT * FROM risk_events WHERE riskScore >= 0.8 AND dismissed = 0 ORDER BY timestamp DESC")
    fun getHighRiskEvents(): LiveData<List<RiskEvent>>

    @Insert
    fun insert(event: RiskEvent): Long

    @Update
    fun update(event: RiskEvent)

    @Query("DELETE FROM risk_events WHERE timestamp < :before")
    fun deleteOldEvents(before: Long): Int

    @Query("DELETE FROM risk_events")
    fun deleteAll(): Int

    @Query("SELECT * FROM risk_events WHERE id = :id")
    fun getEventById(id: Long): RiskEvent?
}
