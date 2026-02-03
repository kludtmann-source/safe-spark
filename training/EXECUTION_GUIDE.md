# 🚀 KidGuard Training Pipeline - Ausführungs-Guide

**Status:** ✅ ALLE 4 SCRIPTS ERSTELLT  
**Datum:** 26. Januar 2026

---

## 📋 **ÜBERSICHT**

Diese Pipeline löst das Problem der **unbalancierten englischen Daten** und trainiert ein optimiertes Model:

| Problem | Lösung | Script |
|---------|--------|--------|
| 🇬🇧 **Englische PAN12-Daten** | Übersetzung EN → DE | `translate_dataset.py` |
| ⚖️  **Unbalanciert** (18% Grooming) | Data Augmentation | `augment_data.py` |
| 📉 **Niedriger Recall** | Class Weights + Focal Loss | `train_model.py` |
| 🔍 **Keine Evaluation** | Detaillierte Metriken | `evaluate_model.py` |

---

## 🎯 **AUSFÜHRUNGS-REIHENFOLGE**

### **Schritt 1: Dependencies installieren**

```bash
cd ~/AndroidStudioProjects/KidGuard

# Translation
pip install deep-translator

# Machine Learning
pip install tensorflow scikit-learn

# Visualisierung
pip install matplotlib seaborn

# Progress Bars
pip install tqdm
```

---

### **Schritt 2: Übersetzung EN → DE** ⭐ **PRIORITÄT**

```bash
python3 training/translate_dataset.py
```

**Was passiert:**
- Lädt `kidguard_train.json` und `kidguard_test.json`
- Erkennt englische Texte (PAN12-Daten)
- Übersetzt mit GoogleTranslator
- Optimiert für Jugendsprache (7-11 Jahre)
- Rate-Limiting (0.5s pro Request)

**Output:**
```
training/data/combined/
├── kidguard_german_train.json  ✅ 749 Samples (DE)
└── kidguard_german_test.json   ✅ 188 Samples (DE)
```

**Dauer:** ~10-15 Minuten (abhängig von Google API)

**Beispiel Output:**
```json
{
  "text": "du wirkst sehr reif für dein alter",
  "original_text": "you seem very mature for your age",
  "label": "STAGE_TRUST",
  "source": "pan12",
  "language": "de"
}
```

---

### **Schritt 3: Data Augmentation**

```bash
python3 training/augment_data.py
```

**Was passiert:**
- Lädt `kidguard_german_train.json`
- Analysiert Label-Distribution
- Erweitert Grooming-Klassen auf je **150 Samples**
- Methoden:
  - Back-Translation (DE → EN → DE)
  - Synonym-Replacement
- Shuffled Output

**Output:**
```
training/data/augmented/
└── kidguard_augmented_train.json  ✅ ~1,200+ Samples
```

**Erwartete Distribution:**
```
STAGE_SAFE: ~750 Samples
STAGE_TRUST: 150 Samples
STAGE_NEEDS: 150 Samples
STAGE_ISOLATION: 150 Samples
STAGE_ASSESSMENT: 150 Samples
STAGE_SEXUAL: 150 Samples
```

**Dauer:** ~20-30 Minuten (Back-Translation)

---

### **Schritt 4: Model Training**

```bash
python3 training/train_model.py
```

**Was passiert:**
- Lädt augmented Training-Daten
- Berechnet **Class Weights** (automatisch)
- Trainiert CNN-basiertes Model
- **Early Stopping** auf Validation **Grooming-Recall** (nicht Accuracy!)
- Speichert:
  - Best Model (`kidguard_best.keras`)
  - TFLite Model (`app/src/main/assets/kidguard_model.tflite`)
  - Training-History Plot
  - Metadata (Tokenizer, Labels)

**Konfiguration:**
```python
Vocab Size: 5,000
Embedding Dim: 128
Max Length: 50
Epochs: 50 (Early Stopping)
Batch Size: 32
Loss: Sparse Categorical Crossentropy + Class Weights
```

**Kritische Metrik:**
```
🎯 Grooming-Recall > 95% (MUSS erreicht werden!)
```

**Output:**
```
training/models/
├── kidguard_best.keras           ✅ Best Model
└── training_history.png          ✅ Training-Verlauf

app/src/main/assets/
├── kidguard_model.tflite         ✅ Production Model (~120 KB)
└── kidguard_metadata.json        ✅ Tokenizer + Labels
```

**Dauer:** ~15-30 Minuten (abhängig von Hardware)

---

### **Schritt 5: Evaluation**

```bash
python3 training/evaluate_model.py
```

**Was passiert:**
- Lädt Best Model + Test-Daten
- Berechnet:
  - Classification Report (Precision, Recall, F1 pro Klasse)
  - Confusion Matrix (Visualisierung)
  - Per-Class Recall
  - Gesamt-Grooming-Recall
- **False-Negative-Analyse** (kritisch!)

**Output:**
```
training/reports/
├── confusion_matrix.png          ✅ Visualisierung
└── false_negatives.json          ✅ Verpasste Grooming-Messages
```

**Erwartete Metriken:**
```
Accuracy: 92-94%
Grooming-Recall: > 95%
False-Negatives: < 3%
```

**Dauer:** ~2-5 Minuten

---

## 📊 **ERWARTETE VERBESSERUNG**

| Metrik | Vorher (207 Samples) | Nachher (1,200+ Samples) |
|--------|----------------------|---------------------------|
| **Dataset Size** | 207 | **1,200+** |
| **Grooming Samples** | ~100 | **750** |
| **Vocabulary** | 381 | **2,000+** |
| **Accuracy** | 90.5% | **92-94%** |
| **Grooming-Recall** | 88% | **> 95%** |
| **False Negatives** | ~5% | **< 3%** |

---

## ⚠️  **TROUBLESHOOTING**

### **Problem 1: deep-translator Fehler**

```bash
# Fehler: ModuleNotFoundError: No module named 'deep_translator'
pip install deep-translator

# Alternative: googletrans
pip install googletrans==4.0.0rc1
```

### **Problem 2: TensorFlow Memory Error**

```python
# Reduziere Batch Size in train_model.py
batch_size=16  # statt 32
```

### **Problem 3: Grooming-Recall < 95%**

**Optionen:**
1. Mehr Augmentation (`target=200` in `augment_data.py`)
2. Höhere Class Weights
3. Focal Loss aktivieren (`use_focal_loss=True` in `train_model.py`)
4. Mehr Epochs

### **Problem 4: Google Translation Rate Limit**

```python
# Erhöhe Delay in translate_dataset.py
DatasetTranslator(rate_limit_delay=1.0)  # statt 0.5
```

---

## 🎯 **QUICK START (Alle Schritte)**

```bash
# 1. Dependencies
pip install deep-translator tensorflow scikit-learn matplotlib seaborn tqdm

# 2. Übersetzung
python3 training/translate_dataset.py

# 3. Augmentation
python3 training/augment_data.py

# 4. Training
python3 training/train_model.py

# 5. Evaluation
python3 training/evaluate_model.py
```

**Gesamtdauer:** ~1-2 Stunden

---

## ✅ **SUCCESS CRITERIA**

**Training ist erfolgreich wenn:**

✅ Grooming-Recall > 95%  
✅ False-Negatives < 3%  
✅ TFLite Model < 1 MB  
✅ Alle 6 Klassen vertreten  
✅ Deutsche Texte korrekt übersetzt  

---

## 📦 **FILES CREATED**

```
training/
├── translate_dataset.py          ✅ EN → DE Translation
├── augment_data.py              ✅ Data Augmentation
├── train_model.py               ✅ Model Training
├── evaluate_model.py            ✅ Evaluation
├── data/
│   ├── combined/
│   │   ├── kidguard_german_train.json
│   │   └── kidguard_german_test.json
│   └── augmented/
│       └── kidguard_augmented_train.json
├── models/
│   └── kidguard_best.keras
└── reports/
    ├── training_history.png
    ├── confusion_matrix.png
    └── false_negatives.json

app/src/main/assets/
├── kidguard_model.tflite
└── kidguard_metadata.json
```

---

## 🚀 **NEXT STEPS**

Nach erfolgreichem Training:

1. ✅ **Integration** in Android App (bereits vorhanden)
2. ✅ **Testing** auf Pixel 10
3. ✅ **Performance-Check** (< 50ms per Message)
4. ✅ **False-Negative-Review** (kritisch!)
5. ✅ **Production Deployment**

---

**Status:** ✅ **PIPELINE COMPLETE & READY**  
**Target:** Grooming-Recall > 95% für Production  
**Zielgruppe:** Deutschsprachige Kinder (7-11 Jahre)

**Let's train! 🚀**
