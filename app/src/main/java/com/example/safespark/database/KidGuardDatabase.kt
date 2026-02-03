package com.example.safespark.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.util.Log

/**
 * 🗄️ SafeSpark Room Database
 *
 * Zentrale Datenbank für die App.
 * Aktuell nur eine Tabelle: risk_events
 *
 * Features:
 * - Thread-safe Singleton Pattern
 * - Automatische Migration (fallbackToDestructiveMigration)
 * - Lazy Initialization
 *
 * Version History:
 * - v1: Initial Schema (RiskEvent Entity)
 *
 * TODO für Production:
 * - [ ] Migration Strategy statt fallbackToDestructiveMigration
 * - [ ] Verschlüsselung mit SQLCipher (für message-Feld)
 * - [ ] exportSchema = true + Versionskontrolle
 */
@Database(
    entities = [RiskEvent::class],
    version = 1,
    exportSchema = false // TODO: In Production = true
)
abstract class KidGuardDatabase : RoomDatabase() {

    /**
     * Zugriff auf RiskEvent DAO
     */
    abstract fun riskEventDao(): RiskEventDao

    companion object {

        private const val TAG = "SafeSparkDatabase"
        private const val DATABASE_NAME = "safespark_database"

        /**
         * Singleton Instance
         * @Volatile = sichtbar für alle Threads
         */
        @Volatile
        private var INSTANCE: KidGuardDatabase? = null

        /**
         * 🔐 Thread-safe Database Instance
         *
         * Verwendet Double-Checked Locking Pattern:
         * 1. Prüfe ob INSTANCE null (ohne Lock für Performance)
         * 2. Falls null: synchronized Block für Thread-Safety
         * 3. Prüfe nochmal (könnte von anderem Thread erstellt worden sein)
         * 4. Falls immer noch null: Erstelle Database
         *
         * @param context Application Context (wird automatisch verwendet)
         * @return SafeSparkDatabase Singleton
         */
        fun getDatabase(context: Context): KidGuardDatabase {
            // Schneller Pfad: Wenn bereits initialisiert
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }

            // Langsamer Pfad: Erstelle Database (synchronized für Thread-Safety)
            synchronized(this) {
                // Double-Check: Könnte von anderem Thread erstellt worden sein
                val instance = INSTANCE
                if (instance != null) {
                    return instance
                }

                Log.d(TAG, "🔨 Erstelle neue Database-Instanz...")

                val newInstance = Room.databaseBuilder(
                    context.applicationContext,
                    KidGuardDatabase::class.java,
                    DATABASE_NAME
                )
                .fallbackToDestructiveMigration() // ⚠️ Löscht bei Schema-Änderung!
                .build()

                INSTANCE = newInstance
                Log.d(TAG, "✅ Database-Instanz erstellt")

                return newInstance
            }
        }

        /**
         * 🧪 Für Tests: Database-Instanz zurücksetzen
         * Damit jeder Test eine frische DB bekommt
         */
        fun resetInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
                Log.d(TAG, "🔄 Database-Instanz zurückgesetzt")
            }
        }
    }
}
