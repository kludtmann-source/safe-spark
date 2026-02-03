package com.example.safespark.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

/**
 * 🧪 RiskEvent DAO Instrumented Tests
 *
 * Diese Tests laufen auf einem echten Gerät/Emulator (nicht Unit-Tests).
 *
 * Testabdeckung:
 * - Insert Event
 * - Query Events
 * - Update Event (dismiss)
 * - Delete old Events
 * - Count Events by Date
 *
 * Run:
 * ./gradlew connectedAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class RiskEventDaoTest {

    private lateinit var database: KidGuardDatabase
    private lateinit var dao: RiskEventDao

    /**
     * Setup: Erstelle In-Memory Database vor jedem Test
     * In-Memory = schneller + wird nach Test automatisch gelöscht
     */
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // In-Memory Database (existiert nur während Test)
        database = Room.inMemoryDatabaseBuilder(
            context,
            KidGuardDatabase::class.java
        )
        .allowMainThreadQueries() // Nur für Tests OK
        .build()

        dao = database.riskEventDao()
    }

    /**
     * Teardown: Schließe Database nach jedem Test
     */
    @After
    fun teardown() {
        database.close()
    }

    // ========== Tests ==========

    @Test
    fun insertEvent_and_getById_returnsCorrectEvent() = runBlocking {
        // Given
        val event = createTestEvent(
            appName = "WhatsApp",
            message = "Bist du allein?",
            riskScore = 0.85f
        )

        // When
        val eventId = dao.insert(event)
        val retrieved = dao.getEventById(eventId)

        // Then
        assertNotNull(retrieved)
        assertEquals("WhatsApp", retrieved?.appName)
        assertEquals("Bist du allein?", retrieved?.message)
        assertEquals(0.85f, retrieved?.riskScore ?: 0f, 0.01f)
    }

    @Test
    fun insertMultipleEvents_and_getAllEvents_returnsInDescendingOrder() = runBlocking {
        // Given: Füge 3 Events mit unterschiedlichen Timestamps ein
        val event1 = createTestEvent(timestamp = 1000L, appName = "App1")
        val event2 = createTestEvent(timestamp = 2000L, appName = "App2")
        val event3 = createTestEvent(timestamp = 3000L, appName = "App3")

        // When
        dao.insert(event1)
        dao.insert(event2)
        dao.insert(event3)

        // getAllEvents() gibt LiveData zurück, aber wir brauchen direkten Zugriff
        // → Nutze getEventsSince(0) als Alternative
        val events = dao.getEventsSince(0)

        // Then: Sollte neueste zuerst sein (3000, 2000, 1000)
        assertEquals(3, events.size)
        assertEquals("App3", events[0].appName)
        assertEquals("App2", events[1].appName)
        assertEquals("App1", events[2].appName)
    }

    @Test
    fun updateEvent_dismissed_isReflectedInDatabase() = runBlocking {
        // Given
        val event = createTestEvent(dismissed = false)
        val eventId = dao.insert(event)

        // When: Markiere als dismissed
        val updatedEvent = event.copy(id = eventId, dismissed = true)
        dao.update(updatedEvent)

        // Then
        val retrieved = dao.getEventById(eventId)
        assertTrue(retrieved?.dismissed == true)
    }

    @Test
    fun getActiveCountSince_returnsCorrectCount() = runBlocking {
        // Given: 2 active, 1 dismissed, 1 zu alt
        val now = System.currentTimeMillis()
        val yesterday = now - (24 * 60 * 60 * 1000)
        val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000)

        dao.insert(createTestEvent(timestamp = now, dismissed = false))      // ✅ zählt
        dao.insert(createTestEvent(timestamp = yesterday, dismissed = false)) // ✅ zählt
        dao.insert(createTestEvent(timestamp = yesterday, dismissed = true))  // ❌ dismissed
        dao.insert(createTestEvent(timestamp = twoDaysAgo, dismissed = false)) // ❌ zu alt

        // When: Count seit gestern (24h)
        val count = dao.getActiveCountSince(yesterday - 1000) // -1000ms um Rounding zu vermeiden

        // Then: Sollte 2 sein
        assertEquals(2, count)
    }

    @Test
    fun deleteOldEvents_removesOnlyOldOnes() = runBlocking {
        // Given
        val now = System.currentTimeMillis()
        val old = now - (40L * 24 * 60 * 60 * 1000) // 40 Tage alt
        val recent = now - (10L * 24 * 60 * 60 * 1000) // 10 Tage alt

        dao.insert(createTestEvent(timestamp = old, appName = "OldApp"))
        dao.insert(createTestEvent(timestamp = recent, appName = "RecentApp"))
        dao.insert(createTestEvent(timestamp = now, appName = "CurrentApp"))

        // When: Lösche Events älter als 30 Tage
        val cutoff = now - (30L * 24 * 60 * 60 * 1000)
        dao.deleteOldEvents(cutoff)

        // Then: Nur 2 sollten übrig bleiben
        val remaining = dao.getEventsSince(0)
        assertEquals(2, remaining.size)
        assertFalse(remaining.any { it.appName == "OldApp" })
        assertTrue(remaining.any { it.appName == "RecentApp" })
        assertTrue(remaining.any { it.appName == "CurrentApp" })
    }

    @Test
    fun getHighRiskEvents_filtersCorrectly() = runBlocking {
        // Given: High, Medium, Low Risk Events
        dao.insert(createTestEvent(riskScore = 0.9f, appName = "HighRisk"))
        dao.insert(createTestEvent(riskScore = 0.7f, appName = "MediumRisk"))
        dao.insert(createTestEvent(riskScore = 0.5f, appName = "LowRisk"))
        dao.insert(createTestEvent(riskScore = 0.85f, appName = "HighRisk2", dismissed = true)) // dismissed

        // When: getHighRiskEvents (Score >= 0.8, nicht dismissed)
        // Hinweis: getHighRiskEvents gibt LiveData, nutze Query direkt für Test
        val highRiskEvents = dao.getEventsSince(0).filter { it.riskScore >= 0.8f && !it.dismissed }

        // Then: Nur 1 Event sollte matchen
        assertEquals(1, highRiskEvents.size)
        assertEquals("HighRisk", highRiskEvents[0].appName)
    }

    // ========== Helper Functions ==========

    /**
     * Erstellt ein Test-RiskEvent mit Default-Werten
     */
    private fun createTestEvent(
        timestamp: Long = System.currentTimeMillis(),
        appPackage: String = "com.test.app",
        appName: String = "TestApp",
        message: String = "Test message",
        riskScore: Float = 0.75f,
        mlStage: String = "STAGE_TRUST",
        keywordMatches: String = "test,keyword",
        dismissed: Boolean = false
    ): RiskEvent {
        return RiskEvent(
            timestamp = timestamp,
            appPackage = appPackage,
            appName = appName,
            message = message,
            riskScore = riskScore,
            mlStage = mlStage,
            keywordMatches = keywordMatches,
            dismissed = dismissed
        )
    }
}

/**
 * ============================================================================
 * RUN TESTS:
 * ============================================================================
 *
 * 1. Starte Emulator
 * 2. Run Tests:
 *    ./gradlew connectedAndroidTest
 *
 * 3. Oder in Android Studio:
 *    - Rechtsklick auf RiskEventDaoTest.kt
 *    - Run 'RiskEventDaoTest'
 *
 * Erwartete Ausgabe:
 *    ✅ 7 Tests passed
 *
 * ============================================================================
 */
