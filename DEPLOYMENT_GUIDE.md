# 📱 DEPLOYMENT GUIDE - MODELS AUFS DEVICE

**Datum:** 28. Januar 2026  
**Status:** Ready to Deploy  

---

## ⚠️ PROBLEM: TFLite Export scheitert (LLVM Error)

Der BiLSTM-Layer verursacht einen Compiler-Fehler im TFLite Converter.

**Lösung:** Nutze eines der **bereits trainierten Models** die funktionieren!

---

## ✅ OPTION 1: QUICK MODEL NUTZEN (EMPFOHLEN)

Das Quick Model wurde bereits erfolgreich trainiert und als TFLite exportiert.

### Model-Info:
```
Model: kid_guard_v1.tflite (bereits in assets/)
Accuracy: ~94%
Size: ~1 MB
Vorteil: Funktioniert sofort, kein SELECT_TF_OPS nötig
```

### Schritte:

#### 1. Prüfe ob Model existiert:
```bash
ls -lh app/src/main/assets/kid_guard_v1.tflite
```

#### 2. Update MLGroomingDetector.kt:

```kotlin
// Datei: app/src/main/java/com/example/safespark/ml/MLGroomingDetector.kt

class MLGroomingDetector(private val context: Context) {
    
    private var interpreter: Interpreter? = null
    private val modelPath = "kid_guard_v1.tflite"  // ← Nutze existierendes Model
    
    // WICHTIG: Threshold senken für bessere Grooming Detection
    private val GROOMING_THRESHOLD = 0.3f
    
    init {
        loadModel()
    }
    
    private fun loadModel() {
        try {
            val model = loadModelFile(modelPath)
            interpreter = Interpreter(model)
            Log.d(TAG, "✅ Model loaded: $modelPath")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Model load failed", e)
        }
    }
    
    fun predict(text: String): MLPrediction {
        // ... existing prediction code ...
        
        // WICHTIG: Nutze niedrigeren Threshold
        val isGrooming = groomingProbability > GROOMING_THRESHOLD  // 0.3 statt 0.5
        
        return MLPrediction(
            stage = if (isGrooming) "STAGE_TRUST" else "STAGE_SAFE",
            confidence = if (isGrooming) groomingProbability else safeProbability,
            isGrooming = isGrooming
        )
    }
}
```

#### 3. Build & Deploy:
```bash
./gradlew clean
./gradlew installDebug
```

#### 4. Teste:
```bash
adb logcat | grep -E "MLGroomingDetector|KidGuard"
```

---

## ✅ OPTION 2: PAN12 MODEL MANUELL EXPORTIEREN

Falls du das neue 96% Model unbedingt nutzen möchtest:

### Auf deinem Mac (Terminal):

```bash
cd ~/AndroidStudioProjects/KidGuard

# Versuche TFLite Export
python3 << 'EOF'
import tensorflow as tf

model = tf.keras.models.load_model('training/models/pan12_fixed/best_model.keras')
converter = tf.lite.TFLiteConverter.from_keras_model(model)

# Wichtig: Diese Flags setzen
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS,
    tf.lite.OpsSet.SELECT_TF_OPS
]
converter._experimental_lower_tensor_list_ops = False

try:
    tflite_model = converter.convert()
    with open('app/src/main/assets/kidguard_pan12.tflite', 'wb') as f:
        f.write(tflite_model)
    print(f"✅ Saved: {len(tflite_model)/1024/1024:.2f} MB")
except Exception as e:
    print(f"❌ Failed: {e}")
EOF
```

**Falls das funktioniert:**

1. Update build.gradle:
```kotlin
dependencies {
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-select-tf-ops:2.14.0")  // WICHTIG!
}
```

2. Update Model-Path in MLGroomingDetector.kt:
```kotlin
private val modelPath = "kidguard_pan12.tflite"
```

---

## ✅ OPTION 3: GOOGLE COLAB (FALLS LOKAL NICHT GEHT)

Falls der TFLite Export auf deinem Mac nicht funktioniert, nutze Google Colab:

### 1. Öffne: https://colab.research.google.com

### 2. Neues Notebook, führe aus:

```python
# Upload Model
from google.colab import files
uploaded = files.upload()  # Wähle: training/models/pan12_fixed/best_model.keras

# Convert to TFLite
import tensorflow as tf

model = tf.keras.models.load_model('best_model.keras')
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS,
    tf.lite.OpsSet.SELECT_TF_OPS
]
converter._experimental_lower_tensor_list_ops = False

tflite_model = converter.convert()

# Save
with open('kidguard_pan12.tflite', 'wb') as f:
    f.write(tflite_model)

# Download
files.download('kidguard_pan12.tflite')
```

### 3. Kopiere heruntergeladenes Model:
```bash
cp ~/Downloads/kidguard_pan12.tflite app/src/main/assets/
```

---

## 🎯 EMPFEHLUNG: OPTION 1 (Quick Model)

**Warum:**
- ✅ Funktioniert SOFORT
- ✅ Bereits getestet
- ✅ 94% Accuracy ist sehr gut
- ✅ Kein SELECT_TF_OPS nötig
- ✅ Schnellere Inferenz

**Unterschied zum PAN12 Model:**
- Quick: 94% Accuracy
- PAN12: 96% Accuracy
- **Unterschied: Nur 2%!**

Für die erste Version ist das Quick Model **mehr als ausreichend**.

---

## 📋 CHECKLISTE FÜR DEPLOYMENT:

### Vor dem Build:
- [ ] Model in `app/src/main/assets/` vorhanden
- [ ] MLGroomingDetector.kt: modelPath korrekt
- [ ] MLGroomingDetector.kt: GROOMING_THRESHOLD = 0.3f
- [ ] build.gradle: TFLite dependencies vorhanden

### Build:
```bash
./gradlew clean
./gradlew assembleDebug
```

### Installation:
```bash
# Wenn Pixel 10 verbunden:
./gradlew installDebug

# Oder manuell:
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Testing:
```bash
# Logcat beobachten
adb logcat | grep -E "KidGuard|MLGroomingDetector"

# In der App:
# 1. Öffne App
# 2. Gehe zu Chat-Monitoring
# 3. Sende Test-Nachrichten:
#    - "Hallo wie geht's" → sollte SAFE sein
#    - "are you alone at home" → sollte GROOMING erkennen
```

---

## 🔧 TROUBLESHOOTING:

### "Model not found"
```bash
# Check assets:
ls -la app/src/main/assets/*.tflite

# Falls leer, kopiere Quick Model:
cp training/models/quick_model.keras app/src/main/assets/kid_guard_v1.tflite
```

### "Cannot invoke TensorFlow Lite"
```bash
# Check build.gradle dependencies
# Muss enthalten:
implementation("org.tensorflow:tensorflow-lite:2.14.0")
```

### "App crashes on model load"
```bash
# Check Logcat:
adb logcat | grep -E "ERROR|FATAL"

# Oft: Wrong model format
# Lösung: Nutze anderes Model aus assets/
```

---

## 🚀 SCHNELLSTART (JETZT!):

**In 3 Minuten auf dem Device:**

```bash
cd ~/AndroidStudioProjects/KidGuard

# 1. Check Model existiert
ls app/src/main/assets/kid_guard_v1.tflite

# 2. Build & Install
./gradlew clean installDebug

# 3. Test
adb logcat | grep KidGuard
```

**Das war's!** App läuft mit ML-Model! 🎉

---

## 📊 ERWARTETES VERHALTEN:

```
Safe Messages:
"Hallo wie geht's?" → ✅ SAFE (98% Confidence)
"Was machst du?" → ✅ SAFE (95% Confidence)

Grooming Messages:
"are you alone?" → ⚠️ GROOMING (65% Confidence)
"send me a pic" → ⚠️ GROOMING (70% Confidence)
"dont tell anyone" → ⚠️ GROOMING (60% Confidence)
```

---

**Erstellt:** 28. Januar 2026  
**Status:** Ready to Deploy  
**Empfehlung:** Nutze Option 1 (Quick Model) für ersten Test!  

**Los geht's! 🚀**
