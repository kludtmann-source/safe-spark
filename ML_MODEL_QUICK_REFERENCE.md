# 🚀 ML-Modell Quick Reference - KidGuard

**Für schnelle Antworten auf die wichtigsten Fragen**

---

## ❓ Welches Modell wird verwendet?

**Aktives Modell:** `grooming_detector_scientific.tflite`

```kotlin
// In MLGroomingDetector.kt, Zeile 20:
private const val MODEL_FILE = "grooming_detector_scientific.tflite"
```

---

## 📊 Wichtigste Specs

| Eigenschaft | Wert |
|-------------|------|
| **Name** | grooming_detector_scientific |
| **Format** | TensorFlow Lite (Float32) |
| **Größe** | 0.03 MB (30 KB) |
| **Architektur** | Conv1D + GlobalMaxPooling |
| **Vocab** | 1000 Wörter (DE + EN) |
| **Sequence Length** | 50 Tokens |
| **Accuracy** | 90.5% |
| **Inferenz-Zeit** | < 10ms |
| **Klassen** | 5 (Grooming Stages) |

---

## 🏷️ Die 5 Klassen

1. **STAGE_SAFE** ✅ - Harmlos (Risk: 0)
2. **STAGE_TRUST** 🟡 - Vertrauensaufbau (Risk: 0.4-0.6)
3. **STAGE_NEEDS** 🟠 - Materielle Angebote (Risk: 0.6-0.7)
4. **STAGE_ISOLATION** 🔴 - Geheimhaltung (Risk: 0.7-0.85)
5. **STAGE_ASSESSMENT** 🚨 - Akute Gefahr (Risk: 0.85-1.0)

---

## 💻 Wo wird es verwendet?

### 1. Laden
**File:** `app/src/main/java/com/example/kidguard/ml/MLGroomingDetector.kt`  
**Zeile:** 42-46 (init Block)

```kotlin
loadModel()  // Lädt grooming_detector_scientific.tflite
loadMetadata()  // Lädt grooming_detector_scientific_metadata.json
```

### 2. Inferenz
**File:** `app/src/main/java/com/example/kidguard/ml/MLGroomingDetector.kt`  
**Methode:** `predict(message: String): GroomingPrediction?`

```kotlin
val prediction = mlDetector.predict("Bist du allein?")
// → STAGE_ASSESSMENT (0.95 Confidence)
```

### 3. Integration
**File:** `app/src/main/java/com/example/kidguard/KidGuardEngine.kt`  
**Zeile:** 28 + 51-57

```kotlin
private val mlDetector: MLGroomingDetector  // Instanz
val mlPrediction = mlDetector.predict(input)  // Aufruf
```

---

## 📂 Wo sind die Dateien?

### Training
```
ml/models/grooming_detector_scientific.tflite      ← Source
ml/models/grooming_detector_scientific_metadata.json
```

### Production (Android)
```
app/src/main/assets/grooming_detector_scientific.tflite      ← APK
app/src/main/assets/grooming_detector_scientific_metadata.json
```

---

## 🎯 Beispiel-Predictions

| Input | Predicted Stage | Confidence | Dangerous? |
|-------|----------------|------------|------------|
| "Wie geht's?" | STAGE_SAFE | 0.92 | ❌ |
| "Du bist echt reif" | STAGE_TRUST | 0.84 | ❌ |
| "Brauchst du Robux?" | STAGE_NEEDS | 0.88 | ✅ |
| "Schreib auf Snapchat" | STAGE_ISOLATION | 0.91 | ✅ |
| "Bist du allein?" | STAGE_ASSESSMENT | 0.95 | ✅ |

---

## 🔧 Hybrid-System

**KidGuard nutzt NICHT nur ML!**

```
Text-Input
    ↓
┌──────────────────┐
│  ML-Modell (70%) │  ← grooming_detector_scientific.tflite
└──────────────────┘
    +
┌──────────────────┐
│  Keywords (30%)  │  ← vocabulary.txt
└──────────────────┘
    ↓
Final Risk-Score
```

**Warum?**
- **Robustheit:** Keywords fangen Edge-Cases
- **Transparenz:** Debuggable
- **Geschwindigkeit:** Fallback bei ML-Fehlern

---

## 📚 Vollständige Doku

📖 **Siehe:** `ML_MODEL_DOCUMENTATION.md` (umfassende 400+ Zeilen Dokumentation)

---

## 🚀 Quick Commands

### Modell testen
```bash
cd /Users/knutludtmann/AndroidStudioProjects/KidGuard
./quick_test_ml.sh
```

### Training wiederholen
```bash
cd ml
source venv/bin/activate
python scripts/train_phase3.py
```

### Assets aktualisieren
```bash
cp ml/models/grooming_detector_scientific.tflite app/src/main/assets/
cp ml/models/grooming_detector_scientific_metadata.json app/src/main/assets/
```

---

## ✅ Status

| Aspekt | Status |
|--------|--------|
| **Modell vorhanden** | ✅ |
| **In App integriert** | ✅ |
| **Funktioniert** | ✅ |
| **Dokumentiert** | ✅ |
| **Getestet** | ⚠️ (manuell, keine Unit-Tests) |

---

**Letzte Aktualisierung:** 26. Januar 2026
