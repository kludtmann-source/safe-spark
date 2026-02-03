#!/bin/bash
set -e  # Stoppe bei Fehler

echo "🔥 EMERGENCY FIX - Vereinfachte Version"
echo "========================================"
echo ""

# Wechsle ins Projekt-Verzeichnis
cd /Users/knutludtmann/AndroidStudioProjects/KidGuard || exit 1

# 1. Deinstalliere alte App
echo "1️⃣ Deinstalliere alte App..."
adb -s 56301FDCR006BT uninstall com.example.safespark 2>/dev/null || echo "   (App war nicht installiert)"
echo "   ✅ Fertig"
echo ""

# 2. Lösche Build-Caches
echo "2️⃣ Lösche Build-Caches..."
rm -rf app/build .gradle build 2>/dev/null || true
echo "   ✅ Fertig"
echo ""

# 3. Build komplett neu
echo "3️⃣ Starte kompletten Neu-Build (das dauert 1-2 Minuten)..."
./gradlew clean :app:assembleDebug --no-daemon --console=plain
BUILD_EXIT_CODE=$?

if [ $BUILD_EXIT_CODE -ne 0 ]; then
    echo ""
    echo "❌ BUILD FEHLGESCHLAGEN!"
    echo "Bitte führe manuell in Android Studio aus:"
    echo "  1. Build → Clean Project"
    echo "  2. Build → Rebuild Project"
    echo "  3. Run → Run 'app'"
    exit 1
fi

echo "   ✅ Build erfolgreich!"
echo ""

# 4. Prüfe ob APK existiert
if [ ! -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "❌ APK nicht gefunden!"
    echo "Bitte baue manuell in Android Studio"
    exit 1
fi

echo "4️⃣ Installiere neue APK..."
adb -s 56301FDCR006BT install -r app/build/outputs/apk/debug/app-debug.apk
echo "   ✅ Installation erfolgreich!"
echo ""

# 5. Aktiviere Accessibility
echo "5️⃣ Aktiviere Accessibility Service..."
adb -s 56301FDCR006BT shell settings put secure enabled_accessibility_services com.example.safespark/.GuardianAccessibilityService
adb -s 56301FDCR006BT shell settings put secure accessibility_enabled 1
echo "   ✅ Accessibility aktiviert"
echo ""

# 6. Starte App
echo "6️⃣ Starte App..."
adb -s 56301FDCR006BT shell am start -n com.example.safespark/.MainActivity
echo "   ✅ App gestartet"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ FERTIG!"
echo ""
echo "📱 PRÜFE JETZT AUF DEM GERÄT:"
echo ""
echo "Öffne KidGuard → Scrolle zur Log-Card"
echo ""
echo "Siehst du diese Zeile?"
echo "  🔥 VERSION: 2.0-ASSESSMENT-FIX-ACTIVE 🔥"
echo ""
echo "JA  → Perfekt! Teste: 'bist du heute alleine?' in WhatsApp"
echo "NEIN → Alte APK läuft noch. Baue in Android Studio neu:"
echo "        Build → Clean → Rebuild → Run"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
