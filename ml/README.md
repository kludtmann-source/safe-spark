# 🚀 KidGuard ML Training - Phase 1 & 2

## 📋 Übersicht

Dieses Verzeichnis enthält alle Scripts für das Training eines **kontextbewussten TensorFlow Lite Modells < 5MB** für On-Device Risikoerkennung auf dem Pixel 10.

---

## 🎯 Ziele

### Phase 1: Synthetische Basis-Daten
- **2000 Beispiele** generieren (Safe, Toxic, Grooming)
- **Keyword-Basis** überwinden
- **Kontext-Verständnis** aufbauen

### Phase 2: Sliding Window & Kontext
- **3-5 Nachrichten Fenster** für Gesprächsverläufe
- **Grooming-Pattern Erkennung** (Six Stages)
- **< 5MB Modell-Größe**
- **< 50ms Inferenz-Zeit** auf Pixel 10

---

## 📦 Installation

### 1. Python Environment erstellen

```bash
cd /Users/knutludtmann/AndroidStudioProjects/KidGuard/ml

# Virtual Environment erstellen
python3 -m venv venv

# Aktivieren
source venv/bin/activate  # macOS/Linux
# oder
venv\Scripts\activate  # Windows

# Dependencies installieren
pip install tensorflow==2.14.0 numpy scikit-learn
```

---

## 🚀 Training Durchführen

### Schritt 1: Daten generieren

```bash
cd scripts
python generate_data.py
```

**Output:**
```
Generated 2000 examples
Saved to data/training_data.json
```

### Schritt 2: Modell trainieren

```bash
python train_model.py
```

**Expected Output:**
```
✅ Keras Modell gespeichert: models/kidguard_model.h5
✅ TFLite Modell gespeichert: models/kidguard_model.tflite
📦 Größe: 2.3 MB
⏱️  Inferenz-Zeit: 12.5 ms
✅ Performance-Ziel erreicht (< 50ms)!
```

---

## 📂 Output-Struktur

Nach dem Training:

```
ml/
├── data/
│   └── training_data.json          # Generierte Training-Daten
├── models/
│   ├── kidguard_model.tflite       # ⭐ Für Android
│   ├── vocabulary.json             # ⭐ Für Android  
│   └── kidguard_model.h5           # Keras Backup
└── scripts/
    ├── generate_data.py            # Datengenerierung
    └── train_model.py              # Modell Training
```

---

## 📱 Android Integration

### Schritt 1: Assets kopieren

```bash
# Kopiere Modell und Vokabular
cp models/kidguard_model.tflite ../app/src/main/assets/
cp models/vocabulary.json ../app/src/main/assets/
```

### Schritt 2: MLRiskAnalyzer implementieren

Siehe: `ML_INTEGRATION_GUIDE.md`

---

## 🧪 Testing

### Test-Beispiele

```python
test_texts = [
    "Hey wie geht's?",              # → Safe
    "Du bist so dumm",              # → Toxic
    "Das bleibt unser Geheimnis",   # → Grooming
    "Schick mir ein Bild",          # → Grooming
    "Hast du Hausaufgaben?",        # → Safe
]
```

### Performance-Ziele

- ✅ **Modell-Größe:** < 5 MB
- ✅ **Inferenz-Zeit:** < 50 ms
- ✅ **Accuracy:** > 85%
- ✅ **False Positives:** < 10%

---

## 🔄 Training-Konfiguration

### Modell-Parameter

```python
CONFIG = {
    "max_words": 500,              # Vokabular-Größe
    "max_sequence_length": 30,     # Max Wörter pro Nachricht
    "embedding_dim": 16,           # Embedding-Dimension
    "context_window": 3,           # Sliding Window Größe
    "epochs": 50,
    "batch_size": 32,
    "num_classes": 3,              # Safe, Toxic, Grooming
}
```

### Architektur

```
Input (30 Tokens)
    ↓
Embedding (16D)
    ↓
Conv1D (32 Filters, Kernel=3)
    ↓
GlobalAveragePooling1D
    ↓
Dense (16 Units, ReLU)
    ↓
Dropout (0.5)
    ↓
Dense (3 Units, Softmax)
    ↓
Output [Safe, Toxic, Grooming]
```

**Gesamtgröße:** ~2-3 MB (mit INT8 Quantization)

---

## 📊 Training-Daten Details

### Verteilung

- **40% Safe** (800 Beispiele)
  - Hausaufgaben, Gaming, Sport, Alltag
  
- **30% Toxic** (600 Beispiele)
  - Beleidigungen, Scam, Druck, Aggression
  
- **30% Grooming** (600 Beispiele)
  - Geheimnisse, Geschenke, Treffen, Sexualisierung

### Beispiele

**Safe:**
```json
{
  "text": "Hast du die Mathe-Hausaufgaben schon gemacht?",
  "label": 0,
  "category": "safe_hausaufgaben"
}
```

**Toxic:**
```json
{
  "text": "Du bist so dumm",
  "label": 1,
  "category": "toxic_beleidigung"
}
```

**Grooming:**
```json
{
  "text": "Das bleibt unser Geheimnis okay?",
  "label": 2,
  "category": "grooming_geheimnisse"
}
```

---

## 🔧 Optimierungen

### Modell-Größe reduzieren

1. **Vokabular verkleinern:**
   ```python
   CONFIG["max_words"] = 300  # statt 500
   ```

2. **Embedding-Dimension reduzieren:**
   ```python
   CONFIG["embedding_dim"] = 12  # statt 16
   ```

3. **INT8 Quantization:**
   ```python
   converter.optimizations = [tf.lite.Optimize.DEFAULT]
   ```

### Performance verbessern

1. **Sequence-Length reduzieren:**
   ```python
   CONFIG["max_sequence_length"] = 20  # statt 30
   ```

2. **Conv1D statt LSTM:**
   - Schneller
   - Weniger Parameter
   - Besser für On-Device

---

## 🚨 Troubleshooting

### Problem: Modell > 5MB

**Lösung:**
```python
# Reduziere Konfiguration
CONFIG["max_words"] = 300
CONFIG["embedding_dim"] = 12
```

### Problem: Inferenz > 50ms

**Lösung:**
```python
# Reduziere Sequence-Length
CONFIG["max_sequence_length"] = 20

# Nutze Conv1D statt LSTM
model = create_lightweight_model()  # nicht create_context_model()
```

### Problem: Zu viele False Positives

**Lösung:**
```bash
# Mehr Safe-Beispiele generieren
python generate_data.py --safe-ratio 0.5
```

---

## 📈 Nächste Schritte (Phase 3 & 4)

### Phase 3: BKA/LKA Daten

1. **Echte Protokolle analysieren**
2. **Hard Example Mining**
3. **Fine-Tuning des Modells**

### Phase 4: On-Device Test

1. **Performance-Messung auf Pixel 10**
2. **False-Positive Rate messen**
3. **Feedback-Loop implementieren**

---

## 📝 Lizenz & Hinweise

⚠️ **Wichtig:**
- Training-Daten sind **synthetisch**
- Für Produktions-Nutzung: **Echte Daten** (BKA/LKA) notwendig
- **Privacy-konform:** Alles On-Device, keine Cloud

---

## 📚 Weitere Dokumentation

- `ML_INTEGRATION_GUIDE.md` - Android Integration
- `PERFORMANCE_TUNING.md` - Optimierungs-Tipps
- `DATASET_EXPANSION.md` - Mehr Training-Daten

---

**Erstellt:** 25. Januar 2026  
**Status:** Phase 1 & 2 implementiert  
**Ziel:** < 5MB, < 50ms, On-Device
