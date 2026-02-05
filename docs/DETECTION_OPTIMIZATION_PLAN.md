# Detection Optimization Plan

## Aktuelle Probleme

### 1. Logging nicht transparent
- Zu viele Debug-Logs vermischen sich mit relevanten Detections
- Fehlende Struktur: Input-Text nicht immer mit Score geloggt
- Keine einfache Filterbarkeit für "nur positive Vorfälle"

### 2. Zu viele False Positives
- **Einzelwort-Trigger**: "allein", "zimmer", "eltern" triggern ohne Kontext
- **Niedrige Thresholds**: Semantic 0.75f, Pattern-basiert 0.70f
- **Kein Dual-Confirmation**: Ein einziger Layer kann Alert auslösen
- **Fehlende Textlängen-Prüfung**: Kurze Texte erhalten hohe Scores

---

## Analyse der aktuellen Thresholds

| Layer | Aktueller Threshold | Problem |
|-------|---------------------|---------|
| SemanticDetector | 0.75f | Zu sensitiv für ähnliche, harmlose Phrasen |
| AssessmentPatterns | 0.70-0.85f | Einzelwörter ohne Kontext triggern |
| OspreyDetector | 0.5-0.9f (per Stage) | Inkonsistent |
| MLGroomingDetector | 0.3f (Doku!) | Viel zu niedrig! |

---

## OPTIMIERUNGSMASSNAHMEN

### Maßnahme 1: Zentrales Detection-Logging

**Neuer DetectionLogger mit strukturiertem Output:**

```kotlin
// Nur positive Detections loggen
// Format: TAG "SafeSpark-ALERT" für einfaches Filtern
```

**Implementierung:** Neue Klasse `DetectionLogger.kt`

### Maßnahme 2: Kontext-basierte Pattern-Erkennung

**Problem:** "allein" triggert auch bei "Ich bin nicht allein"

**Lösung:** Phrase statt Einzelwort + Negations-Check

```kotlin
// VORHER (zu sensitiv)
"allein" to 0.85f

// NACHHER (kontext-aware)
"bist du allein" to 0.85f
"are you alone" to 0.85f
"ist niemand bei dir" to 0.80f
// + Negations-Check: "nicht allein" → SKIP
```

### Maßnahme 3: Threshold-Erhöhung

**Empfohlene neue Thresholds:**

| Layer | Vorher | Nachher | Begründung |
|-------|--------|---------|------------|
| SemanticDetector | 0.75 | **0.82** | Mehr Separation von ähnlichen Phrasen |
| SUPERVISION_CHECK | 0.75 | **0.80** | Kritischste Kategorie |
| SECRECY_REQUEST | 0.78 | **0.85** | Häufige False Positives |
| Assessment Patterns | 0.70-0.85 | **0.80-0.90** | Nur bei klarem Match |
| MLGroomingDetector | 0.3 | **0.65** | Deutlich zu niedrig! |

### Maßnahme 4: Dual-Confirmation-Requirement

**Logik:** Alarm nur wenn MINDESTENS 2 Layer übereinstimmen

```kotlin
fun shouldTriggerAlert(scores: Map<String, Float>): Boolean {
    val highScoreLayers = scores.count { it.value > 0.70f }
    return highScoreLayers >= 2  // Mindestens 2 Layer müssen übereinstimmen
}
```

### Maßnahme 5: Mindest-Textlänge

**Problem:** "ok" oder "ja" erhalten manchmal hohe Scores

**Lösung:**
```kotlin
const val MIN_TEXT_LENGTH_FOR_ANALYSIS = 10  // Mindestens 10 Zeichen
const val MIN_WORDS_FOR_PATTERN_MATCH = 3    // Mindestens 3 Wörter für Pattern
```

### Maßnahme 6: Negations-Filter

```kotlin
private val negationPatterns = listOf(
    "nicht allein", "not alone",
    "keine eltern", "parents are here",
    "niemandem erzählen" // vs. "erzähl niemandem" (verschiedene Intention!)
)

fun containsNegation(text: String): Boolean {
    return negationPatterns.any { text.lowercase().contains(it) }
}
```

### Maßnahme 7: Confidence-Decay für kurze Texte

```kotlin
fun adjustConfidenceByLength(score: Float, textLength: Int): Float {
    return when {
        textLength < 15 -> score * 0.6f   // Stark reduzieren
        textLength < 30 -> score * 0.8f   // Leicht reduzieren  
        else -> score                      // Volle Confidence
    }
}
```

---

## IMPLEMENTATION PRIORITY

| # | Maßnahme | Impact | Aufwand | Priorität |
|---|----------|--------|---------|-----------|
| 1 | DetectionLogger | Hoch (Debugging) | Niedrig | 🔴 SOFORT |
| 2 | Kontext-Patterns | Sehr Hoch | Mittel | 🔴 SOFORT |
| 3 | Threshold-Erhöhung | Sehr Hoch | Niedrig | 🔴 SOFORT |
| 4 | Dual-Confirmation | Hoch | Mittel | 🟡 Bald |
| 5 | Mindest-Textlänge | Mittel | Niedrig | 🟡 Bald |
| 6 | Negations-Filter | Mittel | Mittel | 🟢 Nice-to-have |
| 7 | Length-based Decay | Niedrig | Niedrig | 🟢 Nice-to-have |

---

## KONKRETE CODE-ÄNDERUNGEN

### 1. DetectionLogger.kt (NEU)

```kotlin
package com.example.safespark.logging

object DetectionLogger {
    private const val TAG = "SafeSpark-ALERT"
    
    fun logPositive(text: String, score: Float, method: String, pattern: String?) {
        Log.w(TAG, "═══════════════════════════════════════")
        Log.w(TAG, "🚨 GROOMING DETECTED")
        Log.w(TAG, "───────────────────────────────────────")
        Log.w(TAG, "Text: \"${text.take(100)}\"")
        Log.w(TAG, "Score: ${(score * 100).toInt()}%")
        Log.w(TAG, "Method: $method")
        pattern?.let { Log.w(TAG, "Pattern: $it") }
        Log.w(TAG, "═══════════════════════════════════════")
    }
}
```

### 2. SemanticResult.kt - Thresholds erhöhen

```kotlin
SUPERVISION_CHECK(
    threshold = 0.80f,  // vorher: 0.75f
    ...
),
SECRECY_REQUEST(
    threshold = 0.85f,  // vorher: 0.78f
    ...
),
```

### 3. KidGuardEngine.kt - Pattern-Änderungen

```kotlin
// VORHER (Einzelwörter)
val assessmentPatterns = mapOf(
    "allein" to 0.85f,
    ...
)

// NACHHER (Phrasen)
val assessmentPatterns = mapOf(
    "bist du allein" to 0.85f,
    "bist du alleine" to 0.85f,
    "are you alone" to 0.85f,
    "ist jemand bei dir" to 0.80f,
    "wo sind deine eltern" to 0.80f,
    "sind deine eltern da" to 0.80f,
    ...
)
```

### 4. DetectionConfig.kt (NEU) - Zentrale Konfiguration

```kotlin
package com.example.safespark.config

object DetectionConfig {
    // Thresholds
    const val SEMANTIC_THRESHOLD = 0.82f
    const val ML_THRESHOLD = 0.65f
    const val PATTERN_THRESHOLD = 0.80f
    
    // Text Requirements
    const val MIN_TEXT_LENGTH = 10
    const val MIN_WORDS_FOR_PATTERN = 3
    
    // Confirmation
    const val REQUIRE_DUAL_CONFIRMATION = true
    const val MIN_LAYERS_FOR_ALERT = 2
}
```

---

## TESTPLAN

Nach Implementation:

1. **Bekannte False Positives testen:**
   - "Ich bin nicht allein" → sollte NICHT triggern
   - "ok" → sollte NICHT triggern
   - "Meine Eltern sind im Zimmer" → sollte NICHT triggern

2. **True Positives verifizieren:**
   - "Bist du alleine?" → MUSS triggern
   - "Erzähl niemandem davon" → MUSS triggern
   - "Schick mir ein Foto" → MUSS triggern

3. **Logging verifizieren:**
   - Logcat Filter: `SafeSpark-ALERT`
   - Nur positive Detections sichtbar
   - Text + Score + Method in jedem Log

---

## NÄCHSTE SCHRITTE

1. ✅ Diesen Plan reviewen
2. ⬜ DetectionLogger.kt implementieren
3. ⬜ Thresholds in SemanticResult.kt erhöhen
4. ⬜ Assessment-Patterns in KidGuardEngine.kt auf Phrasen umstellen
5. ⬜ DetectionConfig.kt erstellen
6. ⬜ Auf Device testen mit bekannten False Positives
7. ⬜ Ergebnisse dokumentieren
