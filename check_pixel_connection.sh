#!/bin/bash

# Pixel 10 Pro Verbindungs-Check
# Datum: 28. Januar 2026

echo "📱 Pixel 10 Pro Verbindungs-Check"
echo "===================================="
echo ""

ADB="$HOME/Library/Android/sdk/platform-tools/adb"

# Prüfe ob ADB existiert
if [ ! -f "$ADB" ]; then
    echo "❌ ADB nicht gefunden!"
    echo ""
    echo "ADB sollte sein: $ADB"
    echo ""
    echo "Installiere Android SDK über Android Studio:"
    echo "  Tools → SDK Manager → Android SDK Platform-Tools"
    exit 1
fi

echo "✅ ADB gefunden: $ADB"
echo ""

# Prüfe ADB Version
echo "📊 ADB Version:"
"$ADB" version | head -3
echo ""

# Prüfe USB-Geräte
echo "🔌 USB-Geräte:"
system_profiler SPUSBDataType 2>/dev/null | grep -B 5 -A 10 -i "pixel\|google\|android" | head -30

if [ $? -ne 0 ]; then
    echo "⚠️ Kein Android/Pixel-Gerät über USB erkannt"
    echo ""
    echo "Mögliche Gründe:"
    echo "  - Kabel nicht richtig eingesteckt"
    echo "  - Pixel ist gesperrt (entsperren!)"
    echo "  - USB-Debugging nicht aktiviert"
    echo ""
fi

echo ""
echo "🔄 Starte ADB Server neu..."
"$ADB" kill-server 2>&1
sleep 1
"$ADB" start-server 2>&1
echo ""

echo "📱 Verbundene Android-Geräte:"
DEVICES=$("$ADB" devices -l)
echo "$DEVICES"
echo ""

# Zähle Geräte
DEVICE_COUNT=$(echo "$DEVICES" | grep -c "device$")
EMULATOR_COUNT=$(echo "$DEVICES" | grep -c "emulator")
PHYSICAL_COUNT=$((DEVICE_COUNT - EMULATOR_COUNT))

echo "📊 Zusammenfassung:"
echo "   Emulatoren: $EMULATOR_COUNT"
echo "   Physische Geräte: $PHYSICAL_COUNT"
echo ""

if [ $PHYSICAL_COUNT -eq 0 ]; then
    echo "❌ Kein physisches Gerät gefunden!"
    echo ""
    echo "🔧 LÖSUNG - Auf dem Pixel 10:"
    echo "================================"
    echo ""
    echo "1. Entwickleroptionen aktivieren:"
    echo "   Einstellungen → Über das Telefon"
    echo "   → Build-Nummer 7x antippen"
    echo ""
    echo "2. USB-Debugging aktivieren:"
    echo "   Einstellungen → System → Entwickleroptionen"
    echo "   → USB-Debugging AN"
    echo ""
    echo "3. USB-Modus ändern:"
    echo "   Notification Shade → USB-Einstellungen"
    echo "   → 'Dateiübertragung' wählen"
    echo ""
    echo "4. Autorisierung erlauben:"
    echo "   Dialog 'USB-Debugging zulassen?' → OK"
    echo "   ☑ 'Immer von diesem Computer zulassen'"
    echo ""
    echo "5. Dann Script erneut ausführen:"
    echo "   ./check_pixel_connection.sh"
    echo ""
else
    echo "✅ Physisches Gerät gefunden!"
    echo ""
    echo "📱 Gerät-Details:"
    "$ADB" devices -l | grep -v "emulator" | grep "device$"
    echo ""
    echo "🎉 ERFOLGREICH!"
    echo ""
    echo "Nächste Schritte:"
    echo "  1. Öffne Android Studio"
    echo "  2. Device Selector → Wähle Pixel 10 Pro"
    echo "  3. Shift+F10 (Run)"
    echo "  4. App wird auf Pixel 10 installiert!"
    echo ""
fi

# Prüfe auf unauthorized
UNAUTHORIZED=$("$ADB" devices | grep -c "unauthorized")
if [ $UNAUTHORIZED -gt 0 ]; then
    echo ""
    echo "⚠️ WARNUNG: Gerät ist 'unauthorized'"
    echo ""
    echo "Lösung:"
    echo "  1. Auf Pixel 10: Entsperre das Gerät"
    echo "  2. Es sollte ein Dialog erscheinen:"
    echo "     'USB-Debugging zulassen?'"
    echo "  3. Klicke OK"
    echo "  4. Falls Dialog nicht kommt:"
    echo "     - Kabel ab- und wieder anstecken"
    echo "     - USB-Debugging AUS und wieder AN"
    echo ""
fi

# Prüfe auf offline
OFFLINE=$("$ADB" devices | grep -c "offline")
if [ $OFFLINE -gt 0 ]; then
    echo ""
    echo "⚠️ WARNUNG: Gerät ist 'offline'"
    echo ""
    echo "Lösung:"
    echo "  1. Kabel abstecken"
    echo "  2. Pixel 10 neu starten"
    echo "  3. Nach Neustart: USB-Debugging aktivieren"
    echo "  4. Kabel wieder anstecken"
    echo "  5. Script erneut ausführen"
    echo ""
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📄 Vollständige Anleitung:"
echo "   PIXEL_10_CONNECTION_GUIDE.md"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
