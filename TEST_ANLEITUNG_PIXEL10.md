# 📱 KidGuard ML-Modell auf Pixel 10 testen - Schritt für Schritt

**Datum:** 28. Januar 2026  
**Device:** Pixel 10 Pro (Serial: 56301FDCR006BT)  
**Status:** ✅ Verbunden & bereit

---

## 🚀 SCHNELLSTART (5 Minuten)

### Option 1: Mit Android Studio (EMPFOHLEN) ⭐

```
1. Android Studio öffnen
2. Device Selector → Pixel 10 (56301FDCR006BT) wählen
3. Shift+F10 drücken
4. App wird gebaut, installiert & gestartet
```

### Option 2: Mit Script

```bash
./install_on_pixel10.sh
```

---

## 📋 DETAILLIERTE TEST-ANLEITUNG

### Phase 1: Installation (2 Min)

#### In Android Studio:

1. **Öffne Android Studio**
   - Projekt sollte bereits geladen sein

2. **Wähle Pixel 10 als Target**
   ```
   Toolbar oben → Device Selector
   → Pixel 10 (56301FDCR006BT) ← Klick hier!
   ```

3. **Baue & Installiere**
   ```
   Shift+F10 (Run)
   
   ODER
   
   Grünes ▶️ Play-Icon klicken
   ```

4. **Warte auf Installation**
   ```
   ⏳ Building APK...
   ⏳ Installing APK on 56301FDCR006BT...
   ✅ Installation successful
   🚀 Launching safespark.MainActivity...
   ✅ App started
   ```

---

### Phase 2: AccessibilityService aktivieren (2 Min)

**Auf dem Pixel 10:**

1. **Öffne Einstellungen**
   ```
   Zahnrad-Icon → Einstellungen
   ```

2. **Gehe zu Eingabehilfen**
   ```
   Einstellungen → Eingabehilfe (Accessibility)
   
   ODER
   
   Suche: "Eingabehilfe" in Settings-Suche
   ```

3. **Finde KidGuard**
   ```
   Scrolle runter zu "Heruntergeladene Dienste"
   → KidGuard
   ```

4. **Aktiviere Service**
   ```
   Toggle rechts oben → AN (blau)
   ```

5. **Bestätige Dialog**
   ```
   "KidGuard Zugriff erlauben?"
   
   Berechtigungen:
   - App-Inhalte abrufen ✅
   - Aktionen durchführen ✅
   
   [Abbrechen] [Zulassen] ← Klick "Zulassen"!
   ```

6. **Verifiziere**
   ```
   Toggle sollte AN (blau) sein
   Status: "Ein" sollte angezeigt werden
   ```

---

### Phase 3: ML-Modell testen (5 Min)

#### Test 1: STAGE_SAFE (Harmlos)

**Auf dem Pixel 10:**

1. **Öffne Messenger App**
   ```
   WhatsApp, Messages, Telegram, etc.
   (Jede Chat-App funktioniert)
   ```

2. **Öffne beliebigen Chat**

3. **Schreibe harmlose Nachricht:**
   ```
   "Wie geht's dir?"
   "Hast du die Hausaufgaben gemacht?"
   "Willst du Fortnite spielen?"
   ```

4. **Erwartung:**
   - ✅ KEINE Notification
   - ✅ Score < 0.5 (im Log)

---

#### Test 2: STAGE_TRUST (Verdächtig)

**Schreibe:**
```
"Du bist echt reifer als andere in deinem Alter"
"Ich versteh dich besser als deine Eltern"
```

**Erwartung:**
- ⚠️ Möglicherweise Notification (Score 0.5-0.7)
- ⚠️ Log zeigt: STAGE_TRUST

---

#### Test 3: STAGE_NEEDS (Risiko)

**Schreibe:**
```
"Brauchst du Robux?"
"Ich kann dir V-Bucks kaufen"
"Willst du einen Battle Pass?"
```

**Erwartung:**
- 🚨 Notification erscheint
- 🚨 Score 0.6-0.8
- 🚨 Log zeigt: STAGE_NEEDS

---

#### Test 4: STAGE_ASSESSMENT (KRITISCH!) 🚨

**Schreibe:**
```
"Bist du allein?"
"Wo sind deine Eltern?"
"Ist jemand bei dir im Zimmer?"
"Bist du grad allein zu Hause?"
```

**Erwartung:**
- 🚨🚨🚨 NOTIFICATION erscheint!
- 🚨 Score > 0.8 (HIGH RISK!)
- 🚨 Log zeigt: STAGE_ASSESSMENT
- 🚨 "Hohes Risiko" in Notification

---

### Phase 4: Ergebnisse prüfen

#### Auf dem Pixel 10:

**1. Notification prüfen:**
```
Ziehe Notification Shade nach unten
→ Solltest KidGuard Warnung(en) sehen:

"🚨 KidGuard Alert"
"WhatsApp: Mögliches Grooming erkannt"
"Score: 0.85 - Hohes Risiko"
```

**2. App öffnen:**
```
Tippe auf Notification
ODER
App-Drawer → KidGuard öffnen
```

---

#### Am Mac (Logs):

**Terminal öffnen und ausführen:**

```bash
# Alle KidGuard Logs
adb -s 56301FDCR006BT logcat | grep KidGuard

# Nur Risiko-Erkennungen
adb -s 56301FDCR006BT logcat | grep "RISK DETECTED"

# Database-Speicherung
adb -s 56301FDCR006BT logcat | grep "RiskEvent gespeichert"
```

**Erwartete Logs:**
```
D/GuardianAccessibility: ✅ Service erstellt
D/GuardianAccessibility: 🔔 Notifications AKTIVIERT
D/GuardianAccessibility: 💾 Database INITIALISIERT

W/GuardianAccessibility: 🚨 RISK DETECTED! (ML-Enhanced)
W/GuardianAccessibility: ⚠️ Score: 0.85
W/GuardianAccessibility: ⚠️ Quelle: com.whatsapp
W/GuardianAccessibility: 📝 Text: 'Bist du allein?'

D/GuardianAccessibility: 💾 RiskEvent gespeichert in DB (ID: 1) ← ✅ WICHTIG!
W/GuardianAccessibility: 🔔 Notification gesendet für: WhatsApp
```

---

#### In Android Studio (Database Inspector):

**Während App auf Pixel 10 läuft:**

1. **Öffne Database Inspector**
   ```
   View → Tool Windows → App Inspection
   → Tab: Database Inspector
   ```

2. **Wähle Device**
   ```
   Device Selector: Pixel 10 (56301FDCR006BT)
   ```

3. **Wähle Database**
   ```
   kidguard_database
   ```

4. **Öffne Tabelle**
   ```
   risk_events ← Klick drauf
   ```

5. **Prüfe Einträge**
   ```
   Solltest deine Test-Nachrichten sehen:
   
   | id | timestamp | appPackage | appName | message | riskScore | mlStage |
   |----|-----------|------------|---------|---------|-----------|---------|
   | 1  | 173801... | com.whatsapp | WhatsApp | Bist du allein? | 0.85 | STAGE_ASSESSMENT |
   | 2  | 173801... | com.whatsapp | WhatsApp | Brauchst du Robux? | 0.72 | STAGE_NEEDS |
   ```

---

## 🧪 ERWEITERTE TESTS

### Test 5: Verschiedene Apps

**Teste in:**
- ✅ WhatsApp
- ✅ Messages (SMS)
- ✅ Telegram
- ✅ Signal
- ✅ Instagram
- ✅ TikTok

**Erwartung:** Funktioniert in ALLEN Apps! 🎉

---

### Test 6: Deutsch vs. Englisch

**Deutsche Nachrichten:**
```
"Bist du allein?"
"Brauchst du Robux?"
"Du bist reif für dein Alter"
```

**Englische Nachrichten:**
```
"Are you alone?"
"Do you need Robux?"
"You're mature for your age"
```

**Erwartung:** Beide Sprachen werden erkannt! ✅

---

### Test 7: Emojis & Sonderzeichen

```
"Bist du allein? 😊"
"!!!ROBUX!!!"
"Du bist <special>"
```

**Erwartung:** Funktioniert trotzdem! ✅

---

### Test 8: Lange Nachrichten

```
"Hallo wie geht's dir heute? Ich wollte dich fragen, ob du vielleicht Lust hast, dich mal zu treffen? Bist du grad allein zu Hause oder sind deine Eltern da?"
```

**Erwartung:** 
- Wird auf 500 Zeichen gekürzt
- "Bist du allein" wird trotzdem erkannt! ✅

---

## 📊 ERFOLGS-KRITERIEN

### ✅ Test erfolgreich wenn:

**Pixel 10:**
- [ ] ✅ App installiert & startet
- [ ] ✅ AccessibilityService aktiviert
- [ ] ✅ Harmlose Nachrichten → Keine Notification
- [ ] ✅ "Bist du allein?" → Notification erscheint
- [ ] ✅ Notification zeigt Score & App-Name
- [ ] ✅ Funktioniert in mehreren Apps

**Logs (Mac):**
- [ ] ✅ "Service erstellt"
- [ ] ✅ "Database INITIALISIERT"
- [ ] ✅ "RISK DETECTED" bei Test-Nachrichten
- [ ] ✅ "RiskEvent gespeichert in DB"

**Database Inspector:**
- [ ] ✅ risk_events Tabelle existiert
- [ ] ✅ Einträge werden gespeichert
- [ ] ✅ Alle Felder korrekt befüllt

---

## 🐛 TROUBLESHOOTING

### Problem: Keine Notification erscheint

**Lösungen:**

1. **AccessibilityService prüfen:**
   ```
   Einstellungen → Eingabehilfe → KidGuard
   → Sollte "Ein" sein
   
   Falls nicht: Toggle aus und wieder an
   ```

2. **Permissions prüfen:**
   ```
   Einstellungen → Apps → KidGuard
   → Berechtigungen
   → Notifications sollten erlaubt sein
   ```

3. **Notification Settings:**
   ```
   Einstellungen → Apps → KidGuard → Notifications
   → Alle Kategorien AN
   ```

4. **Force Stop & Restart:**
   ```
   Einstellungen → Apps → KidGuard → Force Stop
   → App neu öffnen
   → AccessibilityService neu aktivieren
   ```

---

### Problem: "Service not running" in Logs

**Lösung:**
```
1. AccessibilityService deaktivieren
2. Pixel 10 neu starten
3. AccessibilityService aktivieren
4. Teste nochmal
```

---

### Problem: Database zeigt keine Einträge

**Prüfe Logs:**
```bash
adb -s 56301FDCR006BT logcat | grep "RiskEvent"

# Sollte zeigen:
# "💾 RiskEvent gespeichert in DB (ID: X)"
```

**Falls nicht:**
- App neu installieren
- Clean & Rebuild in Android Studio
- Database Inspector → Refresh

---

### Problem: App crasht

**Logs prüfen:**
```bash
adb -s 56301FDCR006BT logcat | grep -E "AndroidRuntime|FATAL"
```

**Dann:**
- Screenshot vom Crash senden
- Logcat-Ausgabe speichern
- In Android Studio: Run → Debug 'app'

---

## 📈 PERFORMANCE TESTEN

### Inference-Zeit messen

**In Logs suchen:**
```bash
adb -s 56301FDCR006BT logcat | grep "Inference"
```

**Erwartung:**
- ✅ ML-Inference < 50ms
- ✅ Keyword-Matching < 10ms
- ✅ Gesamt < 100ms

---

## 🎯 NÄCHSTE SCHRITTE

### Nach erfolgreichem Test:

1. **Screenshots machen:**
   - Notification
   - Database Inspector
   - App-Interface

2. **Logs speichern:**
   ```bash
   adb -s 56301FDCR006BT logcat -d > test_logs.txt
   ```

3. **Test-Bericht erstellen:**
   - Welche Nachrichten getestet
   - Welche erkannt wurden
   - Score-Verteilung
   - Funktioniert alles wie erwartet?

4. **Feedback:**
   - Gibt es False Positives?
   - Gibt es False Negatives?
   - Performance OK?

---

## 💡 TIPPS

### Bessere Test-Nachrichten:

**Kombination von Patterns:**
```
"Du bist echt reif. Brauchst du Robux? Bist du allein?"
→ Mehrere Risk-Indicators → Höherer Score
```

**Kontext-Nachrichten:**
```
"Treffen wir uns? Bist du allein zu Hause?"
→ "Treffen" + "allein" → Sehr hohes Risiko!
```

---

## 🎊 ERFOLG!

**Wenn alle Tests ✅:**

🎉 **DAS ML-MODELL FUNKTIONIERT AUF DEINEM PIXEL 10!**

**Du hast erfolgreich getestet:**
- ✅ On-Device ML-Inference
- ✅ AccessibilityService
- ✅ Notification-System
- ✅ Database-Persistenz
- ✅ Hybrid-System (ML + Keywords)

**Priorität 1.3 (Room Database): 100% FERTIG! 🎊**

**Nach Dashboard UI morgen: MVP 100% KOMPLETT!**

---

**Viel Erfolg beim Testen! 🚀**

**Bei Problemen:** Schau in die Logs oder frag nach Hilfe!

---

**Erstellt:** 28. Januar 2026, 02:30 Uhr  
**Device:** Pixel 10 Pro (56301FDCR006BT)  
**Status:** ✅ Bereit zum Testen
