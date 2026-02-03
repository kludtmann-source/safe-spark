package com.example.safespark.database

import androidx.lifecycle.LiveData
import android.util.Log
import java.util.concurrent.Executors

/**
 * RiskEvent Repository - Vereinfacht ohne Coroutines
 */
class RiskEventRepository(private val dao: RiskEventDao) {

    private val TAG = "RiskEventRepository"
    private val executor = Executors.newSingleThreadExecutor()

    val allEvents: LiveData<List<RiskEvent>> = dao.getAllEvents()
    val activeEvents: LiveData<List<RiskEvent>> = dao.getActiveEvents()
    val highRiskEvents: LiveData<List<RiskEvent>> = dao.getHighRiskEvents()

    fun insert(event: RiskEvent): Long {
        val id = dao.insert(event)
        Log.d(TAG, "✅ Event gespeichert: ID=$id, App=${event.appName}, Score=${event.riskScore}")
        return id
    }

    fun dismissEvent(event: RiskEvent) {
        executor.execute {
            dao.update(event.copy(dismissed = true))
            Log.d(TAG, "👋 Event ignoriert: ID=${event.id}")
        }
    }

    fun update(event: RiskEvent) {
        executor.execute {
            dao.update(event)
        }
    }

    fun getTodayCount(): Int {
        val todayStart = getStartOfDay()
        return dao.getActiveCountSince(todayStart)
    }

    fun getWeekCount(): Int {
        val weekStart = getStartOfWeek()
        return dao.getActiveCountSince(weekStart)
    }

    fun getMonthCount(): Int {
        val monthStart = getStartOfMonth()
        return dao.getActiveCountSince(monthStart)
    }

    fun getEventsByApp(appPackage: String): LiveData<List<RiskEvent>> {
        return dao.getEventsByApp(appPackage)
    }

    fun getEventById(id: Long): RiskEvent? {
        return dao.getEventById(id)
    }

    fun cleanupOldEvents(retentionDays: Int = 30) {
        executor.execute {
            val cutoffTime = System.currentTimeMillis() - (retentionDays.toLong() * 24 * 60 * 60 * 1000)
            val deleted = dao.deleteOldEvents(cutoffTime)
            Log.d(TAG, "🗑️ $deleted Events älter als $retentionDays Tage gelöscht")
        }
    }

    fun deleteAll() {
        executor.execute {
            val deleted = dao.deleteAll()
            Log.d(TAG, "🗑️ $deleted Events gelöscht")
        }
    }

    private fun getStartOfDay(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getStartOfWeek(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.MONDAY)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getStartOfMonth(): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1)
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
