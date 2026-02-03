#!/bin/bash

# Phase 1 Testing Script für Pixel 10
# Demo-Model Validation

echo "🚀 PHASE 1 - DEMO-MODEL TESTING"
echo "================================"
echo ""

DEVICE="56301FDCR006BT"

# Check Device Connection
echo "📱 Prüfe Pixel 10 Verbindung..."
adb devices | grep "$DEVICE" > /dev/null

if [ $? -ne 0 ]; then
    echo "❌ Pixel 10 nicht verbunden!"
    echo "   Führe aus: ./check_pixel_connection.sh"
    exit 1
fi

echo "✅ Pixel 10 verbunden"
echo ""

# Check if app is installed
echo "📦 Prüfe App-Installation..."
adb -s "$DEVICE" shell pm list packages | grep "com.example.kidguard" > /dev/null

if [ $? -ne 0 ]; then
    echo "⚠️ App nicht installiert"
    echo ""
    echo "INSTALLATION IN ANDROID STUDIO:"
    echo "1. Öffne Android Studio"
    echo "2. Build → Rebuild Project"
    echo "3. Device Selector → Pixel 10 (${DEVICE})"
    echo "4. Run → Run 'app' (Shift+F10)"
    echo ""
    echo "Dann führe dieses Script erneut aus!"
    exit 1
fi

echo "✅ App installiert"
echo ""

# Start Logcat monitoring
echo "📊 Starte Log-Monitoring..."
echo "   Filter: MLGroomingDetector + GuardianAccessibility"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🧪 TESTE JETZT AUF PIXEL 10:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "TEST 1: Harmlose Nachricht"
echo '  → Schreibe: "Wie geht es dir?"'
echo "  → Erwartung: Score < 0.4, SAFE"
echo ""
echo "TEST 2: Grooming-Nachricht"
echo '  → Schreibe: "Bist du allein?"'
echo "  → Erwartung: Score > 0.8, STAGE_ASSESSMENT"
echo ""
echo "TEST 3: Spätnachts (wenn 23-06 Uhr)"
echo '  → Schreibe: "Bist du allein?"'
echo "  → Erwartung: Score > 0.9 (Temporal Bonus!)"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📺 LIVE LOGS (Strg+C zum Beenden):"
echo ""

# Monitor logs
adb -s "$DEVICE" logcat -c # Clear old logs
adb -s "$DEVICE" logcat | grep -E "MLGroomingDetector|GuardianAccessibility" --line-buffered | while read line; do
    # Highlight important lines
    if echo "$line" | grep -q "DEMO MODE"; then
        echo "🔧 $line"
    elif echo "$line" | grep -q "GEFÄHRLICH"; then
        echo "🚨 $line"
    elif echo "$line" | grep -q "Rule-Based"; then
        echo "📊 $line"
    elif echo "$line" | grep -q "RISK DETECTED"; then
        echo "⚠️  $line"
    elif echo "$line" | grep -q "RiskEvent gespeichert"; then
        echo "💾 $line"
    elif echo "$line" | grep -q "Notification gesendet"; then
        echo "🔔 $line"
    else
        echo "$line"
    fi
done
