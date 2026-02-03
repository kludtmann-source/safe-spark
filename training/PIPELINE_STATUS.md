# 📊 KidGuard Training Pipeline - Status Report

**Datum:** 25. Januar 2026  
**Status:** Schritt 1 von 4 abgeschlossen

---

## ✅ **ABGESCHLOSSEN**

### **Schritt 1: Übersetzung EN → DE** ✅

```
✅ kidguard_german_train.json erstellt
✅ kidguard_german_test.json erstellt
✅ 749 Training-Samples verarbeitet
✅ 188 Test-Samples verarbeitet
```

**Details:**
- Englische PAN12-Texte wurden übersetzt
- Deutsche Scientific Papers beibehalten
- Format mit `language: "de"` markiert
- Einige kurze Texte ("sorry", "ok") wurden ggf. nicht übersetzt (zu kurz)

---

## ⏳ **NÄCHSTE SCHRITTE**

### **Schritt 2: Data Augmentation** (20-30 Min)

```bash
cd ~/AndroidStudioProjects/KidGuard
python3 training/augment_data.py
```

**Was wird gemacht:**
- Lädt `kidguard_german_train.json`
- Erweitert Grooming-Klassen auf je 150 Samples
- Methoden: Back-Translation + Synonym-Replacement
- Output: `training/data/augmented/kidguard_augmented_train.json`

**Erwartetes Ergebnis:**
```
VORHER (nach Übersetzung): 749 Samples
NACHHER (augmented): ~1,200+ Samples

Grooming-Klassen:
  STAGE_TRUST: ~30 → 150 Samples
  STAGE_NEEDS: ~20 → 150 Samples
  STAGE_ISOLATION: ~15 → 150 Samples
  STAGE_ASSESSMENT: ~10 → 150 Samples
  STAGE_SEXUAL: ~10 → 150 Samples
```

---

### **Schritt 3: Model Training** (15-30 Min)

```bash
python3 training/train_model.py
```

**WICHTIG:** Das Script muss aktualisiert werden, um die augmented Daten zu nutzen!

**Änderung erforderlich in `train_model.py`:**
```python
# Zeile ~430 - Ändere Input-Datei:
X_train, y_train, X_val, y_val, X_test, y_test, class_weights = trainer.load_data(
    train_file='training/data/augmented/kidguard_augmented_train.json',  # ← ÄNDERN!
    test_file='training/data/combined/kidguard_german_test.json'
)
```

**Output:**
- `training/models/kidguard_best.keras`
- `app/src/main/assets/kidguard_model.tflite`
- `training/reports/training_history.png`

---

### **Schritt 4: Evaluation** (2-5 Min)

```bash
python3 training/evaluate_model.py
```

**Output:**
- Classification Report
- Confusion Matrix
- False-Negative-Analyse

---

## 🎯 **QUICK CONTINUE**

Führe die verbleibenden Schritte nacheinander aus:

```bash
# Schritt 2
python3 training/augment_data.py

# Schritt 3 (nach Augmentation)
python3 training/train_model.py

# Schritt 4 (nach Training)
python3 training/evaluate_model.py
```

---

## 📊 **AKTUELLE DATEN-STATISTIK**

```
✅ Übersetzt: 749 Training + 188 Test = 937 Samples
⏳ Nach Augmentation: ~1,200+ Samples
⏳ Nach Training: TFLite Model (~120 KB)
```

---

## ⚠️  **WICHTIGE HINWEISE**

1. **Dependencies prüfen:**
   ```bash
   pip install deep-translator tensorflow scikit-learn matplotlib seaborn tqdm
   ```

2. **train_model.py anpassen:**
   - Input-Pfad auf `augmented_train.json` ändern (nach Schritt 2)

3. **Grooming-Recall > 95%:**
   - Das ist das kritische Ziel!
   - Wird in Schritt 4 (Evaluation) gemessen

---

## 🚀 **NEXT ACTION**

```bash
python3 training/augment_data.py
```

**Dauer:** ~20-30 Minuten  
**Output:** training/data/augmented/kidguard_augmented_train.json

---

**Status:** 1/4 Schritte abgeschlossen ✅  
**Fortschritt:** 25% ████░░░░░░░░░░░░

**Weiter so! 💪**
