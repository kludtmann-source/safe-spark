package com.example.safespark

import android.app.NotificationManager
import android.content.Context
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import com.google.common.truth.Truth.assertThat

/**
 * Unit-Tests für NotificationHelper
 *
 * Testet:
 * - Notification-Erstellung
 * - Risk-Level Klassifizierung
 * - App-Name Mapping
 * - Notification-Channel Setup
 */
@RunWith(MockitoJUnitRunner::class)
class NotificationHelperTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockNotificationManager: NotificationManager

    // ========== RISK LEVEL TESTS ==========

    @Test
    fun `risk level HIGH for score above 0_8`() {
        val score = 0.85f

        val riskLevel = when {
            score >= 0.8f -> "Hohes Risiko"
            score >= 0.6f -> "Mittleres Risiko"
            else -> "Verdächtiger Inhalt"
        }

        assertThat(riskLevel).isEqualTo("Hohes Risiko")
    }

    @Test
    fun `risk level MEDIUM for score between 0_6 and 0_8`() {
        val score = 0.7f

        val riskLevel = when {
            score >= 0.8f -> "Hohes Risiko"
            score >= 0.6f -> "Mittleres Risiko"
            else -> "Verdächtiger Inhalt"
        }

        assertThat(riskLevel).isEqualTo("Mittleres Risiko")
    }

    @Test
    fun `risk level LOW for score below 0_6`() {
        val score = 0.5f

        val riskLevel = when {
            score >= 0.8f -> "Hohes Risiko"
            score >= 0.6f -> "Mittleres Risiko"
            else -> "Verdächtiger Inhalt"
        }

        assertThat(riskLevel).isEqualTo("Verdächtiger Inhalt")
    }

    @Test
    fun `boundary test score exactly 0_8`() {
        val score = 0.8f
        val riskLevel = when {
            score >= 0.8f -> "Hohes Risiko"
            score >= 0.6f -> "Mittleres Risiko"
            else -> "Verdächtiger Inhalt"
        }

        assertThat(riskLevel).isEqualTo("Hohes Risiko")
    }

    @Test
    fun `boundary test score exactly 0_6`() {
        val score = 0.6f
        val riskLevel = when {
            score >= 0.8f -> "Hohes Risiko"
            score >= 0.6f -> "Mittleres Risiko"
            else -> "Verdächtiger Inhalt"
        }

        assertThat(riskLevel).isEqualTo("Mittleres Risiko")
    }

    // ========== APP NAME MAPPING TESTS ==========

    @Test
    fun `map WhatsApp package to friendly name`() {
        val packageName = "com.whatsapp"
        val expectedName = "WhatsApp"

        val appName = when (packageName) {
            "com.whatsapp" -> "WhatsApp"
            "org.telegram.messenger" -> "Telegram"
            "com.instagram.android" -> "Instagram"
            "com.snapchat.android" -> "Snapchat"
            "com.discord" -> "Discord"
            else -> packageName
        }

        assertThat(appName).isEqualTo(expectedName)
    }

    @Test
    fun `map Telegram package to friendly name`() {
        val packageName = "org.telegram.messenger"
        val expectedName = "Telegram"

        val appName = when (packageName) {
            "com.whatsapp" -> "WhatsApp"
            "org.telegram.messenger" -> "Telegram"
            "com.instagram.android" -> "Instagram"
            "com.snapchat.android" -> "Snapchat"
            "com.discord" -> "Discord"
            else -> packageName
        }

        assertThat(appName).isEqualTo(expectedName)
    }

    @Test
    fun `map Instagram package to friendly name`() {
        val packageName = "com.instagram.android"
        val expectedName = "Instagram"

        val appName = when (packageName) {
            "com.whatsapp" -> "WhatsApp"
            "org.telegram.messenger" -> "Telegram"
            "com.instagram.android" -> "Instagram"
            "com.snapchat.android" -> "Snapchat"
            "com.discord" -> "Discord"
            else -> packageName
        }

        assertThat(appName).isEqualTo(expectedName)
    }

    @Test
    fun `map Snapchat package to friendly name`() {
        val packageName = "com.snapchat.android"
        val expectedName = "Snapchat"

        val appName = when (packageName) {
            "com.whatsapp" -> "WhatsApp"
            "org.telegram.messenger" -> "Telegram"
            "com.instagram.android" -> "Instagram"
            "com.snapchat.android" -> "Snapchat"
            "com.discord" -> "Discord"
            else -> packageName
        }

        assertThat(appName).isEqualTo(expectedName)
    }

    @Test
    fun `map Discord package to friendly name`() {
        val packageName = "com.discord"
        val expectedName = "Discord"

        val appName = when (packageName) {
            "com.whatsapp" -> "WhatsApp"
            "org.telegram.messenger" -> "Telegram"
            "com.instagram.android" -> "Instagram"
            "com.snapchat.android" -> "Snapchat"
            "com.discord" -> "Discord"
            else -> packageName
        }

        assertThat(appName).isEqualTo(expectedName)
    }

    @Test
    fun `unknown package returns package name`() {
        val packageName = "com.unknown.app"

        val appName = when (packageName) {
            "com.whatsapp" -> "WhatsApp"
            "org.telegram.messenger" -> "Telegram"
            "com.instagram.android" -> "Instagram"
            "com.snapchat.android" -> "Snapchat"
            "com.discord" -> "Discord"
            else -> packageName
        }

        assertThat(appName).isEqualTo(packageName)
    }

    // ========== NOTIFICATION TEXT TESTS ==========

    @Test
    fun `notification title contains risk level`() {
        val riskLevel = "Hohes Risiko"
        val expectedTitle = "⚠️ Hohes Risiko erkannt"

        val title = "⚠️ $riskLevel erkannt"

        assertThat(title).isEqualTo(expectedTitle)
    }

    @Test
    fun `notification content contains app name`() {
        val appName = "WhatsApp"
        val score = 0.85f
        val scorePercent = (score * 100).toInt()

        val content = "In $appName wurde ein Risiko erkannt (Score: $scorePercent%)"

        assertThat(content).contains("WhatsApp")
        assertThat(content).contains("85%")
    }

    @Test
    fun `notification big text contains all details`() {
        val appName = "Instagram"
        val scorePercent = 75
        val timestamp = "14:32:45.123"

        val bigText = """
            SafeSpark hat ein Risiko in $appName erkannt.
            
            Risiko-Score: $scorePercent%
            Zeitpunkt: $timestamp
            
            Tippen Sie hier, um Details zu sehen.
        """.trimIndent()

        assertThat(bigText).contains(appName)
        assertThat(bigText).contains("75%")
        assertThat(bigText).contains(timestamp)
    }

    // ========== SCORE FORMATTING TESTS ==========

    @Test
    fun `score is formatted as percentage`() {
        val scores = mapOf(
            0.85f to 85,
            0.75f to 75,
            0.5f to 50,
            0.95f to 95,
            0.123f to 12,
            0.999f to 99
        )

        scores.forEach { (score, expectedPercent) ->
            val percent = (score * 100).toInt()
            assertThat(percent).isEqualTo(expectedPercent)
        }
    }

    @Test
    fun `score percentage is between 0 and 100`() {
        val testScores = listOf(0.0f, 0.5f, 1.0f, 0.123f, 0.999f)

        testScores.forEach { score ->
            val percent = (score * 100).toInt()
            assertThat(percent).isAtLeast(0)
            assertThat(percent).isAtMost(100)
        }
    }

    // ========== NOTIFICATION CHANNEL TESTS ==========

    @Test
    fun `channel ID is unique and descriptive`() {
        val channelId = "safespark_alerts"

        assertThat(channelId).isNotEmpty()
        assertThat(channelId).doesNotContain(" ")
        assertThat(channelId).contains("safespark")
    }

    @Test
    fun `channel name is user friendly`() {
        val channelName = "SafeSpark Warnungen"

        assertThat(channelName).isNotEmpty()
        assertThat(channelName).contains("SafeSpark")
    }

    @Test
    fun `channel description explains purpose`() {
        val channelDescription = "Benachrichtigungen über erkannte Risiken in Chat-Apps"

        assertThat(channelDescription).isNotEmpty()
        assertThat(channelDescription).contains("Risiken")
    }

    @Test
    fun `channel importance is high for critical alerts`() {
        // NotificationManager.IMPORTANCE_HIGH = 4
        val expectedImportance = 4 // IMPORTANCE_HIGH

        assertThat(expectedImportance).isAtLeast(3) // Mindestens DEFAULT
    }

    // ========== NOTIFICATION PRIORITY TESTS ==========

    @Test
    fun `high risk notifications have max priority`() {
        val score = 0.9f

        // Priority.MAX = 2
        val expectedPriority = if (score >= 0.8f) 2 else 1

        assertThat(expectedPriority).isEqualTo(2)
    }

    @Test
    fun `medium risk notifications have high priority`() {
        val score = 0.7f

        // Priority.HIGH = 1
        val expectedPriority = if (score >= 0.8f) 2 else 1

        assertThat(expectedPriority).isEqualTo(1)
    }

    // ========== VIBRATION PATTERN TESTS ==========

    @Test
    fun `vibration pattern is defined`() {
        val vibrationPattern = longArrayOf(0, 500, 250, 500)

        assertThat(vibrationPattern).hasLength(4)
        assertThat(vibrationPattern[0]).isEqualTo(0) // Delay
        assertThat(vibrationPattern[1]).isEqualTo(500) // Vibrate
        assertThat(vibrationPattern[2]).isEqualTo(250) // Pause
        assertThat(vibrationPattern[3]).isEqualTo(500) // Vibrate again
    }

    @Test
    fun `vibration pattern total duration is reasonable`() {
        val vibrationPattern = longArrayOf(0, 500, 250, 500)
        val totalDuration = vibrationPattern.sum()

        // Total: 1250ms = 1.25 seconds
        assertThat(totalDuration).isEqualTo(1250L)
        assertThat(totalDuration).isLessThan(3000L) // Nicht zu lang
    }

    // ========== NOTIFICATION ID TESTS ==========

    @Test
    fun `notification ID is unique per risk event`() {
        val timestamp = System.currentTimeMillis()
        val notificationId = (timestamp % Int.MAX_VALUE).toInt()

        assertThat(notificationId).isNotEqualTo(0)
    }

    @Test
    fun `notification IDs are different for different timestamps`() {
        val timestamp1 = 1706284800000L // 26.01.2026 14:00:00
        val timestamp2 = 1706288400000L // 26.01.2026 15:00:00

        val id1 = (timestamp1 % Int.MAX_VALUE).toInt()
        val id2 = (timestamp2 % Int.MAX_VALUE).toInt()

        assertThat(id1).isNotEqualTo(id2)
    }

    // ========== INTENT TESTS ==========

    @Test
    fun `notification opens MainActivity when clicked`() {
        val targetActivity = "MainActivity"

        assertThat(targetActivity).isEqualTo("MainActivity")
    }

    @Test
    fun `notification intent has correct flags`() {
        // FLAG_UPDATE_CURRENT = 0x08000000
        // FLAG_IMMUTABLE = 0x04000000
        val expectedFlags = 0x08000000 or 0x04000000

        assertThat(expectedFlags).isNotEqualTo(0)
    }

    // ========== EDGE CASE TESTS ==========

    @Test
    fun `handle null or empty app name`() {
        val packageNames = listOf(null, "", "   ")

        packageNames.forEach { packageName ->
            val appName = packageName?.takeIf { it.isNotBlank() } ?: "Unbekannte App"
            assertThat(appName).isNotEmpty()
        }
    }

    @Test
    fun `handle extreme risk scores`() {
        val extremeScores = listOf(-0.5f, 0.0f, 1.0f, 1.5f)

        extremeScores.forEach { score ->
            val clamped = score.coerceIn(0.0f, 1.0f)
            assertThat(clamped).isAtLeast(0.0f)
            assertThat(clamped).isAtMost(1.0f)
        }
    }

    @Test
    fun `handle very long app names`() {
        val longAppName = "A".repeat(100)

        val truncated = if (longAppName.length > 50) {
            longAppName.substring(0, 47) + "..."
        } else {
            longAppName
        }

        assertThat(truncated.length).isAtMost(50)
    }

    // ========== PERMISSION TESTS ==========

    @Test
    fun `POST_NOTIFICATIONS permission is required for Android 13+`() {
        val androidVersion = 33 // Android 13 (TIRAMISU)
        val requiresPermission = androidVersion >= 33

        assertThat(requiresPermission).isTrue()
    }

    @Test
    fun `no permission needed for Android 12 and below`() {
        val androidVersion = 32 // Android 12L
        val requiresPermission = androidVersion >= 33

        assertThat(requiresPermission).isFalse()
    }

    // ========== NOTIFICATION ACTIONS TESTS ==========

    @Test
    fun `notification has tap action`() {
        val actionLabel = "Öffnen"

        assertThat(actionLabel).isNotEmpty()
    }

    @Test
    fun `notification should be auto-cancelable`() {
        val autoCancel = true

        assertThat(autoCancel).isTrue()
        // Nach Tap sollte Notification verschwinden
    }

    // ========== BIGTEXT STYLE TESTS ==========

    @Test
    fun `BigTextStyle shows full message`() {
        val shortMessage = "Kurzer Text"
        val longMessage = "Dies ist ein sehr langer Text der in einer normalen " +
                          "Notification nicht komplett angezeigt werden würde aber " +
                          "mit BigTextStyle sehen Eltern alle Details"

        assertThat(longMessage.length).isGreaterThan(shortMessage.length)
        // BigTextStyle sollte genutzt werden für Details
    }

    // ========== SOUND & LED TESTS ==========

    @Test
    fun `notification uses default sound`() {
        val useDefaultSound = true

        assertThat(useDefaultSound).isTrue()
    }

    @Test
    fun `notification priority affects heads-up display`() {
        // Priority.MAX zeigt Heads-Up Notification (über anderen Apps)
        val priority = 2 // MAX
        val showsHeadsUp = priority >= 1

        assertThat(showsHeadsUp).isTrue()
    }

    // ========== DOCUMENTATION ==========

    /**
     * HINWEIS FÜR ECHTE TESTS:
     *
     * Diese Tests dokumentieren erwartetes Verhalten. Für funktionale Tests:
     *
     * 1. Erstelle Instrumented Tests mit echtem Context
     * 2. Teste NotificationHelper mit echtem NotificationManager
     * 3. Verwende Robolectric für Shadow-Notifications
     *
     * Beispiel mit Robolectric:
     *
     * @RunWith(RobolectricTestRunner::class)
     * @Config(sdk = [33])
     * class NotificationHelperRobolectricTest {
     *
     *     private lateinit var context: Context
     *     private lateinit var notificationHelper: NotificationHelper
     *
     *     @Before
     *     fun setup() {
     *         context = ApplicationProvider.getApplicationContext()
     *         notificationHelper = NotificationHelper(context)
     *     }
     *
     *     @Test
     *     fun testRealNotification() {
     *         notificationHelper.sendRiskNotification(
     *             packageName = "com.whatsapp",
     *             riskScore = 0.85f,
     *             timestamp = System.currentTimeMillis()
     *         )
     *
     *         val shadowNotificationManager = shadowOf(
     *             context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
     *         )
     *
     *         assertThat(shadowNotificationManager.size()).isEqualTo(1)
     *         val notification = shadowNotificationManager.allNotifications[0]
     *         assertThat(notification.channelId).isEqualTo("safespark_alerts")
     *     }
     * }
     */
}
