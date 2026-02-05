# SafeSpark

**On-device grooming detection for child safety on Android.**

SafeSpark is a research prototype that detects online grooming patterns in real-time messaging applications using a multi-layer machine learning pipeline—entirely on-device, without cloud dependencies.

---

## Features

- **On-Device Processing**: All text analysis runs locally (TensorFlow Lite). No data leaves the device.
- **GDPR Compliant**: Zero server communication, zero data collection. Conversation buffers are RAM-only.
- **9-Layer Detection Pipeline**: Combines semantic similarity, transformer-based classification, n-gram analysis, and contextual features.
- **6-Stage Grooming Taxonomy**: Based on established research (PAN-12, Osprey framework).
- **Conversation-Level Analysis**: Detects escalation patterns across multiple messages.
- **Real-Time Alerts**: Notifications to parents/guardians when risk patterns are detected.
- **Multi-Layer Ensemble**: Combines specialized detectors for robust classification.

---

## Grooming Stage Taxonomy

SafeSpark uses the Osprey 6-stage grooming taxonomy:

| Stage | Label | Description | Severity |
|-------|-------|-------------|----------|
| TRUST_BUILDING | Vertrauensaufbau | Compliments, "You're so mature", "I understand you" | 0.5 |
| ISOLATION | Vom Umfeld isolieren | "Don't tell anyone", platform switching (Telegram, Snapchat) | 0.7 |
| DESENSITIZATION | Sexuelles normalisieren | Introducing sexual topics casually | 0.8 |
| SEXUAL_CONTENT | Explizite Inhalte | Requesting pictures, explicit language | 0.9 |
| MAINTENANCE | Schweigen erzwingen | "This stays between us", threats | 0.85 |
| ASSESSMENT | Situationscheck | "Are you alone?", "Where are your parents?" | 0.6 |

---

## Architecture

```
Detection Pipeline (9 Layers)
├── Semantic Similarity     (25%)  – Embedding-based intent matching
├── Osprey Transformer      (20%)  – 6-stage classifier (Fani Lab)
├── ML Grooming Model       (20%)  – Custom TFLite CNN
├── Trigram Detector        (12%)  – N-gram pattern matching
├── Adult Context Detector  (10%)  – Role identification
├── Context-Aware Detector   (8%)  – Temporal/frequency analysis
├── Stage Progression        (3%)  – Escalation tracking
└── Keyword Patterns         (1%)  – Fallback detection
```

### Conversation Buffer

SafeSpark maintains a **GDPR-compliant, in-memory conversation buffer** to enable Osprey's conversation-level analysis:

```
┌─────────────────────────────────────────────────────────┐
│  ConversationBuffer (RAM only, no persistence)          │
├─────────────────────────────────────────────────────────┤
│  • Max 50 messages per contact                          │
│  • Auto-cleanup: messages older than 60 minutes removed │
│  • Pseudonymized contact IDs (hash-based)               │
│  • Cleared on service stop/app restart                  │
└─────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────┐
│  Osprey Conversation Analysis                           │
├─────────────────────────────────────────────────────────┤
│  Format: [CHILD] text [SEP] [CONTACT] text [SEP] ...    │
│  Detects: Stage progression (Trust → Isolation → ...)   │
│  Context Features:                                      │
│    • message_count                                      │
│    • contact_msg_ratio                                  │
│    • avg_msg_length                                     │
│    • conversation_duration_min                          │
│    • messages_per_minute                                │
└─────────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Platform | Android (API 26+) |
| Language | Kotlin |
| ML Runtime | TensorFlow Lite, ONNX Runtime |
| Semantic Encoder | MiniLM-L6-v2 (quantized ONNX) |
| Transformer Integration | Osprey framework (Fani Lab) |
| Build System | Gradle (Kotlin DSL) |

---

## Osprey Integration

This project builds on top of the **[Osprey framework](https://github.com/fani-lab/Osprey)** developed by **Prof. Fani's Lab** at the University of Windsor. Osprey provides transformer-based grooming stage classification using its established 6-stage taxonomy.

**Adaptation for SafeSpark:**
- Conversion pipeline for TFLite (on-device inference)
- Integration into 9-layer ensemble architecture
- Preservation of Osprey's stage classification semantics
- **Conversation-level analysis** via ConversationBuffer
- Stage progression tracking across message sequences

**Key Insight:** Osprey was designed to analyze full conversation transcripts. SafeSpark's ConversationBuffer recreates this capability on-device by accumulating messages per chat partner and feeding them to the Osprey classifier as a sequence.

**Citation:**
> Fani Lab. *Osprey: Online Sexual Predator Recognition.* University of Windsor. https://github.com/fani-lab/Osprey

---

## Project Status

**Research Prototype / MVP**

Current focus: Validating on-device feasibility of transformer-based grooming detection.

- [x] Multi-layer detection pipeline (functional)
- [x] On-device inference (TFLite)
- [x] GDPR-compliant architecture (no cloud)
- [x] Real-time notification system
- [x] Tested on physical devices (Pixel 10)
- [x] Conversation buffer for Osprey integration
- [x] Stage progression detection
- [ ] Full Osprey BERT model (TFLite conversion in progress)
- [ ] Academic evaluation / benchmark comparison

---

## Repository Structure

```
SafeSpark_App/
├── app/                     # Android application
│   ├── src/main/
│   │   ├── java/.../        # Kotlin source code
│   │   │   ├── ml/          # Detection layers
│   │   │   │   ├── ConversationBuffer.kt    # NEW: GDPR-compliant message buffer
│   │   │   │   ├── OspreyLocalDetector.kt   # Osprey TFLite integration
│   │   │   │   ├── MLGroomingDetector.kt    # CNN-based detection
│   │   │   │   ├── StageProgressionDetector.kt
│   │   │   │   └── TrigramDetector.kt
│   │   │   ├── detection/   # Semantic analysis
│   │   │   │   └── SemanticDetector.kt      # MiniLM embeddings
│   │   │   ├── logging/     # Structured logging
│   │   │   │   └── DetectionLogger.kt       # Finding-only logs
│   │   │   └── config/      # Configuration
│   │   │       └── DetectionConfig.kt       # Thresholds
│   │   └── assets/          # TFLite/ONNX models
├── training/                # ML training scripts
│   └── Osprey/              # Osprey framework integration
├── ml/                      # Model development
├── docs/                    # Documentation
└── scripts/                 # Utility scripts
```

---

## Key Components

### ConversationBuffer (`ml/ConversationBuffer.kt`)

GDPR-compliant, RAM-only message buffer for conversation-level analysis:

```kotlin
// Add message to buffer
ConversationBuffer.addMessage(contactId, ConversationMessage(
    text = "Are you alone?",
    authorId = contactId,
    timestamp = System.currentTimeMillis(),
    isLocalUser = false
))

// Get formatted input for Osprey
val ospreyInput = ConversationBuffer.getOspreyInput(contactId)
// → "[CONTACT] Are you alone? [SEP] [CHILD] Yes [SEP] ..."

// Get context features
val features = ConversationBuffer.getContextFeatures(contactId)
// → {message_count: 12, contact_msg_ratio: 0.6, ...}
```

### OspreyLocalDetector (`ml/OspreyLocalDetector.kt`)

TFLite-based Osprey classifier with conversation support:

```kotlin
// Single message analysis (fallback)
val result = ospreyDetector.predict("Are you alone?")

// Conversation analysis (preferred)
val result = ospreyDetector.analyzeConversation(
    messages = ConversationBuffer.getConversation(contactId),
    contextFeatures = ConversationBuffer.getContextFeatures(contactId)
)
// → OspreyResult(stage="ISOLATION", confidence=0.87, isRisk=true, ...)
```

### DetectionLogger (`logging/DetectionLogger.kt`)

Structured logging for findings only:

```kotlin
// Only positive findings are logged
DetectionLogger.logFinding(
    text = input,
    score = 0.87f,
    stage = GroomingStage.ISOLATION,
    method = "Osprey-Conversation",
    pattern = "TRUST_BUILDING → ISOLATION"
)

// Output in Logcat (filter: "SafeSpark-ALERT"):
// 21:43:12 🚨 Isolierung | 87% | 'Bist du alleine zu Hause'
```

---

## Building

```bash
# Clone repository
git clone https://github.com/kludtmann-source/safe-spark.git
cd safe-spark

# Build APK
./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk

# Build Release APK
./gradlew assembleRelease
adb install app/build/outputs/apk/release/app-release.apk
```

**Requirements:**
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34+

---

## Privacy & GDPR Compliance

SafeSpark is designed with privacy-first principles:

| Aspect | Implementation |
|--------|----------------|
| Data Storage | RAM only, no disk persistence |
| Conversation Buffer | Auto-cleared after 60 minutes |
| Contact IDs | Pseudonymized (hash-based, no names) |
| Network | Zero server communication |
| Logging | Metadata only, no message content in logs |
| Service Stop | All buffers cleared on stop/interrupt |

---

## Contact

**Knut Ludtmann**  
Research Prototype Developer  
GitHub: [@kludtmann-source](https://github.com/kludtmann-source)

---

## License

License terms pending discussions with research partners.

---

## Acknowledgments

- **Fani Lab, University of Windsor** – Osprey framework for grooming detection
- **PAN-12 Dataset** – Training data foundation for sexual predator identification
- Research literature on online grooming detection informing the multi-layer approach
