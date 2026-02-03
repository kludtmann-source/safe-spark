# 🚀 APP STARTEN - SCHRITT FÜR SCHRITT

**Datum:** 28. Januar 2026, 01:10 Uhr  
**Status:** Bereit zum Start!

---

## ⚡ SCHNELLSTART (2 Minuten)

### In Android Studio:

1. **Öffne Android Studio** (falls nicht offen)

2. **Öffne Projekt:** KidGuard  
   (Falls noch nicht geladen)

3. **Drücke:** `Shift + F10`  
   **ODER** Klicke das grüne **▶️ Play-Icon** in der Toolbar

4. **Wähle Device:** Pixel 8 API 35  
   (Emulator startet automatisch, falls nicht läuft)

5. **Warte:** App wird gebaut, installiert und gestartet

6. **Fertig!** App läuft auf Emulator 🎉

---

## 📱 DETAILLIERTE ANLEITUNG

### Schritt 1: Android Studio öffnen

**Mac Dock:** Klicke Android Studio Icon

**ODER Terminal:**
```bash
open -a "Android Studio"
```

**ODER Finder:**
```
Applications → Android Studio
```

---

### Schritt 2: Projekt öffnen (falls nötig)

**Falls KidGuard nicht geöffnet ist:**

```
File → Open
→ Navigiere zu: /Users/knutludtmann/AndroidStudioProjects/KidGuard
→ OK
```

**Warte auf:**
- ✅ Project Tree lädt
- ✅ Gradle Sync (falls läuft)
- ✅ Indexing abgeschlossen

---

### Schritt 3: Emulator auswählen/starten

**In der Toolbar (oben):**

```
Device Selector (Dropdown) → Wähle: Pixel 8 API 35
```

**Falls Emulator nicht läuft:**

```
Device Manager (Icon rechts in Toolbar)
→ Pixel 8 API 35
→ ▶️ Play Button
→ Warte ~30 Sekunden bis Emulator bootet
```

---

### Schritt 4: App starten

**Methode 1: Keyboard (SCHNELLSTE)**
```
Shift + F10
```

**Methode 2: Toolbar**
```
Klicke grünes ▶️ Play-Icon (neben Device Selector)
```

**Methode 3: Menu**
```
Run → Run 'app'
```

**Methode 4: Rechtsklick**
```
Rechtsklick auf "app" in Project Tree
→ Run 'app'
```

---

### Schritt 5: Warten auf Installation

**Du siehst jetzt:**

```
⏳ Launching 'app' on Pixel 8 API 35
⏳ Installing APK...
⏳ $ adb install-multiple...
✅ Installation finished
🚀 Launching activity...
✅ App started
```

**Dauer:** ~10-30 Sekunden (beim ersten Mal länger)

---

### Schritt 6: App ist gestartet! ✅

**Auf dem Emulator siehst du jetzt:**

Die KidGuard App öffnet sich!

**Je nach Status:**
- Onboarding-Screen (erste Installation)
- PIN-Setup (noch keine PIN)
- Dashboard (alles konfiguriert)

---

## 🧪 TESTEN: Database-Integration

### Nach App-Start:

#### 1. AccessibilityService aktivieren

**Auf Emulator:**
```
1. Öffne: Settings (⚙️)
2. Scrolle zu: Accessibility
3. Finde: KidGuard
4. Toggle: ON (Schalter nach rechts)
5. Bestätige Dialog: OK
```

**Alternative:**
```
Settings → Suche "KidGuard" → KidGuard → ON
```

---

#### 2. Logcat öffnen (Android Studio)

**Während App läuft:**

```
View → Tool Windows → Logcat
```

**ODER:**
```
Klicke "Logcat" Tab unten in Android Studio
```

**Filter setzen:**
```
Im Suchfeld oben: KidGuard
```

**Du solltest sehen:**
```
D/GuardianAccessibility: ✅ Service erstellt
D/GuardianAccessibility: 🔔 Notifications AKTIVIERT
D/GuardianAccessibility: 💾 Database INITIALISIERT ← ✅ WICHTIG!
W/GuardianAccessibility: 🎉 onServiceConnected() - Service AKTIV!
```

---

#### 3. Teste mit Grooming-Message

**Auf Emulator:**

**Option 1: WhatsApp (falls installiert)**
```
1. Öffne WhatsApp
2. Öffne Chat
3. Schreibe: "Bist du allein?"
4. Sende ab
```

**Option 2: Messages App**
```
1. Öffne Messages
2. Neuer Chat
3. Schreibe: "Bist du allein?"
4. Sende ab
```

**Option 3: Testing in beliebiger App**
```
Öffne Chrome, Notizen, etc.
Schreibe irgendwo: "Bist du allein?"
```

---

#### 4. Prüfe Logs (Logcat)

**Nach dem Senden solltest du sehen:**

```
W/GuardianAccessibility: ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
W/GuardianAccessibility: [HH:mm:ss] 🚨 RISK DETECTED! (ML-Enhanced)
W/GuardianAccessibility: [HH:mm:ss] ⚠️ Score: 0.85
W/GuardianAccessibility: [HH:mm:ss] ⚠️ Quelle: com.whatsapp
W/GuardianAccessibility: [HH:mm:ss] 📝 Text: 'Bist du allein?...'
W/GuardianAccessibility: ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
D/GuardianAccessibility: 💾 RiskEvent gespeichert in DB (ID: 1) ← ✅ DAS IST NEU!
W/GuardianAccessibility: 🔔 Notification gesendet für: WhatsApp
```

**Wenn du das siehst: 🎉 DATABASE INTEGRATION FUNKTIONIERT!**

---

#### 5. Prüfe Database Inspector

**In Android Studio (während App läuft):**

```
1. View → Tool Windows → App Inspection
2. Tab: Database Inspector (oben)
3. Wähle: kidguard_database (links in der Liste)
4. Klicke: risk_events (Tabelle aufklappen)
5. Du solltest Einträge sehen!
```

**Erwarteter Eintrag:**
```
id: 1
timestamp: 1738012345678
appPackage: com.whatsapp
appName: WhatsApp
message: Bist du allein?
riskScore: 0.85
mlStage: STAGE_ASSESSMENT
keywordMatches: 
dismissed: 0 (false)
```

**Wenn du den Eintrag siehst: 🎊 ALLES FUNKTIONIERT PERFEKT!**

---

#### 6. Prüfe Notification

**Auf Emulator:**

Ziehe die Notification-Leiste nach unten.

**Du solltest sehen:**
```
🚨 KidGuard Alert
WhatsApp: Mögliches Grooming erkannt
Score: 0.85
```

**Wenn Notification da ist: ✅ Notifications funktionieren!**

---

## 🐛 TROUBLESHOOTING

### Problem: "No devices available"

**Lösung:**
```
1. Device Manager → Pixel 8 API 35 → Play
2. Warte 30 Sekunden bis Emulator bootet
3. Nochmal: Shift+F10
```

---

### Problem: "Build failed"

**Lösung:**
```
1. Build → Clean Project
2. Build → Rebuild Project
3. Shift+F10
```

---

### Problem: App startet, aber Accessibility funktioniert nicht

**Lösung:**
```
1. Settings → Apps → KidGuard → Force Stop
2. Settings → Accessibility → KidGuard → OFF
3. Toggle wieder ON
4. Bestätige Dialog
5. Teste nochmal
```

---

### Problem: Keine Logs in Logcat

**Lösung:**
```
1. Prüfe Filter: "KidGuard" (groß geschrieben!)
2. Prüfe Log-Level: "Debug" (nicht "Error")
3. Prüfe Device: Richtiger Emulator ausgewählt?
4. App neu starten: Shift+F10
```

---

### Problem: Database Inspector zeigt nichts

**Lösung:**
```
1. Prüfe ob App läuft (grüner Punkt in Toolbar)
2. App Inspection → Refresh Button (⟳)
3. Falls leer: Sende nochmal Test-Message
4. Prüfe Logcat: "💾 RiskEvent gespeichert" erschienen?
```

---

### Problem: AccessibilityService schaltet sich ab

**Lösung:**
```
1. Settings → Battery → KidGuard → Unrestricted
2. Settings → Accessibility → KidGuard → Toggle ON
3. Teste nochmal
```

---

## ✅ ERFOLGS-CHECKLISTE

**Nach erfolgreichem Test hake ab:**

- [ ] ✅ Android Studio geöffnet
- [ ] ✅ KidGuard Projekt geladen
- [ ] ✅ Emulator läuft (Pixel 8 API 35)
- [ ] ✅ App gestartet (Shift+F10)
- [ ] ✅ AccessibilityService aktiviert
- [ ] ✅ Logcat zeigt: "💾 Database INITIALISIERT"
- [ ] ✅ Test-Message gesendet: "Bist du allein?"
- [ ] ✅ Logcat zeigt: "🚨 RISK DETECTED!"
- [ ] ✅ Logcat zeigt: "💾 RiskEvent gespeichert in DB (ID: 1)" ← ✅ KRITISCH!
- [ ] ✅ Database Inspector zeigt Eintrag in risk_events
- [ ] ✅ Notification erscheint auf Emulator

**Wenn ALLE ✅ → PRIORITÄT 1.3 (ROOM DATABASE) FERTIG! 🎉**

---

## 📊 Was du erreicht hast

### Wenn alle Tests erfolgreich:

```
✅ Priorität 1.1: Unit-Tests (100%)
✅ Priorität 1.2: Dashboard UI (0% - MORGEN)
✅ Priorität 1.3: Room Database (100%) ← HEUTE FERTIG!
✅ Priorität 1.4: EncryptedSharedPreferences (100%)

GESAMT: 75% MVP FERTIG!
```

**Nach Dashboard UI morgen: 100% MVP! 🎊**

---

## 🎯 NÄCHSTE SCHRITTE

### HEUTE (nach erfolgreichem Test):
- [x] ✅ Room Database Code erstellt
- [x] ✅ GuardianAccessibilityService integriert
- [x] ✅ Build erfolgreich
- [ ] ⏳ App gestartet (JETZT)
- [ ] ⏳ Test erfolgreich (JETZT)
- [ ] ⏳ Database Inspector Verifikation (JETZT)

### MORGEN (29. Januar):
- [ ] Dashboard UI erstellen
- [ ] DashboardFragment.kt
- [ ] RecyclerView mit RiskEvent-Liste
- [ ] LiveData aus repository.activeEvents
- [ ] Navigation von MainActivity

### ÜBERMORGEN (30. Januar):
- [ ] Detail-View für Risiken
- [ ] "Ignorieren"-Button (dismiss)
- [ ] Statistiken (Heute/Woche/Monat)
- [ ] Polish & Bugfixes

---

## 💡 TIPPS

### Schneller Testen:
```
1. Emulator immer laufen lassen
2. Nach Code-Änderungen: Shift+F10 (Instant Run)
3. Logs im Blick: Logcat immer offen
4. Database Inspector: Live-Updates bei Refresh
```

### Effizientes Debugging:
```
1. Breakpoints setzen (Klick auf Zeilennummer)
2. Debug statt Run: Shift+F9
3. Logcat Filter nutzen: "KidGuard"
4. Database Inspector für Daten-Verifikation
```

---

## 🎊 ZUSAMMENFASSUNG

### So startest du die App:

**KURZ:**
```
Shift + F10 in Android Studio
```

**LANG:**
```
1. Öffne Android Studio
2. Wähle Device: Pixel 8 API 35
3. Shift+F10
4. Warte auf Installation
5. App startet!
```

**Dann testen:**
```
1. AccessibilityService aktivieren
2. "Bist du allein?" schreiben
3. Logs prüfen: "💾 RiskEvent gespeichert"
4. Database Inspector: Eintrag sehen
```

---

**LOS GEHT'S! ÖFFNE ANDROID STUDIO UND DRÜCKE SHIFT+F10! 🚀**

---

**Erstellt:** 28. Januar 2026, 01:10 Uhr  
**Status:** ✅ ANLEITUNG KOMPLETT  
**Nächster Schritt:** Android Studio → Shift+F10
