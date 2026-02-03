#!/bin/bash

# Quick Start Script für Option A Testing
#
# Usage: ./quick_start_option_a.sh

echo "🚀 KidGuard Option A - Quick Start"
echo "=================================="
echo ""

# Farben
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 1. Check if we're in the right directory
if [ ! -f "build.gradle.kts" ]; then
    echo "${RED}❌ Fehler: Nicht im KidGuard-Projekt-Verzeichnis!${NC}"
    echo "   Bitte navigiere zu: ~/AndroidStudioProjects/KidGuard"
    exit 1
fi

echo "${GREEN}✅ KidGuard-Projekt gefunden${NC}"
echo ""

# 2. Clean Build
echo "🧹 Step 1: Clean Build"
echo "----------------------"
./gradlew clean

if [ $? -ne 0 ]; then
    echo "${RED}❌ Clean fehlgeschlagen${NC}"
    exit 1
fi
echo "${GREEN}✅ Clean erfolgreich${NC}"
echo ""

# 3. Compile Kotlin
echo "⚙️  Step 2: Compile Kotlin Code"
echo "--------------------------------"
./gradlew compileDebugKotlin

if [ $? -ne 0 ]; then
    echo "${RED}❌ Compilation fehlgeschlagen!${NC}"
    echo ""
    echo "${YELLOW}Mögliche Fehler:${NC}"
    echo "1. StageProgressionDetector.kt Syntax-Fehler"
    echo "2. KidGuardEngine.kt Import-Fehler"
    echo "3. Gradle-Cache-Problem"
    echo ""
    echo "${YELLOW}Lösungen:${NC}"
    echo "• Öffne Android Studio und prüfe Errors"
    echo "• Führe 'Invalidate Caches / Restart' aus"
    echo "• Check die Error-Messages oben"
    exit 1
fi
echo "${GREEN}✅ Compilation erfolgreich${NC}"
echo ""

# 4. Build APK
echo "📦 Step 3: Build Debug APK"
echo "--------------------------"
./gradlew assembleDebug

if [ $? -ne 0 ]; then
    echo "${RED}❌ Build fehlgeschlagen!${NC}"
    exit 1
fi
echo "${GREEN}✅ Build erfolgreich${NC}"
echo ""

# 5. Check if device connected
echo "📱 Step 4: Check Device Connection"
echo "-----------------------------------"
DEVICES=$(adb devices | grep -w "device" | wc -l)

if [ $DEVICES -eq 0 ]; then
    echo "${YELLOW}⚠️  Kein Gerät verbunden${NC}"
    echo ""
    echo "Build erfolgreich, aber kein Gerät gefunden."
    echo "APK liegt unter: app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "Möchtest du trotzdem fortfahren? (y/n)"
    read -r response
    if [[ ! "$response" =~ ^[Yy]$ ]]; then
        exit 0
    fi
else
    echo "${GREEN}✅ Gerät gefunden: $DEVICES Device(s)${NC}"

    # 6. Install APK
    echo ""
    echo "📲 Step 5: Install APK on Device"
    echo "---------------------------------"
    ./gradlew installDebug

    if [ $? -ne 0 ]; then
        echo "${RED}❌ Installation fehlgeschlagen!${NC}"
        exit 1
    fi
    echo "${GREEN}✅ Installation erfolgreich${NC}"
    echo ""

    # 7. Start Logcat
    echo "📋 Step 6: Monitor Logcat"
    echo "-------------------------"
    echo "${YELLOW}Drücke Ctrl+C zum Beenden${NC}"
    echo ""
    adb logcat -c  # Clear logcat
    adb logcat | grep --line-buffered "KidGuardEngine\|TrigramDetector\|StageProgressionDetector\|TimeInvestmentTracker"
fi

echo ""
echo "${GREEN}🎉 Option A Testing Complete!${NC}"
