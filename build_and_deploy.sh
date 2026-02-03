#!/bin/bash

# KidGuard Build & Deploy Script
# Datum: 28. Januar 2026

echo "🚀 KidGuard Build & Deploy"
echo "=========================="
echo ""

# Prüfe ob Android Studio läuft
if pgrep -x "Android Studio" > /dev/null; then
    echo "✅ Android Studio läuft"
else
    echo "⚠️ Android Studio ist nicht gestartet"
    echo "   Bitte starte Android Studio und führe dort den Build durch:"
    echo "   Build → Make Project (Cmd+F9)"
    exit 1
fi

# Prüfe ob Emulator läuft
if adb devices | grep -q "emulator"; then
    echo "✅ Emulator läuft"
    DEVICE=$(adb devices | grep "emulator" | awk '{print $1}')
    echo "   Device: $DEVICE"
else
    echo "⚠️ Kein Emulator gefunden"
    echo "   Starte Emulator in Android Studio oder via:"
    echo "   emulator -avd Pixel_8_API_35"
fi

echo ""
echo "📋 Nächste Schritte in Android Studio:"
echo "======================================"
echo ""
echo "1. File → Sync Project with Gradle Files"
echo "   (Warte auf 'Gradle sync finished')"
echo ""
echo "2. Build → Make Project (Cmd+F9)"
echo "   (Warte auf 'BUILD SUCCESSFUL')"
echo ""
echo "3. Run → Run 'app' (Shift+F10)"
echo "   (App wird auf Emulator installiert)"
echo ""
echo "4. Aktiviere AccessibilityService:"
echo "   Settings → Accessibility → KidGuard → Toggle ON"
echo ""
echo "5. Teste mit Grooming-Message:"
echo "   Öffne WhatsApp → Schreibe: 'Bist du allein?'"
echo ""
echo "6. Prüfe Logs (in neuem Terminal):"
echo "   adb logcat | grep -E 'KidGuard|RiskEvent'"
echo ""
echo "7. Prüfe Database (in Android Studio):"
echo "   View → Tool Windows → App Inspection"
echo "   → Database Inspector → kidguard_database → risk_events"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "💡 WICHTIG: Build muss in Android Studio erfolgen!"
echo "   Der Terminalweg funktioniert nicht (JDK-Problem)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
