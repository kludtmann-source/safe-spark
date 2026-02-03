# ⚡ SCHNELL-INSTALLATION - KidGuard App

**Datum:** 26. Januar 2026  
**APK:** app-debug.apk (✅ erfolgreich gebaut)

---

## 🚀 INSTALLATION IN 3 SCHRITTEN

### Schritt 1: Terminal öffnen
```bash
# Öffne ein neues Terminal-Fenster
# (cmd+Space → "Terminal" eingeben)
```

---

### Schritt 2: Emulator starten (falls nicht bereits läuft)
```bash
# Gehe zum Projekt-Verzeichnis
cd /Users/knutludtmann/AndroidStudioProjects/KidGuard

# Starte Emulator
~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.1 &

# Warte 60 Sekunden bis Emulator gestartet ist
sleep 60
```

**ODER:** Verwende Android Studio → Tools → Device Manager → Start Emulator

---

### Schritt 3: App installieren
```bash
# Installiere APK
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk

# Erwartete Ausgabe:
# Performing Streamed Install
# Success
```

---

### Schritt 4: App starten
```bash
# Starte KidGuard
~/Library/Android/sdk/platform-tools/adb shell am start -n safesparkk/.MainActivity
```

---

## ✅ VERIFIKATION

### Prüfe ob Installation erfolgreich war:
```bash
# Prüfe installierte Apps
~/Library/Android/sdk/platform-tools/adb shell pm list packages | grep safespark

# Erwartete Ausgabe:
# package:safesparkk
```

### Prüfe App-Logs:
```bash
# Live-Logs anzeigen
~/Library/Android/sdk/platform-tools/adb logcat | grep KidGuard

# Erwartete Logs:
# MainActivity: ✅ MainActivity: KidGuardEngine initialisiert
# GuardianAccessibility: ✅ Service erstellt
```

---

## 🎯 WAS DU SEHEN SOLLTEST

### Nach App-Start:
1. ✅ **Dashboard öffnet sich**
2. ✅ Header: "📊 KidGuard Dashboard"
3. ✅ Statistik-Card:
   - Heute: 0
   - Letzte 7 Tage: 0
   - Gesamt: 0
4. ✅ Empty State: "✅ Keine Risiken erkannt"
5. ✅ Text: "Alles sicher! 🎉"

---

## 🔧 TROUBLESHOOTING

### Problem: "adb: command not found"
```bash
# Setze PATH
export PATH="$PATH:$HOME/Library/Android/sdk/platform-tools"

# Versuche erneut
adb devices
```

---

### Problem: "no devices/emulators found"
```bash
# Starte Emulator manuell in Android Studio
# Oder:
~/Library/Android/sdk/emulator/emulator -avd Medium_Phone_API_36.1 &

# Warte 60 Sekunden
sleep 60

# Prüfe Verbindung
adb devices
```

---

### Problem: "INSTALL_FAILED_UPDATE_INCOMPATIBLE"
```bash
# Deinstalliere alte Version
adb uninstall safesparkk

# Installiere neu
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 ALTERNATIVE: Installation via Drag & Drop

### Methode A: Android Studio
1. Öffne Android Studio
2. Starte Emulator (Device Manager)
3. Drag & Drop `app-debug.apk` auf Emulator-Fenster
4. App wird automatisch installiert

### Methode B: Direkt auf Gerät
1. Kopiere APK auf Gerät (USB/Cloud)
2. Öffne Datei-Manager auf Gerät
3. Tap auf APK-Datei
4. "Installieren" bestätigen
5. "Aus unbekannten Quellen" erlauben (falls nötig)

---

## 🎉 ERFOLG!

Nach erfolgreicher Installation:

### Nächste Schritte:
1. **AccessibilityService aktivieren:**
   - Settings → Accessibility → KidGuard → Enable

2. **Testen:**
   - Öffne WhatsApp (oder andere Chat-App)
   - Tippe: "Bist du allein?"
   - Gehe zurück zu KidGuard
   - Dashboard sollte Event zeigen! 🚨

3. **Logs prüfen:**
   ```bash
   adb logcat | grep "GuardianAccessibility"
   # Sollte zeigen: 🚨 RISK DETECTED!
   ```

---

## 📊 ERWARTETES ERGEBNIS

```
Dashboard nach Risiko-Erkennung:
┌─────────────────────────────────┐
│  📊 KidGuard Dashboard          │
├─────────────────────────────────┤
│  Erkannte Risiken:              │
│  • Heute: 1                     │
│  • Letzte 7 Tage: 1             │
│  • Gesamt: 1                    │
│                                 │
│  🚨 Hoch: 1  🟠 Mittel: 0       │
│  🟡 Niedrig: 0                  │
├─────────────────────────────────┤
│  Letzte Ereignisse              │
│                                 │
│  🚨 WhatsApp                    │
│  26.01.2026 17:30               │
│  "Bist du allein?"              │
│  Score: 85% (HOCH)              │
│  [Details] [Ignorieren]         │
└─────────────────────────────────┘
```

---

## ⚡ QUICK INSTALL (Ein Befehl)

Wenn Emulator bereits läuft:
```bash
cd /Users/knutludtmann/AndroidStudioProjects/KidGuard && \
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk && \
~/Library/Android/sdk/platform-tools/adb shell am start -n safesparkk/.MainActivity
```

---

## 📞 HILFE

**Script verwenden:**
```bash
cd /Users/knutludtmann/AndroidStudioProjects/KidGuard
./install_app.sh
```

**Manuelle Schritte:**
Siehe oben (Schritt 1-4)

**Bei weiteren Problemen:**
- Prüfe `adb devices` → mindestens 1 Gerät sollte "device" status haben
- Prüfe APK existiert: `ls -lh app/build/outputs/apk/debug/app-debug.apk`
- Prüfe Logs: `adb logcat | grep -E "Error|Exception|KidGuard"`

---

**Status:** ✅ APK BEREIT ZUR INSTALLATION  
**Größe:** ~20 MB  
**Build:** SUCCESSFUL  
**Nächster Schritt:** Führe Schritt 2-4 oben aus
