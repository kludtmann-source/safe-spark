package com.example.safespark.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * RiskEvent Entity - Speichert erkannte Grooming-Risiken
 */
@Entity(tableName = "risk_events")
data class RiskEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val appPackage: String,
    val appName: String,
    val message: String,
    val riskScore: Float,
    val mlStage: String,
    val keywordMatches: String = "",
    val dismissed: Boolean = false
)
