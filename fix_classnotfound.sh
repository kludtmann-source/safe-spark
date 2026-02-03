#!/bin/bash
# KidGuard - Fix ClassNotFoundException und Clean Build

echo "============================================"
echo "🔧 KidGuard ClassNotFoundException Fix"
echo "============================================"
echo ""

cd /Users/knutludtmann/AndroidStudioProjects/KidGuard

echo "1️⃣ Stoppe laufende Gradle-Prozesse..."
pkill -f gradle 2>/dev/null
sleep 2

echo "2️⃣ Lösche Build-Cache..."
rm -rf app/build
rm -rf .gradle
rm -rf build

echo "3️⃣ Lösche APK vom Device..."
adb uninstall com.example.safespark 2>/dev/null

echo ""
echo "4️⃣ Clean Build starten..."
./gradlew clean

echo ""
echo "5️⃣ Build mit Multidex..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "============================================"
    echo "✅ BUILD ERFOLGREICH!"
    echo "============================================"
    echo ""

    # Check if device connected
    if adb devices | grep -q "device$"; then
        echo "6️⃣ Installiere auf Device..."
        ./gradlew installDebug

        if [ $? -eq 0 ]; then
            echo ""
            echo "✅ INSTALLATION ERFOLGREICH!"
            echo ""
            echo "7️⃣ Starte App..."
            adb shell am start -n com.example.safespark/.MainActivity

            echo ""
            echo "============================================"
            echo "✅ APP LÄUFT!"
            echo "============================================"
            echo ""
            echo "📊 Logcat beobachten:"
            echo "   adb logcat | grep -E 'KidGuard|AndroidRuntime'"
        else
            echo "❌ Installation fehlgeschlagen"
        fi
    else
        echo "⚠️  Kein Device verbunden"
        echo "   APK liegt in: app/build/outputs/apk/debug/app-debug.apk"
    fi
else
    echo ""
    echo "============================================"
    echo "❌ BUILD FEHLGESCHLAGEN"
    echo "============================================"
    echo ""
    echo "Prüfe Fehler oben oder führe aus:"
    echo "   ./gradlew assembleDebug --stacktrace"
fi
