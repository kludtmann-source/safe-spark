#!/bin/bash
# KidGuard - Automated Deployment Script

echo "========================================"
echo "🚀 KidGuard Deployment auf Device"
echo "========================================"
echo ""

cd /Users/knutludtmann/AndroidStudioProjects/KidGuard

# Check if device is connected
echo "1️⃣ Checking device connection..."
if adb devices | grep -q "device$"; then
    echo "✅ Device connected"
    adb devices
else
    echo "❌ No device found!"
    echo "   Please connect your Pixel 10 via USB"
    exit 1
fi

echo ""
echo "2️⃣ Checking ML Model..."
if [ -f "app/src/main/assets/kid_guard_v1.tflite" ]; then
    echo "✅ Model found: kid_guard_v1.tflite"
    ls -lh app/src/main/assets/kid_guard_v1.tflite
elif [ -f "app/src/main/assets/grooming_detector.tflite" ]; then
    echo "✅ Model found: grooming_detector.tflite"
    ls -lh app/src/main/assets/grooming_detector.tflite
else
    echo "⚠️  No TFLite model found in assets"
    echo "   Available models:"
    ls -lh app/src/main/assets/*.tflite 2>/dev/null || echo "   None"
fi

echo ""
echo "3️⃣ Building APK..."
./gradlew clean
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Build successful"
else
    echo "❌ Build failed"
    echo "   Check error messages above"
    exit 1
fi

echo ""
echo "4️⃣ Installing on device..."
./gradlew installDebug

if [ $? -eq 0 ]; then
    echo "✅ Installation successful"
else
    echo "❌ Installation failed"
    exit 1
fi

echo ""
echo "5️⃣ Starting app..."
adb shell am start -n com.example.safespark/.MainActivity

echo ""
echo "========================================"
echo "✅ DEPLOYMENT COMPLETE!"
echo "========================================"
echo ""
echo "📱 App läuft auf deinem Device"
echo ""
echo "🔍 Live-Monitoring:"
echo "   adb logcat | grep -E 'KidGuard|MLGrooming'"
echo ""
echo "🧪 Testing:"
echo "   1. Öffne die App"
echo "   2. Gehe zu Chat-Monitoring"
echo "   3. Teste mit:"
echo "      - Safe: 'Hallo wie geht's?'"
echo "      - Grooming: 'are you alone?'"
echo ""
read -p "Möchtest du Logcat jetzt beobachten? (y/n): " watch_log

if [ "$watch_log" = "y" ] || [ "$watch_log" = "Y" ]; then
    echo ""
    echo "📊 Watching logs (Ctrl+C to stop)..."
    adb logcat | grep -E "KidGuard|MLGrooming|ERROR"
fi
