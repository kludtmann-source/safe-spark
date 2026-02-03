package com.example.safespark.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.safespark.MainActivity

/**
 * 🔔 NotificationHelper - Sendet Push-Benachrichtigungen bei RISK DETECTED
 *
 * Google Play Store Compliance:
 * - Keine Nachrichten-Inhalte werden angezeigt
 * - Nur "Risiko erkannt" + App-Name + Score
 * - Eltern-Benachrichtigung (nicht Kind)
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        private const val CHANNEL_ID = "safespark_risk_alerts"
        private const val CHANNEL_NAME = "Risiko-Warnungen"
        private const val NOTIFICATION_ID_BASE = 1000
    }

    init {
        createNotificationChannel()
    }

    /**
     * Erstellt Notification Channel (Android 8+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Benachrichtigungen bei erkannten Risiken"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Sendet Risiko-Benachrichtigung
     *
     * @param appName Name der App (z.B. "WhatsApp")
     * @param score Risk-Score (0.0 - 1.0)
     * @param timestamp Zeitpunkt der Erkennung
     */
    fun sendRiskNotification(appName: String, score: Float, timestamp: String) {
        // Intent zum Öffnen der App
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_type", "risk_detected")
            putExtra("app_name", appName)
            putExtra("score", score)
            putExtra("timestamp", timestamp)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Risiko-Level als Text
        val riskLevel = when {
            score >= 0.9 -> "Hohes Risiko"
            score >= 0.7 -> "Mittleres Risiko"
            else -> "Verdächtige Aktivität"
        }

        // Notification erstellen
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ $riskLevel erkannt")
            .setContentText("In $appName wurde ein Risiko erkannt (Score: ${String.format("%.0f", score * 100)}%)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("SafeSpark hat ein Risiko in $appName erkannt.\n\n" +
                        "Risiko-Score: ${String.format("%.0f", score * 100)}%\n" +
                        "Zeitpunkt: $timestamp\n\n" +
                        "Tippen Sie hier, um Details zu sehen."))
            .build()

        // Notification anzeigen (eindeutige ID pro App)
        val notificationId = NOTIFICATION_ID_BASE + appName.hashCode() % 1000
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Löscht alle Notifications
     */
    fun clearAllNotifications() {
        notificationManager.cancelAll()
    }
}
