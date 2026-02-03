#!/bin/bash
# Quick Test Script für KidGuard ML-Integration
# Verwendung: ./quick_test_ml.sh

echo "🧪 KidGuard ML-Integration Test"
echo "================================"
echo ""

# Device ID
DEVICE="56301FDCR006BT"
ADB="$HOME/Library/Android/sdk/platform-tools/adb"

# 1. Prüfe Device-Verbindung
echo "1️⃣  Prüfe Device-Verbindung..."
$ADB -s $DEVICE shell echo "✅ Device verbunden" || {
    echo "❌ Device nicht verbunden!"
    exit 1
}
echo ""

# 2. Prüfe App-Installation
echo "2️⃣  Prüfe App-Installation..."
if $ADB -s $DEVICE shell pm list packages | grep -q "com.example.kidguard"; then
    echo "✅ KidGuard installiert"
else
    echo "❌ KidGuard NICHT installiert!"
    echo "   Installiere mit: adb install -r app-debug.apk"
    exit 1
fi
echo ""

# 3. Prüfe Accessibility Service
echo "3️⃣  Prüfe Accessibility Service..."
SERVICE_STATUS=$($ADB -s $DEVICE shell settings get secure enabled_accessibility_services)
if echo "$SERVICE_STATUS" | grep -q "kidguard"; then
    echo "✅ Accessibility Service AKTIV"
else
    echo "⚠️  Accessibility Service NICHT aktiv"
    echo "   Aktiviere manuell: Einstellungen → Bedienungshilfen → KidGuard"
    echo ""
    echo "   Oder automatisch:"
    echo "   $ADB -s $DEVICE shell settings put secure enabled_accessibility_services com.example.kidguard/.GuardianAccessibilityService"
    echo "   $ADB -s $DEVICE shell settings put secure accessibility_enabled 1"
fi
echo ""

# 4. Starte App
echo "4️⃣  Starte KidGuard App..."
$ADB -s $DEVICE shell am start -n com.example.safespark/.MainActivity
sleep 2
echo "✅ App gestartet"
echo ""

# 5. Starte Logcat-Monitoring
echo "5️⃣  Starte Logcat-Monitoring..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📊 Monitoring gestartet (Strg+C zum Beenden)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🧪 Jetzt kannst du in WhatsApp testen:"
echo "   1. Harmlos: 'hast du die hausaufgaben gemacht?'"
echo "   2. RISK: 'bist du grad allein?'"
echo "   3. RISK: 'you seem mature for your age'"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Logcat mit Farben
$ADB -s $DEVICE logcat -c  # Clear old logs
$ADB -s $DEVICE logcat | grep --color=always -E "(GuardianAccessibility|MLGroomingDetector|KidGuardEngine|RISK DETECTED)"
