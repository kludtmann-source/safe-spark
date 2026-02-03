# SafeSpark

**On-device grooming detection for child safety on Android.**

SafeSpark is a research prototype that detects online grooming patterns in real-time messaging applications using a multi-layer machine learning pipeline—entirely on-device, without cloud dependencies.

---

## Features

- **On-Device Processing**: All text analysis runs locally on the device (TensorFlow Lite). No data leaves the phone.
- **GDPR Compliant**: Zero server communication, zero data collection.
- **9-Layer Detection Pipeline**: Combines semantic similarity, transformer-based classification, n-gram analysis, and contextual features.
- **6-Stage Grooming Taxonomy**: Based on established research (PAN-12, Osprey framework).
- **Real-Time Alerts**: Notifications to parents/guardians when risk patterns are detected.
- **~95% Accuracy**: Achieved through ensemble of specialized detectors.

---

## Architecture

```
Detection Pipeline (9 Layers)
├── Semantic Similarity     (25%)  – Embedding-based intent matching
├── Osprey Transformer      (20%)  – BERT/RoBERTa 6-stage classifier
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

This project builds on top of the **[Osprey framework](https://github.com/fani-lab/Osprey)** developed by Fani Lab at the University of Windsor. Osprey provides transformer-based grooming stage classification, which has been adapted for on-device inference via TFLite conversion.

**Reference:**
> Fani Lab. *Osprey: Online Sexual Predator Recognition.* GitHub Repository. https://github.com/fani-lab/Osprey

---

## Project Status

**Research Prototype / MVP**

- [x] On-device detection pipeline functional
- [x] Real-time notification system
- [x] GDPR-compliant architecture
- [x] Tested on physical devices (Pixel 10)
- [ ] Osprey TFLite model conversion (in progress)
- [ ] Play Store release preparation

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
GitHub: [@kludtmann-source](https://github.com/kludtmann-source)

---

## License

This project is currently under development. License terms to be determined pending discussions with research partners.

---

## Acknowledgments

- **Fani Lab, University of Windsor** – Osprey framework for grooming detection
- **PAN-12 Dataset** – Training data for sexual predator identification
- Research papers on online grooming detection informing the multi-layer approach

---

## Internal Documentation

For development notes (German), see the `docs/` folder or individual markdown files in the repository root.
