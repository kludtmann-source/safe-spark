# SafeSpark

**On-device grooming detection for child safety on Android.**

SafeSpark is a research prototype that detects online grooming patterns in real-time messaging applications using a multi-layer machine learning pipeline—entirely on-device, without cloud dependencies.

---

## Features

- **On-Device Processing**: All text analysis runs locally (TensorFlow Lite). No data leaves the device.
- **GDPR Compliant**: Zero server communication, zero data collection.
- **9-Layer Detection Pipeline**: Combines semantic similarity, transformer-based classification, n-gram analysis, and contextual features.
- **6-Stage Grooming Taxonomy**: Based on established research (PAN-12, Osprey framework).
- **Real-Time Alerts**: Notifications to parents/guardians when risk patterns are detected.
- **Multi-Layer Ensemble**: Combines specialized detectors for robust classification.

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

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| Platform | Android (API 26+) |
| Language | Kotlin |
| ML Runtime | TensorFlow Lite |
| Transformer Integration | Osprey framework (Fani Lab) |
| Build System | Gradle (Kotlin DSL) |

---

## Osprey Integration

This project builds on top of the **[Osprey framework](https://github.com/fani-lab/Osprey)** developed by **Prof. Fani's Lab** at the University of Windsor. Osprey provides transformer-based grooming stage classification using its established 6-stage taxonomy.

**Adaptation for SafeSpark:**
- Conversion pipeline for TFLite (on-device inference)
- Integration into 9-layer ensemble architecture
- Preservation of Osprey's stage classification semantics

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
- [x] Tested on physical devices
- [ ] Osprey model integration (TFLite conversion pending)
- [ ] Academic evaluation / benchmark comparison

---

## Repository Structure

```
SafeSpark_App/
├── app/                     # Android application
│   ├── src/main/
│   │   ├── java/.../        # Kotlin source code
│   │   │   ├── ml/          # Detection layers
│   │   │   └── detection/   # Semantic analysis
│   │   └── assets/          # TFLite models
├── training/                # ML training scripts
│   └── Osprey/              # Osprey framework integration
├── ml/                      # Model development
└── scripts/                 # Utility scripts
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
```

**Requirements:**
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34

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
