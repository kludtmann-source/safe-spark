#!/bin/bash
# Quick-Start Script für Phase 3 Training
# Führt alle notwendigen Schritte automatisch aus

set -e  # Stop bei Fehlern

echo "================================================"
echo "🛡️  KidGuard Phase 3 - Quick Start"
echo "================================================"

# 1. Prüfe Python Installation
echo ""
echo "1️⃣  Prüfe Python-Installation..."
if ! command -v python3 &> /dev/null; then
    echo "❌ Python3 nicht gefunden! Bitte installieren."
    exit 1
fi
echo "✅ Python3 gefunden: $(python3 --version)"

# 2. Erstelle Virtual Environment (falls nicht vorhanden)
if [ ! -d "venv" ]; then
    echo ""
    echo "2️⃣  Erstelle Virtual Environment..."
    python3 -m venv venv
    echo "✅ Virtual Environment erstellt"
else
    echo ""
    echo "2️⃣  Virtual Environment existiert bereits"
fi

# 3. Aktiviere Virtual Environment
echo ""
echo "3️⃣  Aktiviere Virtual Environment..."
source venv/bin/activate
echo "✅ Virtual Environment aktiviert"

# 4. Installiere Dependencies
echo ""
echo "4️⃣  Installiere Dependencies..."
pip install --upgrade pip -q
pip install -r requirements.txt -q
echo "✅ Dependencies installiert"

# 5. Prüfe ob Dataset existiert
if [ ! -f "data/grooming_stages_dataset.json" ]; then
    echo ""
    echo "❌ Dataset nicht gefunden: data/grooming_stages_dataset.json"
    exit 1
fi
echo ""
echo "5️⃣  Dataset gefunden ($(jq length data/grooming_stages_dataset.json) Beispiele)"

# 6. Erstelle Models-Verzeichnis
mkdir -p models
echo "✅ Models-Verzeichnis bereit"

# 7. Starte Training
echo ""
echo "================================================"
echo "6️⃣  STARTE TRAINING..."
echo "================================================"
echo ""

cd scripts
python3 train_grooming_detection.py

# 8. Zeige Ergebnis
echo ""
echo "================================================"
echo "✅ TRAINING ABGESCHLOSSEN!"
echo "================================================"
echo ""
echo "📦 Output-Dateien:"
ls -lh ../models/grooming_detector.tflite 2>/dev/null || echo "⚠️  Model nicht gefunden"
ls -lh ../models/grooming_detector_metadata.json 2>/dev/null || echo "⚠️  Metadata nicht gefunden"

echo ""
echo "🚀 Nächste Schritte:"
echo "   1. Kopiere Model in Android App:"
echo "      cp models/grooming_detector.tflite ../app/src/main/assets/"
echo ""
echo "   2. Teste auf Pixel 10:"
echo "      ./gradlew assembleDebug && adb install app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "   3. Überwache Logs:"
echo "      adb logcat | grep KidGuardEngine"

echo ""
echo "================================================"
