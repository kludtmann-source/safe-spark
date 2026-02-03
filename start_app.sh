#!/bin/bash

# KidGuard App Starter Script
# Datum: 28. Januar 2026

echo "🚀 KidGuard App Starter"
echo "======================"
echo ""

# Setze Android SDK Pfad
ANDROID_SDK="$HOME/Library/Android/sdk"
ADB="$ANDROID_SDK/platform-tools/adb"
EMULATOR="$ANDROID_SDK/emulator/emulator"

# Prüfe ob Android Studio läuft
if pgrep -x "Android Studio" > /dev/null; then
    echo "✅ Android Studio läuft"
else
    echo "❌ Android Studio ist nicht gestartet"
    echo ""
    echo "📱 LÖSUNG: Starte in Android Studio"
    echo "=================================="
    echo ""
    echo "1. Öffne Android Studio"
    echo "2. Öffne Projekt: KidGuard"
    echo "3. Drücke Shift+F10 (ODER klicke grünes ▶️ Play-Icon)"
    echo "4. Wähle Emulator: Pixel 8 API 35"
    echo "5. App startet automatisch!"
    echo ""
    exit 1
fi

# Prüfe ob ADB verfügbar ist
if [ -f "$ADB" ]; then
    echo "✅ ADB gefunden: $ADB"

    # Prüfe verbundene Geräte
    DEVICES=$("$ADB" devices | grep -v "List" | grep "device$" | wc -l)

    if [ $DEVICES -eq 0 ]; then
        echo "❌ Kein Emulator/Gerät verbunden"
        echo ""
        echo "📱 EMULATOR STARTEN:"
        echo "==================="
        echo ""
        echo "Option 1: In Android Studio"
        echo "  Device Manager → Pixel 8 API 35 → Play"
        echo ""
        echo "Option 2: Terminal (falls emulator verfügbar)"
        if [ -f "$EMULATOR" ]; then
            echo "  $EMULATOR -avd Pixel_8_API_35 &"
        else
            echo "  Emulator nicht gefunden in SDK"
        fi
        echo ""
        exit 1
    else
        echo "✅ $DEVICES Gerät(e) verbunden"
        "$ADB" devices
        echo ""

        # Prüfe ob APK existiert
        APK="app/build/outputs/apk/debug/app-debug.apk"
        if [ -f "$APK" ]; then
            echo "✅ APK gefunden: $APK"
            echo ""
            echo "📲 Installiere App..."

            # Installiere
            "$ADB" install -r "$APK"

            if [ $? -eq 0 ]; then
                echo ""
                echo "✅ App erfolgreich installiert!"
                echo ""
                echo "🚀 Starte App..."

                # Starte App
                "$ADB" shell am start -n com.example.safespark/.MainActivity

                echo ""
                echo "✅ App gestartet!"
                echo ""
                echo "📋 NÄCHSTE SCHRITTE:"
                echo "==================="
                echo ""
                echo "1. Aktiviere AccessibilityService:"
                echo "   Settings → Accessibility → KidGuard → Toggle ON"
                echo ""
                echo "2. Teste mit Grooming-Message:"
                echo "   Öffne WhatsApp → Schreibe: 'Bist du allein?'"
                echo ""
                echo "3. Prüfe Logs (neues Terminal):"
                echo "   $ADB logcat | grep -E 'KidGuard|RiskEvent'"
                echo ""
                echo "4. Database Inspector (Android Studio):"
                echo "   View → Tool Windows → App Inspection"
                echo "   → Database Inspector → kidguard_database → risk_events"
                echo ""
            else
                echo "❌ Installation fehlgeschlagen"
                echo ""
                echo "Nutze Android Studio: Run → Run 'app' (Shift+F10)"
            fi
        else
            echo "❌ APK nicht gefunden: $APK"
            echo ""
            echo "🔨 BUILD ERFORDERLICH:"
            echo "===================="
            echo ""
            echo "In Android Studio:"
            echo "  1. Build → Make Project (Cmd+F9)"
            echo "  2. Run → Run 'app' (Shift+F10)"
            echo ""
        fi
    fi
else
    echo "❌ ADB nicht gefunden in: $ANDROID_SDK"
    echo ""
    echo "📱 NUTZE ANDROID STUDIO:"
    echo "======================="
    echo ""
    echo "1. Öffne Android Studio"
    echo "2. Run → Run 'app' (Shift+F10)"
    echo "   ODER: Klicke grünes ▶️ Play-Icon"
    echo ""
    echo "Android Studio kümmert sich um:"
    echo "  ✅ Emulator starten (falls nötig)"
    echo "  ✅ App builden"
    echo "  ✅ App installieren"
    echo "  ✅ App starten"
    echo ""
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "💡 EMPFEHLUNG: Nutze Android Studio!"
echo "   Shift+F10 → Alles automatisch! 🚀"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
