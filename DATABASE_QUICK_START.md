# ⚡ Quick Start: Room Database - JETZT loslegen!

**Erstellt:** 27. Januar 2026  
**Status:** Bereit für Implementation  
**Zeitaufwand:** 30-45 Minuten

---

## ✅ Was ich gerade für dich erstellt habe

### 1. Database Dateien (4 Stück) ✅
```
app/src/main/java/com/example/kidguard/database/
├── RiskEvent.kt           ✅ Datenmodell (Entity)
├── RiskEventDao.kt        ✅ SQL Queries
├── KidGuardDatabase.kt    ✅ Room Setup
└── RiskEventRepository.kt ✅ Business Logic
```

### 2. Integration Guide ✅
```
app/src/main/java/com/example/kidguard/database/
└── INTEGRATION_GUIDE.kt   ✅ Copy-Paste-Ready Beispiele
```

### 3. Tests ✅
```
app/src/androidTest/java/com/example/kidguard/database/
└── RiskEventDaoTest.kt    ✅ 7 Instrumented Tests
```

### 4. Build Config ✅
```
app/build.gradle.kts       ✅ Room Dependencies aktiviert
```

---

## 🚀 SOFORT-AKTION (30 Min)

### Schritt 1: Gradle Sync (1 Min)
```bash
# In Android Studio:
File → Sync Project with Gradle Files

# Warte bis "BUILD SUCCESSFUL"
```

### Schritt 2: Integration in GuardianAccessibilityService (15 Min)

Öffne `GuardianAccessibilityService.kt` und füge hinzu:

#### 2.1 Imports (oben)
```kotlin
import safespark.database.KidGuardDatabase
import safespark.database.RiskEvent
import safespark.database.RiskEventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
```

#### 2.2 Class Variables
```kotlin
class GuardianAccessibilityService : AccessibilityService() {
    
    // ... existing code ...
    
    // ✅ NEU
    private lateinit var database: KidGuardDatabase
    private lateinit var repository: RiskEventRepository
    
    // ... existing code ...
}
```

#### 2.3 onCreate() - Initialisierung
```kotlin
override fun onCreate() {
    super.onCreate()
    
    // ... existing code ...
    
    // ✅ NEU
    database = KidGuardDatabase.getDatabase(this)
    repository = RiskEventRepository(database.riskEventDao())
    Log.d(TAG, "✅ Database initialisiert")
    
    // ... existing code ...
}
```

#### 2.4 Risiko-Speicherung
Finde die Stelle wo du `sendNotification()` aufrufst und füge DAVOR ein:

```kotlin
// Wo du aktuell Risiko erkennst:
private fun handleHighRiskDetection(result: KidGuardEngine.AnalysisResult, text: String) {
    
    // ✅ NEU: In DB speichern
    val riskEvent = RiskEvent(
        timestamp = System.currentTimeMillis(),
        appPackage = result.appPackage,
        appName = result.appName,
        message = text.take(500),
        riskScore = result.riskScore,
        mlStage = result.mlStage,
        keywordMatches = result.keywords.joinToString(",")
    )
    
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val eventId = repository.insert(riskEvent)
            Log.d(TAG, "✅ Risiko in DB: ID=$eventId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ DB-Fehler", e)
        }
    }
    
    // Existing code
    sendNotification(result.appName, text, result.riskScore)
}
```

### Schritt 3: Build & Test (10 Min)
```bash
# 1. Build
./gradlew assembleDebug

# 2. Install
./gradlew installDebug

# 3. Teste auf Emulator:
# - Aktiviere AccessibilityService
# - Öffne WhatsApp/Testing-App
# - Schreibe: "Bist du allein?"

# 4. Prüfe Logs:
adb logcat | grep "RiskEventRepository"
# Sollte zeigen: "✅ Event gespeichert: ID=1"
```

### Schritt 4: Prüfe Database (5 Min)
```
Android Studio:
→ View → Tool Windows → App Inspection
→ Database Inspector
→ kidguard_database
→ risk_events
→ Solltest 1 Eintrag sehen! 🎉
```

---

## 🧪 Optional: Tests laufen lassen

```bash
# Starte Emulator
emulator -avd Pixel_8_API_35 &

# Run Database Tests
./gradlew connectedAndroidTest

# Erwartung: ✅ 7 Tests passed
```

---

## 📊 Was als nächstes?

### HEUTE ABEND (Optional):
- ✅ Database funktioniert
- ✅ Risiken werden gespeichert
- ✅ Logs zeigen Success

### MORGEN (28. Januar):
- 📱 Dashboard UI erstellen
- 📊 Risiken im Dashboard anzeigen
- 🎨 RecyclerView mit schönem Layout

---

## 💾 Quick Reference: Repository Usage

### In einem Service/Activity:
```kotlin
// Initialisierung
val database = KidGuardDatabase.getDatabase(context)
val repository = RiskEventRepository(database.riskEventDao())

// Event speichern
CoroutineScope(Dispatchers.IO).launch {
    val eventId = repository.insert(riskEvent)
}

// Events holen (für UI)
// In ViewModel/Fragment:
repository.activeEvents.observe(viewLifecycleOwner) { events ->
    // Update UI mit events
}

// Statistiken
CoroutineScope(Dispatchers.IO).launch {
    val todayCount = repository.getTodayCount()
    val weekCount = repository.getWeekCount()
}

// Event ignorieren
CoroutineScope(Dispatchers.IO).launch {
    repository.dismissEvent(event)
}
```

---

## 🐛 Troubleshooting

### Fehler: "Cannot find symbol: RiskEvent"
**Lösung:** Gradle Sync!
```bash
File → Sync Project with Gradle Files
```

### Fehler: "KSP not configured"
**Lösung:** Bereits aktiviert! Sollte nicht auftreten.

### Fehler: "Database not created"
**Lösung:** Prüfe Logs:
```bash
adb logcat | grep "KidGuardDatabase"
# Sollte zeigen: "✅ Database-Instanz erstellt"
```

### Keine Einträge in Database Inspector?
**Prüfe:**
1. Hast du GuardianAccessibilityService aktualisiert?
2. Ist AccessibilityService aktiviert?
3. Hast du eine Grooming-Message getestet?
4. Prüfe Logs: `adb logcat | grep "RiskEventRepository"`

---

## ✅ Definition of Done

- [ ] Gradle Sync erfolgreich
- [ ] GuardianAccessibilityService integriert
- [ ] Build erfolgreich
- [ ] Test: Grooming-Message erkannt
- [ ] Log: "✅ Event gespeichert: ID=1"
- [ ] Database Inspector zeigt Eintrag

**Wenn alle ✅ → Priorität 1.3 FERTIG! 🎉**

---

## 📞 Nächste Schritte

Siehe: `NAECHSTE_SCHRITTE_27_JAN.md`

**Fokus morgen:** Dashboard UI erstellen!

---

**LOS GEHT'S! 🚀**

Öffne Android Studio und starte mit Schritt 1: Gradle Sync!
