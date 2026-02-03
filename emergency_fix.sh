#!/bin/bash

echo "🔥 EMERGENCY FIX - Score Problem"
echo "================================"
echo ""

cd /Users/knutludtmann/AndroidStudioProjects/KidGuard

echo "1️⃣ Deinstalliere alte App..."
adb -s 56301FDCR006BT uninstall com.example.safespark
echo "✅ App deinstalliert"
echo ""

echo "2️⃣ Lösche Build-Caches..."
rm -rf app/build .gradle build
echo "✅ Caches gelöscht"
echo ""

echo "3️⃣ Starte Clean Build..."
./gradlew clean
echo "✅ Clean fertig"
echo ""

echo "4️⃣ Baue neue APK..."
./gradlew :app:assembleDebug
echo "✅ Build fertig"
echo ""

echo "5️⃣ Installiere neue APK..."
adb -s 56301FDCR006BT install -r app/build/outputs/apk/debug/app-debug.apk
echo "✅ Installation fertig"
echo ""

echo "6️⃣ Aktiviere Accessibility Service..."
adb -s 56301FDCR006BT shell settings put secure enabled_accessibility_services com.example.safespark/.GuardianAccessibilityService
adb -s 56301FDCR006BT shell settings put secure accessibility_enabled 1
echo "✅ Accessibility aktiviert"
echo ""

echo "7️⃣ Starte App..."
adb -s 56301FDCR006BT shell am start -n com.example.safespark/.MainActivity
echo "✅ App gestartet"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ FERTIG!"
echo ""
echo "Öffne jetzt KidGuard und prüfe ob du siehst:"
echo "🔥 VERSION: 2.0-ASSESSMENT-FIX-ACTIVE 🔥"
echo ""
echo "Wenn JA → Teste 'bist du heute alleine?' in WhatsApp"
echo "Wenn NEIN → Lauf dieses Script nochmal!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
