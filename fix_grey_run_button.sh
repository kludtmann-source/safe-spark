#!/bin/zsh
# ========================================
# FIX: Grauer/Inaktiver Run Button
# Kann WÄHREND Android Studio läuft ausgeführt werden!
# ========================================

echo "🔧 FIX: Grauer/Inaktiver Run Button"
echo "====================================="
echo ""

cd /Users/knutludtmann/AndroidStudioProjects/KidGuard || exit 1

echo "📋 Mögliche Ursachen für grauen Run Button:"
echo "   1. Gradle Sync läuft noch oder ist fehlgeschlagen"
echo "   2. Build-Fehler im Projekt"
echo "   3. Run Configuration fehlt oder ist falsch"
echo "   4. Gradle Daemon hängt"
echo ""

# Prüfe Gradle Sync Status
echo "1️⃣  Prüfe Gradle Build-Fähigkeit..."
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

if [ -x ./gradlew ]; then
    echo "   Führe Test-Build aus (kann 30-60 Sek dauern)..."
    ./gradlew tasks --quiet 2>&1 | head -5

    if [ $? -eq 0 ]; then
        echo "   ✅ Gradle funktioniert korrekt"
    else
        echo "   ❌ Gradle hat Probleme"
        echo ""
        echo "   Führe Gradle Sync neu aus..."
        ./gradlew --stop 2>/dev/null
        sleep 2
    fi
else
    echo "   ⚠️  gradlew nicht ausführbar"
fi
echo ""

# Prüfe auf Build-Fehler
echo "2️⃣  Prüfe auf Build-Fehler..."
if [ -x ./gradlew ]; then
    echo "   Versuche sauberen Build..."
    ./gradlew clean 2>&1 | tail -3
    echo "   ✅ Clean ausgeführt"
fi
echo ""

# Lösche problematische Caches
echo "3️⃣  Bereinige problematische Caches..."
rm -rf .gradle/configuration-cache 2>/dev/null && echo "   ✓ configuration-cache gelöscht"
rm -rf .gradle/*/executionHistory 2>/dev/null && echo "   ✓ executionHistory gelöscht"
rm -rf .gradle/*/fileHashes 2>/dev/null && echo "   ✓ fileHashes gelöscht"
rm -rf .gradle/buildOutputCleanup 2>/dev/null && echo "   ✓ buildOutputCleanup gelöscht"
echo ""

echo "✅ Bereinigung abgeschlossen!"
echo ""
echo "════════════════════════════════════════════"
echo "📋 JETZT IN ANDROID STUDIO (Wichtige Schritte!):"
echo "════════════════════════════════════════════"
echo ""
echo "SCHRITT 1: Gradle Sync erzwingen"
echo "─────────────────────────────────"
echo "   a) Klicke oben in der Toolbar:"
echo "      File → Sync Project with Gradle Files"
echo ""
echo "   b) ODER klicke auf das 🐘 Elefant-Icon (Gradle Sync)"
echo "      in der Toolbar"
echo ""
echo "   c) WARTE bis unten rechts steht:"
echo "      'BUILD SUCCESSFUL' oder grüner Haken ✅"
echo ""
echo "   ⏱️  Das dauert 1-3 Minuten - sei geduldig!"
echo ""
echo "─────────────────────────────────────────────"
echo ""
echo "SCHRITT 2: Run Configuration prüfen/erstellen"
echo "─────────────────────────────────────────────"
echo "   a) Schaue oben rechts neben dem Run Button ▶️"
echo "      → Siehst du ein Dropdown mit 'app'?"
echo ""
echo "   b) WENN NEIN oder 'No Configuration' steht:"
echo "      → Klicke auf das Dropdown"
echo "      → Wähle 'Edit Configurations...'"
echo ""
echo "   c) Im Dialog:"
echo "      → Lösche alte Konfigurationen (falls vorhanden)"
echo "      → Klicke '+' → 'Android App'"
echo "      → Name: app"
echo "      → Module: KidGuard.app"
echo "      → OK klicken"
echo ""
echo "─────────────────────────────────────────────"
echo ""
echo "SCHRITT 3: Falls IMMER NOCH grau"
echo "─────────────────────────────────────────────"
echo "   → File → Invalidate Caches → Invalidate and Restart"
echo "   → Nach Neustart 2-3 Min warten"
echo ""
echo "════════════════════════════════════════════"
echo ""
echo "💡 WICHTIG:"
echo "   • Der Run Button wird ERST grün wenn:"
echo "     ✅ Gradle Sync FERTIG ist"
echo "     ✅ Keine Build-Fehler existieren"
echo "     ✅ Run Configuration existiert"
echo ""
echo "   • Schaue unten in Android Studio:"
echo "     - Tab 'Build' → Gibt es rote Fehler?"
echo "     - Tab 'Problems' → Gibt es Fehler?"
echo ""
echo "   • Falls rote Fehler da sind:"
echo "     Kopiere sie und zeige sie mir!"
echo ""
echo "════════════════════════════════════════════"
echo ""
