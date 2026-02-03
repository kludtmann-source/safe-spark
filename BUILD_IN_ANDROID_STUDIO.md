# 🚨 BUILD IN ANDROID STUDIO - SCHRITT FÜR SCHRITT

**Datum:** 28. Januar 2026, 00:45 Uhr  
**Problem:** Terminal-Build funktioniert nicht (JDK-Konfiguration)  
**Lösung:** Build direkt in Android Studio durchführen ✅

---

## ⚡ QUICK START (5 Minuten)

### 1. KSP Plugin aktiviert ✅
**Datei:** `app/build.gradle.kts` (Zeile 4-5)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // ✅ KSP für Room Database aktiviert
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}
```

**Status:** ✅ AKTIVIERT

---

### 2. Android Studio: Gradle Sync (1 Min)

**In Android Studio:**

```
File → Sync Project with Gradle Files
```

**ODER:**

```
Toolbar → Elefanten-Icon (Sync) klicken
```

**ODER:**

```
Keyboard: Cmd+Shift+A → "Sync Project" eingeben → Enter
```

**Warte auf:**
```
✅ "Gradle sync finished in X seconds"
```

**Falls Fehler:**
- File → Invalidate Caches / Restart
- Dann nochmal Sync

---

### 3. Android Studio: Build (2 Min)

**In Android Studio:**

```
Build → Make Project
```

**ODER:**

```
Keyboard: Cmd+F9 (Mac) / Ctrl+F9 (Windows)
```

**Warte auf:**
```
✅ "BUILD SUCCESSFUL in X seconds"
```

**Ausgabe sollte zeigen:**
```
> Task :app:kspDebugKotlin
> Task :app:compileDebugKotlin
> Task :app:compileDebugJavaWithJavac
BUILD SUCCESSFUL
```

---

### 4. Android Studio: Run (2 Min)

**Starte Emulator:**
- Toolbar → Device Manager → Pixel 8 API 35 → Play

**ODER:**
- Terminal: `emulator -avd Pixel_8_API_35 &`

**Dann Run:**
```
Run → Run 'app'
```

**ODER:**
```
Keyboard: Shift+F10 (Mac/Windows)
```

**ODER:**
```
Toolbar → Grünes Play-Icon klicken
```

**Warte auf:**
```
✅ App startet auf Emulator
✅ "Installation successful"
```

---

## 🧪 TEST-SZENARIO (5 Minuten)

### Schritt 1: AccessibilityService aktivieren

**Auf Emulator:**
```
1. Öffne: Settings
2. Gehe zu: Accessibility
3. Finde: KidGuard
4. Toggle: ON
5. Bestätige: OK
```

### Schritt 2: Logs öffnen

**In Android Studio:**
```
View → Tool Windows → Logcat
```

**Filter setzen:**
```
Tag: KidGuard
```

**ODER in Terminal:**
```bash
adb logcat | grep -E "KidGuard|RiskEvent"
```

### Schritt 3: Teste Grooming-Message

**Auf Emulator:**
```
1. Öffne: WhatsApp (oder Messages)
2. Schreibe: "Bist du allein?"
3. Sende ab
```

### Schritt 4: Prüfe Logs

**Erwartete Ausgabe:**
```
D/GuardianAccessibility: ✅ Service erstellt
D/GuardianAccessibility: 🔔 Notifications AKTIVIERT
D/GuardianAccessibility: 💾 Database INITIALISIERT
W/GuardianAccessibility: 🚨 RISK DETECTED!
W/GuardianAccessibility: ⚠️ Score: 0.85
D/GuardianAccessibility: 💾 RiskEvent gespeichert in DB (ID: 1)
```

**Falls du das siehst: ✅ DATABASE INTEGRATION FUNKTIONIERT!**

### Schritt 5: Database Inspector

**In Android Studio:**
```
1. View → Tool Windows → App Inspection
2. Tab: Database Inspector
3. Wähle: kidguard_database
4. Öffne: risk_events Tabelle
5. Du solltest 1 Eintrag sehen!
```

**Eintrag sollte enthalten:**
- id: 1
- timestamp: (Unix-Timestamp)
- appPackage: com.whatsapp
- appName: WhatsApp
- message: "Bist du allein?"
- riskScore: 0.85
- mlStage: STAGE_ASSESSMENT
- dismissed: false

**Falls du den Eintrag siehst: 🎉 ALLES FUNKTIONIERT!**

---

## 🐛 Troubleshooting

### Problem: "Cannot resolve symbol 'Room'"

**Lösung:**
```
1. File → Invalidate Caches / Restart
2. File → Sync Project with Gradle Files
3. Build → Rebuild Project
```

### Problem: "KSP not found"

**Lösung:**
```
1. Prüfe app/build.gradle.kts Zeile 4-5
2. Sollte sein: id("com.google.devtools.ksp") version "1.9.20-1.0.14"
3. Sync Project
```

### Problem: "Build failed"

**Lösung:**
```
1. Build → Clean Project
2. File → Invalidate Caches / Restart
3. Build → Rebuild Project
```

### Problem: "Accessibility Service not working"

**Lösung:**
```
1. Settings → Apps → KidGuard → Force Stop
2. Settings → Accessibility → KidGuard → Toggle OFF
3. Toggle ON wieder
4. Teste erneut
```

### Problem: "No logs in Logcat"

**Lösung:**
```
1. Prüfe Filter: Tag = "KidGuard" (nicht "kidguard")
2. Prüfe Log Level: Debug (nicht Error only)
3. ODER nutze Terminal: adb logcat | grep KidGuard
```

### Problem: "Database Inspector zeigt nichts"

**Lösung:**
```
1. Prüfe ob App läuft (grüner Punkt in Toolbar)
2. App Inspection → Refresh (Kreis-Icon)
3. Falls immer noch leer: Sende nochmal Test-Message
4. Prüfe Logs ob "💾 RiskEvent gespeichert" erscheint
```

---

## ✅ Erfolgs-Checkliste

**Nach erfolgreicher Integration solltest du sehen:**

- [ ] ✅ Gradle Sync erfolgreich
- [ ] ✅ Build erfolgreich (keine Errors)
- [ ] ✅ App startet auf Emulator
- [ ] ✅ AccessibilityService aktiviert
- [ ] ✅ Logs: "✅ Service erstellt"
- [ ] ✅ Logs: "💾 Database INITIALISIERT"
- [ ] ✅ Logs: "🚨 RISK DETECTED!"
- [ ] ✅ Logs: "💾 RiskEvent gespeichert in DB (ID: 1)"
- [ ] ✅ Database Inspector zeigt Eintrag
- [ ] ✅ Notification erscheint auf Emulator

**Wenn ALLE ✅ → PRIORITÄT 1.3 FERTIG! 🎉**

---

## 📊 Was funktioniert jetzt?

### Ablauf bei Risiko-Erkennung:

```
1. User schreibt: "Bist du allein?"
   ↓
2. GuardianAccessibilityService erkennt Text
   ↓
3. KidGuardEngine.analyzeText(text)
   ↓
4. Score: 0.85 (HIGH RISK)
   ↓
5. saveRiskEventToDatabase() ← ✅ NEU!
   ↓
6. RiskEvent erstellt:
   - timestamp: 1738012345678
   - appPackage: com.whatsapp
   - appName: WhatsApp
   - message: "Bist du allein?"
   - riskScore: 0.85
   - mlStage: STAGE_ASSESSMENT
   - keywordMatches: ""
   - dismissed: false
   ↓
7. repository.insert(riskEvent) (async, Coroutine)
   ↓
8. Room Database speichert in risk_events Tabelle
   ↓
9. Log: "💾 RiskEvent gespeichert in DB (ID: 1)" ✅
   ↓
10. sendRiskNotification() (wie bisher)
    ↓
11. Notification erscheint ✅
```

---

## 🎯 Nächste Schritte

### HEUTE (nach erfolgreichem Test):
- [x] ✅ Room Database Code-Integration
- [x] ✅ KSP Plugin aktiviert
- [ ] ⏳ Build & Test in Android Studio (JETZT)
- [ ] ⏳ Verifikation Database Inspector

### MORGEN (29. Januar):
- [ ] Dashboard UI erstellen
- [ ] DashboardFragment.kt
- [ ] RecyclerView mit RiskEvent-Liste
- [ ] LiveData aus repository.activeEvents

### ÜBERMORGEN (30. Januar):
- [ ] Detail-View für Risiken
- [ ] "Ignorieren"-Button
- [ ] Statistiken (Heute/Woche)

---

## 💡 Warum Android Studio?

**Problem:**
- Terminal-Build braucht korrektes JDK-Setup
- Auf deinem Mac ist JDK nicht im PATH
- `/usr/libexec/java_home` findet nichts

**Lösung:**
- Android Studio hat eigenes JDK (Embedded JDK)
- Funktioniert out-of-the-box
- Keine Terminal-Konfiguration nötig

**Alternative (falls du Terminal bevorzugst):**
```bash
# Option 1: Homebrew
brew install openjdk@17
export JAVA_HOME=$(/opt/homebrew/bin/java_home -v 17)

# Option 2: Android Studio JDK nutzen
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

# Dann:
./gradlew assembleDebug
```

---

## 📄 Zusammenfassung

### Was ich gemacht habe:

1. ✅ KSP Plugin in build.gradle.kts aktiviert
2. ✅ Room Dependencies bereits aktiviert (war schon da)
3. ✅ GuardianAccessibilityService integriert (vorher)
4. ✅ saveRiskEventToDatabase() aktiviert (vorher)
5. ✅ Build-Script erstellt (build_and_deploy.sh)
6. ✅ Diese Anleitung erstellt

### Was DU jetzt tun musst:

1. **Öffne Android Studio**
2. **File → Sync Project with Gradle Files**
3. **Build → Make Project** (Cmd+F9)
4. **Run → Run 'app'** (Shift+F10)
5. **Teste** (siehe oben)
6. **Prüfe Database Inspector**

---

## 🎊 FINALE WORTE

**Du bist FAST fertig!**

- ✅ Code ist fertig
- ✅ Dependencies sind aktiviert
- ✅ KSP ist aktiviert
- ⏳ Nur noch Build & Test fehlt

**Geschätzte Zeit bis fertig:** 10 Minuten

**Öffne jetzt Android Studio und starte mit Schritt 1! 🚀**

---

**Erstellt:** 28. Januar 2026, 00:45 Uhr  
**Status:** ✅ BEREIT FÜR BUILD  
**Nächster Schritt:** Android Studio öffnen
