# KidGuard AccessibilityService - Aktivierungs-Anleitung

## 📱 So aktivierst du den GuardianAccessibilityService

### Methode 1: Über das Emulator/Gerät (Empfohlen)

1. **Öffne die Einstellungen**
   - Gehe zu **Einstellungen** → **Bedienungshilfen** (Accessibility)

2. **Finde KidGuard**
   - Scrolle zu **Installierte Dienste** (Installed Services)
   - Suche nach **"Guardian Accessibility Service"** oder **"KidGuard"**

3. **Aktiviere den Dienst**
   - Tippe auf den Dienst
   - Aktiviere den Schalter
   - Bestätige die Berechtigung

4. **Teste den Dienst**
   - Öffne eine beliebige App mit Texteingabe
   - Tippe Text ein
   - Überprüfe Logcat auf "RISK DETECTED" Meldungen

---

### Methode 2: Via ADB-Befehl (Schnell für Tests)

```bash
# Aktiviere AccessibilityService
adb shell settings put secure enabled_accessibility_services safesparkk/.GuardianAccessibilityService

# Aktiviere Accessibility generell
adb shell settings put secure accessibility_enabled 1

# Starte die App neu
adb shell am force-stop safesparkk
adb shell am start -n safesparkk/.MainActivity
```

---

### Methode 3: Automatisches Test-Skript

Erstelle eine Datei `enable_accessibility.sh`:

```bash
#!/bin/bash

echo "🔧 Aktiviere KidGuard AccessibilityService..."

# Setze den PATH für adb
export ADB=~/Library/Android/sdk/platform-tools/adb

# Aktiviere den Service
$ADB shell settings put secure enabled_accessibility_services safesparkk/.GuardianAccessibilityService
$ADB shell settings put secure accessibility_enabled 1

echo "✅ AccessibilityService aktiviert!"

# Starte die App neu
echo "🔄 Starte App neu..."
$ADB shell am force-stop safesparkk
sleep 1
$ADB shell am start -n safesparkk/.MainActivity

echo "📊 Prüfe Status..."
$ADB shell settings get secure enabled_accessibility_services

echo ""
echo "✅ Fertig! Du kannst jetzt die App testen."
echo "💡 Tipp: Verwende 'adb logcat | grep RISK' um Erkennungen zu sehen"
```

Ausführen:
```bash
chmod +x enable_accessibility.sh
./enable_accessibility.sh
```

---

## 🧪 Test-Szenarien

### Test 1: Einfache Texteingabe
1. Öffne eine Notiz-App oder Messenger
2. Tippe einen harmlosen Text: "Hallo Welt"
3. Erwartung: Keine RISK DETECTED Meldung

### Test 2: Risiko-Text (für Testing)
1. Öffne eine Texteingabe
2. Tippe Testtext ein (simuliert potentiell problematischen Inhalt)
3. Erwartung: "RISK DETECTED" im Logcat wenn Score > 0.8

### Test 3: Logcat-Überwachung
```bash
# Zeige nur KidGuard relevante Logs
adb logcat | grep -E "(GuardianAccessibilityService|KidGuardEngine|RISK)"

# Oder filtere nach Tag
adb logcat GuardianAccessibilityService:D KidGuardEngine:D *:S
```

---

## 🔍 Überprüfung

### Prüfe ob der Service läuft:
```bash
# Zeige aktive Accessibility Services
adb shell settings get secure enabled_accessibility_services

# Sollte anzeigen:
# safesparkk/.GuardianAccessibilityService
```

### Prüfe Accessibility-Status:
```bash
adb shell settings get secure accessibility_enabled

# Sollte anzeigen: 1
```

### Prüfe Service im Logcat:
```bash
adb logcat -d | grep AccessibilityService | tail -10
```

---

## ⚠️ Fehlerbehebung

### Problem: Service erscheint nicht in Einstellungen
**Lösung:**
1. App neu installieren: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
2. Gerät/Emulator neu starten
3. Prüfe AndroidManifest.xml auf korrekte Service-Deklaration

### Problem: Service aktiviert, aber keine Events
**Lösung:**
1. Prüfe ob `accessibility_service_config.xml` existiert
2. Prüfe Logcat auf Fehler: `adb logcat *:E | grep -i accessibility`
3. Stelle sicher, dass EventTypes in config korrekt sind

### Problem: "RISK DETECTED" erscheint nicht
**Lösung:**
1. Prüfe ob KidGuardEngine initialisiert wurde
2. Prüfe TensorFlow Lite Model-Datei
3. Überprüfe Score-Threshold (sollte > 0.8 sein)
4. Teste mit verschiedenen Texten

---

## 📊 Monitoring

### Echtzeit-Log-Überwachung:
```bash
# In einem separaten Terminal-Fenster
adb logcat -c && adb logcat | grep --color=always -E "(RISK DETECTED|GuardianAccessibilityService|analyzeText)"
```

### Log in Datei speichern:
```bash
adb logcat -d > kidguard_test.log
grep "RISK DETECTED" kidguard_test.log
```

---

## 🎯 Erfolgreiche Aktivierung erkennen

Du weißt, dass der Service korrekt läuft, wenn:

1. ✅ Service in Einstellungen → Bedienungshilfen sichtbar ist
2. ✅ Service ist aktiviert (grüner Schalter)
3. ✅ Logcat zeigt AccessibilityEvent-Verarbeitung
4. ✅ Bei Texteingabe werden Events empfangen
5. ✅ KidGuardEngine.analyzeText() wird aufgerufen

---

## 📝 Nächste Schritte nach Aktivierung

1. **Funktionstest:** Teste verschiedene Apps mit Texteingabe
2. **Performance:** Überwache CPU/RAM-Nutzung
3. **Genauigkeit:** Teste mit verschiedenen Texttypen
4. **Batterie:** Prüfe Energieverbrauch über längere Zeit

---

**Status:** 🟢 Ready for Testing  
**Letzte Aktualisierung:** 24. Januar 2026

