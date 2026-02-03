# 🎯 Nächste Schritte - KidGuard (27. Januar 2026)

**Status-Check durchgeführt:** 27. Januar 2026, 10:00 Uhr  
**Projekt-Phase:** Woche 1 von MVP-Entwicklung

---

## ✅ Was bereits FERTIG ist (Priorität 1)

### 1.1 Unit-Tests ✅ **ABGESCHLOSSEN**
- ✅ `MLGroomingDetectorTest.kt` vorhanden
- ✅ `KidGuardEngineTest.kt` vorhanden
- ✅ `NotificationHelperTest.kt` vorhanden
- ✅ `ParentAuthManagerTest.kt` vorhanden

**Status:** 100% fertig

---

### 1.4 EncryptedSharedPreferences ✅ **ABGESCHLOSSEN**
- ✅ In `ParentAuthManager.kt` implementiert
- ✅ AES256-GCM Verschlüsselung aktiv
- ✅ Migration von alten PINs implementiert
- ✅ Tests vorhanden

**Status:** 100% fertig

---

## ❌ Was noch FEHLT (Priorität 1)

### 1.3 Room Database 🔴 **KRITISCH - JETZT STARTEN**

**Problem:**
- Room Dependencies sind auskommentiert in `app/build.gradle.kts`
- Keine `RiskEvent` Entity
- Keine Datenpersistenz für erkannte Risiken
- App vergisst alles bei Neustart

**Was du JETZT tun musst:**

#### Schritt 1: Room Dependencies aktivieren (2 Min)
```kotlin
// In app/build.gradle.kts, Zeile 84-87:
val room_version = "2.6.1"
implementation("androidx.room:room-runtime:$room_version")
implementation("androidx.room:room-ktx:$room_version")
ksp("androidx.room:room-compiler:$room_version")
```

#### Schritt 2: Dateien erstellen (45 Min)
```bash
# Erstelle diese 4 Dateien:
app/src/main/java/com/example/safespark/database/
├── RiskEvent.kt           # Entity (Datenmodell)
├── RiskEventDao.kt        # Database Access Object
├── KidGuardDatabase.kt    # Room Database
└── RiskEventRepository.kt # Repository Pattern
```

#### Schritt 3: GuardianAccessibilityService updaten (30 Min)
- Speichere erkannte Risiken in DB statt nur Log
- Nutze Repository für DB-Zugriff

**Geschätzter Zeitaufwand:** 1.5 - 2 Stunden  
**Priorität:** 🔴 HÖCHSTE

---

### 1.2 Dashboard UI 🔴 **KRITISCH - DANACH STARTEN**

**Problem:**
- MainActivity zeigt nur Onboarding/Auth
- Kein Dashboard für Eltern
- Keine Übersicht der Risiken

**Was du DANACH tun musst:**

#### Phase 1: Basis-Layout (3 Stunden)
```bash
# Erstelle diese Dateien:
app/src/main/java/com/example/safespark/ui/
├── DashboardFragment.kt
├── DashboardViewModel.kt
└── RiskEventAdapter.kt  # RecyclerView Adapter

app/src/main/res/layout/
├── fragment_dashboard.xml
└── item_risk_event.xml
```

#### Phase 2: Navigation (1 Stunde)
- MainActivity → DashboardFragment nach Auth
- DetailView für einzelne Risiken

**Geschätzter Zeitaufwand:** 4-5 Stunden  
**Priorität:** 🔴 HOCH (nach Database)

---

## 📋 KONKRETE TO-DO LISTE (27. Januar - 2. Februar)

### 🎯 HEUTE (27. Januar) - Room Database
```bash
# 1. Dependencies aktivieren
□ Editiere app/build.gradle.kts (Zeile 84-87)
□ Gradle Sync

# 2. Erstelle Database Package
□ mkdir -p app/src/main/java/com/example/safespark/database

# 3. Erstelle Dateien (in dieser Reihenfolge!)
□ RiskEvent.kt           # Datenmodell
□ RiskEventDao.kt        # SQL Queries
□ KidGuardDatabase.kt    # Room DB Setup
□ RiskEventRepository.kt # Business Logic Layer

# 4. Integration
□ GuardianAccessibilityService.kt updaten
□ Teste DB auf Emulator

# 5. Tests (Optional, aber empfohlen)
□ RiskEventDaoTest.kt erstellen
```

**Ziel heute:** Risiken werden in DB gespeichert ✅

---

### 📅 MORGEN (28. Januar) - Dashboard Layout

```bash
# 1. Erstelle UI Package
□ mkdir -p app/src/main/java/com/example/safespark/ui

# 2. Erstelle Layouts
□ fragment_dashboard.xml (Hauptübersicht)
□ item_risk_event.xml    (RecyclerView Item)

# 3. Erstelle Kotlin Files
□ DashboardFragment.kt
□ DashboardViewModel.kt
□ RiskEventAdapter.kt

# 4. Integration
□ MainActivity: Navigiere zu DashboardFragment nach Auth
□ Teste UI auf Emulator
```

**Ziel morgen:** Dashboard zeigt Risiken aus DB ✅

---

### 📅 DIESE WOCHE (29. Jan - 2. Feb) - Dashboard Features

#### Tag 3 (29. Jan): Detail-View
```bash
□ RiskDetailActivity.kt erstellen
□ activity_risk_detail.xml erstellen
□ "Details"-Button im Dashboard verlinken
```

#### Tag 4 (30. Jan): Dismiss/Ignore Feature
```bash
□ "Ignorieren"-Button implementieren
□ RiskEvent.dismissed Flag nutzen
□ Swipe-to-Dismiss (optional)
```

#### Tag 5 (31. Jan): Statistiken
```bash
□ Risiko-Zähler (Heute/Woche/Monat)
□ Chart/Graph (optional mit MPAndroidChart)
□ Per-App Statistiken
```

#### Wochenende (1-2. Feb): Polish & Testing
```bash
□ UI-Tests schreiben (Espresso)
□ End-to-End Test: Grooming-Message → Notification → Dashboard
□ Bugfixes
```

**Ziel Ende Woche:** Funktionales Dashboard ✅

---

## 🚀 SCHNELLSTART: Room Database Setup

### Datei 1: `RiskEvent.kt`
```kotlin
package safespark.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "risk_events")
data class RiskEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val timestamp: Long,
    val appPackage: String,
    val appName: String,
    val message: String,
    val riskScore: Float,
    val mlStage: String,        // "STAGE_TRUST", "STAGE_ASSESSMENT" etc.
    val keywordMatches: String, // "allein,robux" (comma-separated)
    val dismissed: Boolean = false
)
```

### Datei 2: `RiskEventDao.kt`
```kotlin
package safespark.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RiskEventDao {
    
    @Query("SELECT * FROM risk_events ORDER BY timestamp DESC")
    fun getAllEvents(): LiveData<List<RiskEvent>>
    
    @Query("SELECT * FROM risk_events WHERE dismissed = 0 ORDER BY timestamp DESC")
    fun getActiveEvents(): LiveData<List<RiskEvent>>
    
    @Query("SELECT * FROM risk_events WHERE timestamp > :since ORDER BY timestamp DESC")
    suspend fun getEventsSince(since: Long): List<RiskEvent>
    
    @Query("SELECT COUNT(*) FROM risk_events WHERE timestamp > :since AND dismissed = 0")
    suspend fun getActiveCountSince(since: Long): Int
    
    @Insert
    suspend fun insert(event: RiskEvent): Long
    
    @Update
    suspend fun update(event: RiskEvent)
    
    @Query("DELETE FROM risk_events WHERE timestamp < :before")
    suspend fun deleteOldEvents(before: Long)
}
```

### Datei 3: `KidGuardDatabase.kt`
```kotlin
package safespark.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RiskEvent::class],
    version = 1,
    exportSchema = false
)
abstract class KidGuardDatabase : RoomDatabase() {
    
    abstract fun riskEventDao(): RiskEventDao
    
    companion object {
        @Volatile
        private var INSTANCE: KidGuardDatabase? = null
        
        fun getDatabase(context: Context): KidGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KidGuardDatabase::class.java,
                    "kidguard_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
```

### Datei 4: `RiskEventRepository.kt`
```kotlin
package safespark.database

import androidx.lifecycle.LiveData

class RiskEventRepository(private val dao: RiskEventDao) {
    
    val allEvents: LiveData<List<RiskEvent>> = dao.getAllEvents()
    val activeEvents: LiveData<List<RiskEvent>> = dao.getActiveEvents()
    
    suspend fun insert(event: RiskEvent): Long {
        return dao.insert(event)
    }
    
    suspend fun dismissEvent(event: RiskEvent) {
        dao.update(event.copy(dismissed = true))
    }
    
    suspend fun getTodayCount(): Int {
        val todayStart = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
        return dao.getActiveCountSince(todayStart)
    }
    
    suspend fun getWeekCount(): Int {
        val weekStart = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
        return dao.getActiveCountSince(weekStart)
    }
    
    suspend fun cleanupOldEvents() {
        // Lösche Events älter als 30 Tage
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        dao.deleteOldEvents(thirtyDaysAgo)
    }
}
```

---

## 🔧 Integration in GuardianAccessibilityService

### Update `GuardianAccessibilityService.kt`:

```kotlin
// Am Anfang der Klasse:
private lateinit var database: KidGuardDatabase
private lateinit var repository: RiskEventRepository

// In onCreate():
override fun onCreate() {
    super.onCreate()
    database = KidGuardDatabase.getDatabase(this)
    repository = RiskEventRepository(database.riskEventDao())
    // ... existing code
}

// In onAccessibilityEvent(), wenn Risiko erkannt:
private fun handleHighRiskDetection(result: KidGuardEngine.AnalysisResult, text: String) {
    val event = RiskEvent(
        timestamp = System.currentTimeMillis(),
        appPackage = result.appPackage,
        appName = result.appName,
        message = text,
        riskScore = result.riskScore,
        mlStage = result.mlStage,
        keywordMatches = result.keywords.joinToString(",")
    )
    
    // Speichere in DB (in Coroutine)
    CoroutineScope(Dispatchers.IO).launch {
        repository.insert(event)
    }
    
    // ... existing notification code
}
```

---

## 📊 Training Scripts Status

### ✅ Was bereits funktioniert:
- ✅ `translate_dataset.py` - EN→DE Übersetzung
- ✅ `augment_data.py` - Data Augmentation  
- ✅ `train_model.py` - Training mit Class Weights
- ✅ `evaluate_model.py` - Evaluation

### 🔄 Was du tun solltest (NACH Database):

```bash
# 1. Prüfe ob deutsche Daten vorhanden
ls -lh training/data/german/

# 2. Wenn nicht: Übersetze Dataset
cd training
python translate_dataset.py

# 3. Augmentiere (falls Grooming-Klassen < 150 Samples)
python augment_data.py

# 4. Trainiere neues Modell
python train_model.py

# 5. Evaluiere
python evaluate_model.py
```

**Hinweis:** Das ML-Training ist **NICHT** kritisch für MVP!  
Aktuelles Modell (90.5% Accuracy) ist bereits gut genug.  
Fokus: **Database + Dashboard** zuerst!

---

## 🎯 Erfolgs-Kriterien für diese Woche

### Must-Have bis 2. Februar:
- ✅ Room Database implementiert
- ✅ Risiken werden gespeichert
- ✅ Dashboard zeigt Risiko-Liste
- ✅ Basic UI vorhanden (RecyclerView)

### Nice-to-Have:
- ✅ Detail-View für Risiken
- ✅ Dismiss/Ignore Feature
- ✅ Statistiken (Heute/Woche)

---

## 🚨 WICHTIG: Was NICHT tun

### ❌ Vermeide diese Ablenkungen:
- ❌ Noch kein App-Icon designen
- ❌ Noch keine ProGuard Rules
- ❌ Noch kein Multi-Device Support
- ❌ Noch kein Web-Dashboard
- ❌ Noch kein Transfer Learning (BERT)

**Grund:** Das sind Priorität 2-3 Features.  
Erst MVP fertig, dann Optimierungen!

---

## 💡 Tipps & Tricks

### Testing-Workflow:
```bash
# 1. Starte Emulator
emulator -avd Pixel_8_API_35 &

# 2. Installiere App
./gradlew installDebug

# 3. Aktiviere Accessibility
# Settings → Accessibility → KidGuard → Enable

# 4. Öffne WhatsApp/Testing-App
# Schreibe: "Bist du allein?"

# 5. Prüfe Logs
adb logcat | grep "KidGuard"

# 6. Öffne Dashboard → Sollte Risiko zeigen!
```

### Debug Database (in Android Studio):
```
View → Tool Windows → App Inspection → Database Inspector
→ Wähle kidguard_database
→ Öffne risk_events Tabelle
→ Siehst du Einträge? ✅
```

---

## 📞 Hilfe & Ressourcen

### Room Database:
- [Official Codelab](https://developer.android.com/codelabs/android-room-with-a-view-kotlin)
- [Room Documentation](https://developer.android.com/training/data-storage/room)

### RecyclerView & Adapter:
- [RecyclerView Guide](https://developer.android.com/guide/topics/ui/layout/recyclerview)
- [DiffUtil for efficient updates](https://developer.android.com/reference/androidx/recyclerview/widget/DiffUtil)

### LiveData & ViewModel:
- [ViewModel Guide](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [LiveData Overview](https://developer.android.com/topic/libraries/architecture/livedata)

---

## ✅ Definition of Done (Diese Woche)

### Database ✅:
- [ ] Room Dependencies aktiviert
- [ ] 4 Dateien erstellt (Entity, DAO, Database, Repository)
- [ ] GuardianAccessibilityService speichert in DB
- [ ] Test: Risiko-Event in DB sichtbar (App Inspection)

### Dashboard ✅:
- [ ] DashboardFragment erstellt
- [ ] RecyclerView zeigt Risiken aus DB
- [ ] MainActivity navigiert zu Dashboard nach Auth
- [ ] Test: Risiko sichtbar im Dashboard nach Detection

**Wenn alle Checkboxen ✅ → MVP Priorität 1 FERTIG! 🎉**

---

## 🎯 Zusammenfassung

### Dein Fokus JETZT:
1. **HEUTE:** Room Database (1.5-2h)
2. **MORGEN:** Dashboard Layout (3-4h)
3. **DIESE WOCHE:** Dashboard Features (8-10h)

### Warum diese Reihenfolge?
- Database = Foundation (ohne DB kein Dashboard)
- Dashboard = User sieht erstmals die App-Funktionalität
- Features = Polishing des MVP

### Nach dieser Woche:
✅ Du hast ein **funktionales MVP**  
✅ Eltern können Risiken sehen  
✅ Daten werden persistent gespeichert  
✅ App ist **nutzbar** (nicht nur Demo)

---

**Los geht's! Starte mit dem Database-Setup! 🚀**

Öffne `app/build.gradle.kts` und kommentiere die Room-Dependencies ein!

---

**Erstellt:** 27. Januar 2026, 10:00 Uhr  
**Status:** Ready to Execute  
**Nächster Check:** 31. Januar 2026 (Ende Woche 1)
