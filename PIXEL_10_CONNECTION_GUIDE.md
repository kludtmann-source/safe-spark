# 📱 Pixel 10 Pro mit Mac verbinden - Schnell-Hilfe

**Problem:** Pixel 10 Pro wird nicht in Android Studio angezeigt  
**Status:** Nur Emulator wird erkannt (emulator-5554)

---

## ✅ LÖSUNG - Schritt für Schritt:

### 1️⃣ USB-Debugging aktivieren (auf dem Pixel 10)

**Entwickleroptionen aktivieren:**
```
1. Einstellungen öffnen
2. Über das Telefon
3. Build-Nummer 7x antippen
4. "Sie sind jetzt Entwickler!" erscheint
```

**USB-Debugging aktivieren:**
```
1. Einstellungen → System → Entwickleroptionen
2. Scrolle zu "USB-Debugging"
3. Toggle AN (blau)
4. Bestätige Dialog "USB-Debugging zulassen"
```

---

### 2️⃣ Verbindung autorisieren

**Nach USB-Anschluss sollte erscheinen:**
```
"USB-Debugging zulassen?"
Computer-Fingerabdruck: XX:XX:XX:...

☑ Immer von diesem Computer zulassen
[Abbrechen] [OK]
```

**→ Klicke OK!**

---

### 3️⃣ ADB neu starten (am Mac)

**Führe aus:**
```bash
# ADB Server neu starten
~/Library/Android/sdk/platform-tools/adb kill-server
~/Library/Android/sdk/platform-tools/adb start-server

# Geräte prüfen
~/Library/Android/sdk/platform-tools/adb devices
```

**Erwartete Ausgabe:**
```
List of devices attached
emulator-5554          device
1A2B3C4D5E6F7G8H       device    ← Dein Pixel 10!
```

---

### 4️⃣ USB-Modus prüfen (auf Pixel 10)

**Notification Shade nach unten ziehen:**
```
"USB für Dateiübertragung"
oder
"Android-System - USB wird zum Laden verwendet"
```

**Antippen und wählen:**
- ✅ **"Dateiübertragung / Android Auto"**
- ❌ Nicht "Nur Laden"
- ❌ Nicht "PTP (Fotos)"

---

### 5️⃣ Kabel & Port prüfen

**Probleme:**
- ❌ Defektes USB-C Kabel
- ❌ USB-C Hub (verwende direkten Port!)
- ❌ USB-A zu USB-C Adapter (kann Probleme machen)

**Lösung:**
- ✅ Originales Google Pixel Kabel verwenden
- ✅ Direkt in Mac USB-C Port stecken
- ✅ Anderes USB-C Kabel testen

---

### 6️⃣ Android Studio aktualisieren

**Falls Device Selector leer bleibt:**

```
Android Studio → Check for Updates
→ Installiere Updates falls vorhanden
→ Restart Android Studio
```

---

## 🔧 TROUBLESHOOTING

### Problem: "unauthorized" statt "device"

**Ausgabe:**
```
1A2B3C4D5E6F7G8H       unauthorized
```

**Lösung:**
```
1. Auf Pixel 10: USB-Debugging Dialog sollte erscheinen
2. Falls nicht: USB-Debugging AUS → AN
3. Kabel ab- und wieder anstecken
4. Dialog erscheint → OK klicken
5. adb devices erneut prüfen
```

---

### Problem: "offline"

**Ausgabe:**
```
1A2B3C4D5E6F7G8H       offline
```

**Lösung:**
```bash
# ADB komplett zurücksetzen
adb kill-server
adb start-server

# Falls das nicht hilft:
# Auf Pixel 10:
# - USB-Debugging AUS
# - Kabel abstecken
# - Pixel neu starten
# - USB-Debugging AN
# - Kabel wieder einstecken
```

---

### Problem: Gar kein Gerät

**Ausgabe:**
```
List of devices attached
emulator-5554          device
(Kein Pixel!)
```

**Checkliste:**
- [ ] USB-Debugging aktiviert? (Pixel: Einstellungen → Entwickleroptionen)
- [ ] USB-Modus = "Dateiübertragung"? (Notification Shade)
- [ ] Anderes USB-C Kabel testen?
- [ ] Direkten Mac USB-C Port nutzen (kein Hub)?
- [ ] Pixel entsperrt? (Bildschirm muss AN sein!)
- [ ] ADB neu gestartet? (adb kill-server && adb start-server)

---

## 🚀 SCHNELL-SCRIPT

**Ich habe ein Script erstellt:**

```bash
./check_pixel_connection.sh
```

**Das Script macht:**
1. ✅ Prüft USB-Geräte
2. ✅ Prüft ADB-Verbindung
3. ✅ Startet ADB neu falls nötig
4. ✅ Zeigt Troubleshooting-Tipps

---

## 📱 IN ANDROID STUDIO

**Nach erfolgreicher Verbindung:**

```
1. Toolbar oben: Device Selector
2. Sollte zeigen:
   - emulator-5554 (Pixel 8 API 35)
   - [SERIAL] (Pixel 10 Pro)  ← Dein Gerät!
   
3. Wähle Pixel 10 Pro
4. Shift+F10 (Run)
5. App installiert auf echtem Gerät! 🎉
```

---

## 💡 WARUM NICHT ERKANNT?

**Häufigste Gründe:**

1. **USB-Debugging nicht aktiviert** (90% der Fälle!)
2. **USB-Modus falsch** (Nur Laden statt Dateiübertragung)
3. **Autorisierung nicht erteilt** (Dialog verpasst)
4. **Pixel-Bildschirm gesperrt** (muss entsperrt sein!)
5. **Defektes/falsches Kabel** (muss Daten übertragen können)
6. **USB-Hub Problem** (direkt in Mac stecken!)

---

## ✅ ERFOLGS-CHECK

**Wenn alles funktioniert, siehst du:**

**Terminal:**
```bash
$ adb devices

List of devices attached
emulator-5554          device
1A2B3C4D5E6F7G8H       device  ← Pixel 10!
```

**Android Studio:**
```
Device Selector zeigt:
- Pixel 8 API 35 (Emulator)
- Pixel 10 Pro (Physical Device)  ✅
```

**Pixel 10 Notification:**
```
"USB-Debugging verbunden"
"Android-System - Dateiübertragung aktiv"
```

---

## 🎯 NÄCHSTE SCHRITTE

**Nach Verbindung:**

1. **In Android Studio:**
   - Wähle Pixel 10 Pro im Device Selector
   - Shift+F10 (Run)

2. **App wird installiert:**
   - Installation ~10 Sekunden
   - App startet automatisch

3. **Teste:**
   - AccessibilityService aktivieren
   - "Bist du allein?" schreiben
   - Logs prüfen (Logcat)
   - Database Inspector nutzen

---

## 📞 IMMER NOCH PROBLEME?

**Prüfe:**

```bash
# System USB Geräte
system_profiler SPUSBDataType | grep -i google

# ADB Status
adb devices -l

# ADB Version
adb version
```

**Falls immer noch nichts:**
- Pixel 10 neu starten
- Mac neu starten
- Android Studio neu starten
- Anderes USB-C Kabel verwenden

---

**Erstellt:** 28. Januar 2026, 02:15 Uhr  
**Status:** Troubleshooting-Guide bereit  
**Nächster Schritt:** USB-Debugging aktivieren auf Pixel 10
