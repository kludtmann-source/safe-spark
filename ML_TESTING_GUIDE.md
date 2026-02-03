# 🧪 KidGuard ML-Integration - Test Guide

**Datum:** 25. Januar 2026, 21:05 Uhr  
**Status:** ✅ ML-Modell integriert und bereit für Testing

---

## ✅ Was wurde implementiert?

### 1. **MLGroomingDetector.kt** (Neu)
```kotlin
app/src/main/java/com/example/kidguard/ml/MLGroomingDetector.kt
```

**Features:**
- Lädt `grooming_detector_scientific.tflite` (90.5% Accuracy)
- Tokenization + Padding (max 50 tokens)
- 5 Stage Detection:
  - `STAGE_SAFE` - Harmlos
  - `STAGE_TRUST` - Vertrauensaufbau
  - `STAGE_ISOLATION` - Isolation
  - `STAGE_NEEDS` - Materielle Anreize
  - `STAGE_ASSESSMENT` - Risiko-Check

**Output:**
```kotlin
data class GroomingPrediction(
    val stage: String,
    val confidence: Float,
    val isDangerous: Boolean,
    val allProbabilities: Map<String, Float>
)
```

---

### 2. **KidGuardEngine.kt** (Updated)
```kotlin
app/src/main/java/com/example/kidguard/KidGuardEngine.kt
```

**Hybrid-System:**
1. **ML-Prediction** (primär): 90.5% Accuracy
2. **Keyword-Matching** (Fallback): Einfach, schnell

**Scoring-Logik:**
```kotlin
if (mlPrediction.confidence > 0.8f) {
    // Hohe ML-Confidence → ML-Score verwenden
    return mlPrediction.confidence
} else {
    // Niedrige ML-Confidence → Kombiniere ML (70%) + Keywords (30%)
    return (mlScore * 0.7f + keywordScore * 0.3f)
}
```

---

### 3. **GuardianAccessibilityService.kt** (Updated)
```kotlin
app/src/main/java/com/example/kidguard/GuardianAccessibilityService.kt
```

**Enhanced Logging:**
```kotlin
if (score > 0.5) {
    Log.w(TAG, "🚨 RISK DETECTED! (ML-Enhanced)")
    Log.w(TAG, "⚠️ Score: $score")
    Log.w(TAG, "⚠️ Quelle: $packageName")
    Log.w(TAG, "📝 Text: '${text.take(100)}...'")
}
```

---

## 🚀 Wie teste ich das jetzt?

### Schritt 1: App installiert?
```bash
~/Library/Android/sdk/platform-tools/adb -s 56301FDCR006BT shell pm list packages | grep safespark
```

**Expected:** `package:safespark`

---

### Schritt 2: KidGuard Accessibility Service aktivieren

**Auf dem Pixel 10:**
1. Öffne **Einstellungen** → **Bedienungshilfen**
2. Suche **"KidGuard"** in der Liste
3. Aktiviere den Service
4. Bestätige die Berechtigungen

**Oder per ADB:**
```bash
~/Library/Android/sdk/platform-tools/adb -s 56301FDCR006BT shell settings put secure enabled_accessibility_services safesparkk/.GuardianAccessibilityService
~/Library/Android/sdk/platform-tools/adb -s 56301FDCR006BT shell settings put secure accessibility_enabled 1
```

---

### Schritt 3: Logcat starten (Terminal offen lassen!)

```bash
~/Library/Android/sdk/platform-tools/adb -s 56301FDCR006BT logcat | grep -E "(GuardianAccessibility|MLGroomingDetector|KidGuardEngine)"
```

**Expected Output beim Service-Start:**
```
GuardianAccessibility: ✅ Service erstellt
GuardianAccessibility: 🔋 Engine initialisiert
KidGuardEngine: ✅ Engine initialisiert mit 141 Risk-Keywords
MLGroomingDetector: ✅ MLGroomingDetector initialisiert
MLGroomingDetector: ✅ TFLite-Modell geladen
GuardianAccessibility: 🎉 onServiceConnected() - Service AKTIV!
```

---

### Schritt 4: Teste mit harmlosen Nachrichten (WhatsApp)

**Öffne WhatsApp** und schreibe eine harmlose Nachricht:

```
"hast du die hausaufgaben gemacht?"
```

**Expected Logcat:**
```
KidGuardEngine: analyzeText() aufgerufen mit: 'hast du die hausaufgaben gemacht?'
MLGroomingDetector: 📝 Tokenized: hast du die hausaufgaben gemacht?... → 5 tokens
MLGroomingDetector: 🎯 Prediction: STAGE_SAFE (81%)
KidGuardEngine: ✅ Hohe ML-Confidence → Score: 0.0
GuardianAccessibility: ✅ Safe: 'hast du die hausaufgaben gemac...' (Score: 0.0)
```

✅ **Kein Alert** - wie erwartet!

---

### Schritt 5: Teste mit verdächtigen Nachrichten

#### Test 1: STAGE_ASSESSMENT
```
"bist du grad allein?"
```

**Expected:**
```
MLGroomingDetector: 🎯 Prediction: STAGE_ASSESSMENT (90%)
KidGuardEngine: ✅ Hohe ML-Confidence → Score: 0.90
GuardianAccessibility: 🚨 RISK DETECTED! (ML-Enhanced)
GuardianAccessibility: ⚠️ Score: 0.90
GuardianAccessibility: 📝 Text: 'bist du grad allein?'
🔔 Notification gesendet für: WhatsApp
```

✅ **Alert ausgelöst!** - Perfekt!

---

#### Test 2: STAGE_TRUST (Englisch)
```
"you seem mature for your age"
```

**Expected:**
```
MLGroomingDetector: 🎯 Prediction: STAGE_TRUST (99%)
KidGuardEngine: ✅ Hohe ML-Confidence → Score: 0.99
GuardianAccessibility: 🚨 RISK DETECTED! (ML-Enhanced)
```

✅ **Alert ausgelöst!** - ML erkennt auch Englisch!

---

#### Test 3: STAGE_ISOLATION
```
"lass uns auf snapchat weiterchatten"
```

**Expected:**
```
MLGroomingDetector: 🎯 Prediction: STAGE_ISOLATION (97%)
KidGuardEngine: ✅ Hohe ML-Confidence → Score: 0.97
GuardianAccessibility: 🚨 RISK DETECTED! (ML-Enhanced)
```

✅ **Alert ausgelöst!** - Platform-Migration erkannt!

---

#### Test 4: STAGE_NEEDS (Gaming)
```
"ich kauf dir den battle pass wenn du willst"
```

**Expected:**
```
MLGroomingDetector: 🎯 Prediction: STAGE_NEEDS (85%)
KidGuardEngine: ✅ Hohe ML-Confidence → Score: 0.85
GuardianAccessibility: 🚨 RISK DETECTED! (ML-Enhanced)
```

✅ **Alert ausgelöst!** - Virtual-Currency-Manipulation erkannt!

---

## 📊 Erwartete Test-Ergebnisse

| Text | Expected Stage | Expected Score | Alert? |
|------|----------------|----------------|--------|
| "hast du die hausaufgaben gemacht?" | STAGE_SAFE | 0.0 - 0.2 | ❌ |
| "wollen wir zusammen lernen?" | STAGE_SAFE | 0.0 - 0.3 | ❌ |
| "spielst du auch fortnite?" | STAGE_SAFE | 0.0 - 0.3 | ❌ |
| "bist du grad allein?" | STAGE_ASSESSMENT | 0.85 - 0.95 | ✅ |
| "you seem mature for your age" | STAGE_TRUST | 0.95 - 1.0 | ✅ |
| "lass uns auf snapchat chatten" | STAGE_ISOLATION | 0.90 - 1.0 | ✅ |
| "ich kauf dir robux" | STAGE_NEEDS | 0.70 - 0.90 | ✅ |
| "deine eltern verstehen das nicht" | STAGE_ISOLATION | 0.75 - 0.90 | ✅ |

---

## 🐛 Troubleshooting

### Problem 1: "Service nicht gefunden"
**Lösung:**
```bash
# Prüfe ob App installiert ist
adb -s 56301FDCR006BT shell pm list packages | grep safespark

# Re-installiere
adb -s 56301FDCR006BT install -r app/build/outputs/apk/debug/app-debug.apk
```

---

### Problem 2: "Keine Logs im Logcat"
**Lösung:**
```bash
# Prüfe ob Service läuft
adb -s 56301FDCR006BT shell dumpsys accessibility | grep -A 5 "KidGuard"

# Expected: "Bound services: KidGuard"
```

---

### Problem 3: "ML-Modell nicht geladen"
**Lösung:**
```bash
# Prüfe ob Assets vorhanden sind
adb -s 56301FDCR006BT shell run-as safesparkk ls -la files/ 2>/dev/null

# Expected: grooming_detector_scientific.tflite (120KB)
```

Alternativ prüfe Logcat für Fehler:
```bash
adb -s 56301FDCR006BT logcat | grep -E "(ERROR|FATAL|Exception)"
```

---

### Problem 4: "Zu viele False-Positives"
**Anpassung in KidGuardEngine.kt:**
```kotlin
// Erhöhe Threshold von 0.5 auf 0.7
if (score > 0.7) {  // War: 0.5
    // Alert auslösen
}
```

---

### Problem 5: "Zu viele False-Negatives"
**Anpassung in KidGuardEngine.kt:**
```kotlin
// Senke Threshold von 0.5 auf 0.3
if (score > 0.3) {  // War: 0.5
    // Alert auslösen
}
```

---

## 📈 Performance-Monitoring

### CPU-Usage prüfen:
```bash
adb -s 56301FDCR006BT shell top -n 1 | grep safespark
```

**Expected:** < 5% CPU bei normaler Nutzung

---

### Memory-Usage prüfen:
```bash
adb -s 56301FDCR006BT shell dumpsys meminfo safesparkk
```

**Expected:** 
- Total PSS: ~50-80 MB
- Native Heap: ~10-20 MB (TFLite-Modell)

---

### Battery-Impact prüfen:
```bash
adb -s 56301FDCR006BT shell dumpsys batterystats | grep -A 10 safespark
```

**Expected:** < 1% Battery-Usage pro Tag

---

## 🎯 Success Criteria

✅ **Service läuft:** `onServiceConnected()` im Logcat  
✅ **ML-Modell geladen:** `MLGroomingDetector initialisiert`  
✅ **Harmlose Nachrichten:** Score < 0.5, kein Alert  
✅ **Gefährliche Nachrichten:** Score > 0.7, Alert ausgelöst  
✅ **Multilinguale:** Englisch + Deutsch funktioniert  
✅ **Performance:** < 50ms Inferenz-Zeit  
✅ **Battery:** < 1% pro Tag  

---

## 🚀 Nächste Schritte (Optional)

### 1. Real-World Testing
- Teste mit echten WhatsApp-Konversationen
- Sammle False-Positive/Negative Cases
- Verfeinere Threshold (aktuell: 0.5)

### 2. Model-Improvement
- Mehr Trainingsdaten sammeln
- Re-Training mit User-Feedback
- Ziel: 95%+ Accuracy

### 3. Alert-System (Phase 4)
- Push-Notification an Eltern-Device
- Firebase Cloud Messaging
- Verschlüsselte Übertragung

---

**Report erstellt:** 2026-01-25 21:10 Uhr  
**ML-Integration:** ✅ Complete  
**Status:** Ready for Live Testing auf Pixel 10
