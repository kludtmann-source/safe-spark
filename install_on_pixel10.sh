#!/bin/bash

# KidGuard auf Pixel 10 installieren & testen
# Datum: 28. Januar 2026

echo "🚀 KidGuard Installation & Test auf Pixel 10"
echo "=============================================="
echo ""

ADB="$HOME/Library/Android/sdk/platform-tools/adb"
DEVICE="56301FDCR006BT"
PROJECT_DIR="/Users/knutludtmann/AndroidStudioProjects/KidGuard"

cd "$PROJECT_DIR"

# Prüfe ob Pixel 10 verbunden ist
echo "📱 Prüfe Pixel 10 Verbindung..."
CONNECTED=$("$ADB" devices | grep "$DEVICE" | grep "device")

if [ -z "$CONNECTED" ]; then
    echo "❌ Pixel 10 nicht verbunden!"
    echo "   Führe aus: ./check_pixel_connection.sh"
    exit 1
fi

echo "✅ Pixel 10 verbunden: $DEVICE"
echo ""

# Prüfe ob APK existiert
APK="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then
    echo "⚠️ APK nicht gefunden!"
    echo "   Erstelle APK..."
    echo ""

    # In Android Studio bauen wäre besser, aber versuchen wir es
    echo "💡 EMPFEHLUNG:"
    echo "   1. Öffne Android Studio"
    echo "   2. Build → Make Project (Cmd+F9)"
    echo "   3. Dann dieses Script erneut ausführen"
    echo ""
    exit 1
fi

echo "✅ APK gefunden: $APK"
echo ""

# Deinstalliere alte Version (falls vorhanden)
echo "🗑️ Deinstalliere alte Version (falls vorhanden)..."
"$ADB" -s "$DEVICE" uninstall com.example.safespark 2>/dev/null
echo ""

# Installiere APK
echo "📲 Installiere KidGuard auf Pixel 10..."
"$ADB" -s "$DEVICE" install "$APK"

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ Installation erfolgreich!"
    echo ""
else
    echo ""
    echo "❌ Installation fehlgeschlagen!"
    exit 1
fi

# Starte App
echo "🚀 Starte KidGuard..."
"$ADB" -s "$DEVICE" shell am start -n com.example.safespark/.MainActivity

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🎉 APP IST JETZT AUF DEINEM PIXEL 10!"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📋 NÄCHSTE SCHRITTE (auf dem Pixel 10):"
echo ""
echo "1️⃣ AccessibilityService aktivieren:"
echo "   Einstellungen → Eingabehilfe → KidGuard → AN"
echo ""
echo "2️⃣ Teste mit Grooming-Nachricht:"
echo "   - Öffne WhatsApp/Messages"
echo "   - Schreibe: 'Bist du allein?'"
echo "   - Sende ab"
echo ""
echo "3️⃣ Prüfe Notification:"
echo "   - Ziehe Notification Shade nach unten"
echo "   - Solltest KidGuard Warnung sehen! 🚨"
echo ""
echo "4️⃣ Prüfe Logs (am Mac):"
echo "   adb -s $DEVICE logcat | grep KidGuard"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
