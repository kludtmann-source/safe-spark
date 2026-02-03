#!/bin/bash
# KidGuard App Installation Script
# Datum: 26. Januar 2026

set -e

echo "🚀 KidGuard App Installation"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Prüfe ob APK existiert
APK_PATH="/Users/knutludtmann/AndroidStudioProjects/KidGuard/app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK_PATH" ]; then
    echo "❌ Fehler: APK nicht gefunden!"
    echo "Pfad: $APK_PATH"
    exit 1
fi

echo "✅ APK gefunden: $(ls -lh $APK_PATH | awk '{print $5}')"
echo ""

# Prüfe ob adb verfügbar ist
ADB_PATH="$HOME/Library/Android/sdk/platform-tools/adb"

if [ ! -f "$ADB_PATH" ]; then
    echo "❌ Fehler: ADB nicht gefunden!"
    echo "Pfad: $ADB_PATH"
    exit 1
fi

echo "✅ ADB gefunden"
echo ""

# Prüfe verbundene Geräte
echo "🔍 Suche nach verbundenen Geräten..."
DEVICES=$($ADB_PATH devices | grep -v "List of devices" | grep "device$" | wc -l)

if [ $DEVICES -eq 0 ]; then
    echo "⚠️  Kein Gerät verbunden!"
    echo ""
    echo "Bitte wähle eine Option:"
    echo "  1) Emulator starten"
    echo "  2) USB-Gerät verbinden"
    echo "  3) Script abbrechen"
    echo ""

    # Zeige verfügbare Emulatoren
    echo "Verfügbare Emulatoren:"
    $HOME/Library/Android/sdk/emulator/emulator -list-avds
    echo ""

    # Starte automatisch den ersten Emulator
    FIRST_AVD=$($HOME/Library/Android/sdk/emulator/emulator -list-avds | head -1)

    if [ -n "$FIRST_AVD" ]; then
        echo "📱 Starte Emulator: $FIRST_AVD"
        $HOME/Library/Android/sdk/emulator/emulator -avd $FIRST_AVD -no-snapshot-load &
        EMULATOR_PID=$!

        echo "⏳ Warte 60 Sekunden auf Emulator-Start..."
        sleep 60

        echo "⏳ Warte auf Device-Ready..."
        $ADB_PATH wait-for-device

        echo "⏳ Warte auf Boot-Complete..."
        while [ "$($ADB_PATH shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
            sleep 2
        done

        echo "✅ Emulator bereit!"
    else
        echo "❌ Keine Emulatoren gefunden!"
        exit 1
    fi
else
    echo "✅ $DEVICES Gerät(e) verbunden"
    $ADB_PATH devices
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📦 Installiere KidGuard App..."
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Installiere APK
$ADB_PATH install -r "$APK_PATH"

if [ $? -eq 0 ]; then
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "✅ Installation erfolgreich!"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "🚀 Starte App..."
    $ADB_PATH shell am start -n com.example.safespark/.MainActivity

    echo ""
    echo "📊 App sollte jetzt auf dem Gerät laufen!"
    echo ""
    echo "Nächste Schritte:"
    echo "  1. Prüfe ob Dashboard angezeigt wird"
    echo "  2. Aktiviere AccessibilityService:"
    echo "     Settings → Accessibility → KidGuard → Enable"
    echo "  3. Teste Risiko-Erkennung in einer Chat-App"
    echo ""
    echo "📝 Logs anzeigen:"
    echo "  $ADB_PATH logcat | grep KidGuard"
    echo ""
else
    echo ""
    echo "❌ Installation fehlgeschlagen!"
    exit 1
fi
