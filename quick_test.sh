#!/bin/bash

# KidGuard Quick Test Script
# Testet die App auf einem verbundenen Android-Gerät/Emulator

echo "🚀 KidGuard App - Quick Test"
echo "======================================"
echo ""

# Setze Pfade
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ADB=~/Library/Android/sdk/platform-tools/adb
PROJECT_DIR="/Users/knutludtmann/AndroidStudioProjects/KidGuard"

# Prüfe ob adb verfügbar ist
if [ ! -f "$ADB" ]; then
    echo "❌ Fehler: adb nicht gefunden!"
    echo "   Pfad: $ADB"
    exit 1
fi

# Prüfe verbundene Geräte
echo "📱 Prüfe verbundene Geräte..."
DEVICES=$($ADB devices | grep -v "List" | grep "device$" | wc -l)

if [ "$DEVICES" -eq 0 ]; then
    echo "❌ Kein Gerät verbunden!"
    echo "   Starte einen Emulator oder verbinde ein Gerät."
    exit 1
fi

echo "✅ Gerät gefunden!"
$ADB devices
echo ""

# Baue die App
echo "🔨 Baue App..."
cd "$PROJECT_DIR"
./gradlew clean assembleDebug --quiet

if [ $? -ne 0 ]; then
    echo "❌ Build fehlgeschlagen!"
    exit 1
fi

echo "✅ Build erfolgreich!"
echo ""

# Installiere die App
echo "📦 Installiere App..."
$ADB install -r app/build/outputs/apk/debug/app-debug.apk

if [ $? -ne 0 ]; then
    echo "❌ Installation fehlgeschlagen!"
    exit 1
fi

echo "✅ App installiert!"
echo ""

# Aktiviere AccessibilityService
echo "🔧 Aktiviere AccessibilityService..."
$ADB shell settings put secure enabled_accessibility_services com.example.safespark/.GuardianAccessibilityService
$ADB shell settings put secure accessibility_enabled 1

echo "✅ AccessibilityService aktiviert!"
echo ""

# Starte die App
echo "🚀 Starte App..."
$ADB shell am start -n com.example.safespark/.MainActivity

sleep 2

# Prüfe Status
echo ""
echo "📊 Status-Übersicht:"
echo "======================================"

# App-Info
APP_VERSION=$($ADB shell dumpsys package com.example.safespark | grep "versionName" | head -1)
echo "App: $APP_VERSION"

# Prozess-Status
PROCESS=$($ADB shell ps | grep safespark | awk '{print "PID: " $2 ", Name: " $9}')
if [ -n "$PROCESS" ]; then
    echo "✅ Prozess läuft: $PROCESS"
else
    echo "❌ Prozess läuft nicht!"
fi

# Accessibility-Status
ACC_STATUS=$($ADB shell settings get secure accessibility_enabled)
if [ "$ACC_STATUS" = "1" ]; then
    echo "✅ Accessibility aktiviert"
else
    echo "⚠️  Accessibility nicht aktiviert"
fi

# Enabled Services
ENABLED_SERVICES=$($ADB shell settings get secure enabled_accessibility_services)
if [[ "$ENABLED_SERVICES" == *"kidguard"* ]]; then
    echo "✅ GuardianAccessibilityService aktiv"
else
    echo "⚠️  GuardianAccessibilityService nicht aktiv"
fi

echo ""
echo "======================================"
echo "✅ Test abgeschlossen!"
echo ""
echo "💡 Nächste Schritte:"
echo "   1. App ist gestartet und läuft"
echo "   2. Teste Texteingabe in beliebiger App"
echo "   3. Überwache Logs mit:"
echo "      adb logcat | grep -E '(RISK|GuardianAccessibilityService)'"
echo ""
echo "📝 Dokumentation:"
echo "   - TEST_REPORT.md"
echo "   - ACCESSIBILITY_SETUP.md"
echo "   - 16KB_PAGE_SIZE_STATUS.md"
echo ""
