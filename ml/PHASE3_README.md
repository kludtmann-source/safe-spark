# Phase 3: BKA/LKA Deep Dive - Grooming Stage Detection (AKTUALISIERT 2026-01-25)

## Überblick

Phase 3 implementiert ein Machine Learning Model zur Erkennung der **"Six Stages of Grooming"** nach der kriminologischen Taxonomie von Lanning/BKA.

## Datensatz

**Datei:** `ml/data/grooming_patterns.json`

Synthetischer Datensatz mit **40 Beispielen** in authentischem deutschen Chat-Slang (2026).

### Label-Kategorien:

1. **STAGE_TRUST** (8 Beispiele)
   - Aufbau einer "besonderen Freundschaft"
   - Manipulation durch Schmeichelei
   - Beispiel: *"du bist viel reifer als die anderen"*

2. **STAGE_NEEDS** (8 Beispiele)
   - Materielle Anreize (Robux, Skins, Guthaben)
   - Geschenke als Druckmittel
   - Beispiel: *"brauchst du v-bucks? ich kann dir welche kaufen"*

3. **STAGE_ISOLATION** (8 Beispiele)
   - Versuche, Kommunikation zu verstecken
   - Geheimhaltung gegenüber Eltern
   - Beispiel: *"schreib mir lieber auf telegram, da ists sicherer"*

4. **STAGE_ASSESSMENT** (8 Beispiele)
   - Risiko-Check der Umgebung
   - Fragen nach Privatsphäre
   - Beispiel: *"bist du grad allein im zimmer?"*

5. **STAGE_SAFE** (8 Beispiele)
   - Harmloser Gaming-/Schul-Chat
   - Kontrollgruppe für False-Positive-Reduktion
   - Beispiel: *"hast du die hausaufgaben in mathe gemacht?"*

## Training-Skript

**Datei:** `ml/scripts/train_grooming_model.py`

### Features:
- **Character-Level Tokenization** für Tippfehler-Robustheit
- **Bidirectional LSTM** (128 units) für Kontext-Verständnis
- **Multi-Stage Classification** (5 Klassen)
- **TensorFlow Lite Konvertierung** mit INT8-Quantisierung
- **Early Stopping** zur Overfitting-Vermeidung
- **Learning Rate Scheduling** für optimale Konvergenz

### Ausführung:

```bash
cd ml/scripts
python3 train_grooming_model.py
```

### Output:
- `ml/models/grooming_detector.tflite` (< 5MB)
- `ml/models/grooming_vocabulary.json`

## Model-Architektur

```
Input (Text) → Character-Level Tokenization → Embedding (64D)
              ↓
       Bidirectional LSTM (128 units)
              ↓
      Global Max Pooling (Attention)
              ↓
        Dense Layer (64, ReLU)
              ↓
         Dropout (0.5)
              ↓
        Dense Layer (32, ReLU)
              ↓
         Dropout (0.3)
              ↓
      Output (5 Classes, Softmax)
```

## Performance-Ziele (Phase 4)

- ✅ Model-Größe: **< 5MB**
- ⏱️ Inferenz-Zeit: **< 50ms** (Pixel 10)
- 🎯 Accuracy: **> 90%** auf Test-Set
- ⚠️ False-Positive Rate: **< 5%**

## Integration in Android App

1. **Model kopieren:**
   ```bash
   cp ml/models/grooming_detector.tflite app/src/main/assets/
   ```

2. **Metadata laden:**
   ```kotlin
   val metadata = loadJsonFromAssets("grooming_detector_metadata.json")
   ```

3. **Inference-Code:** Siehe `KidGuardEngine.kt`

## Nächste Schritte

### Phase 3 Erweiterung:
- [ ] Datensatz auf **200+ Beispiele** erweitern
- [ ] **Hard Example Mining** implementieren
- [ ] **Sequence Detection** für Gesprächsverläufe (Multi-Turn)

### Phase 4 Integration:
- [ ] Model auf Pixel 10 testen
- [ ] Performance-Messung (Inferenz-Zeit)
- [ ] False-Positive Tests mit echten Kinder-Chats

## Datenschutz & Ethik

⚠️ **Privacy-by-Design:**
- Alle Daten bleiben **On-Device**
- Keine Cloud-Kommunikation
- Eltern werden nur bei **konkretem Risiko** benachrichtigt

🛡️ **Safety-by-Design:**
- Synthetische Daten (keine echten Opfer)
- Validierung durch Kriminologen empfohlen
- Regelmäßige Updates bei neuen Grooming-Taktiken

## Wissenschaftliche Referenzen

- PAN-12 Sexual Predator Identification Dataset
- Lanning, K. (2010). "Child Molesters: A Behavioral Analysis"
- BKA Kriminalstatistik "Cybergrooming" (2024/2025)

---

**Status:** ✅ Phase 3 abgeschlossen (Basis-Implementation)  
**Datum:** 25.01.2026  
**Nächste Phase:** Phase 4 - On-Device Performance Testing
