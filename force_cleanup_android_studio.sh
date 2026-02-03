#!/bin/zsh
# ========================================
# FORCE CLEANUP - Android Studio komplett bereinigen
# ========================================

echo "🧹 FORCE CLEANUP - Android Studio & Gradle"
echo "==========================================="
echo ""

# Schritt 1: Alle Android Studio Prozesse beenden
echo "1️⃣  Beende alle Android Studio Prozesse..."
if pgrep -f "Android Studio" > /dev/null; then
    echo "   Gefundene Prozesse:"
    pgrep -f "Android Studio" | while read pid; do
        echo "   - PID: $pid"
    done

    # Kotlin Daemon
    pkill -f "kotlin.daemon.KotlinCompileDaemon" 2>/dev/null

    # Gradle Daemons
    pkill -f "gradle.launcher.daemon.bootstrap.GradleDaemon" 2>/dev/null

    # fsnotifier
    pkill -f "fsnotifier" 2>/dev/null

    sleep 2

    # Force kill falls nötig
    if pgrep -f "Android Studio" > /dev/null; then
        echo "   Erzwinge Beendigung..."
        pkill -9 -f "Android Studio" 2>/dev/null
        sleep 1
    fi

    if pgrep -f "Android Studio" > /dev/null; then
        echo "   ❌ Konnte nicht alle Prozesse beenden!"
        echo "   Bitte schließe Android Studio manuell (⌘+Q)"
        exit 1
    else
        echo "   ✅ Alle Prozesse beendet"
    fi
else
    echo "   ✅ Keine laufenden Prozesse gefunden"
fi
echo ""

# Schritt 2: Gradle stoppen
echo "2️⃣  Stoppe Gradle..."
cd /Users/knutludtmann/AndroidStudioProjects/KidGuard
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
if [ -x ./gradlew ]; then
    ./gradlew --stop 2>/dev/null
    echo "   ✅ Gradle gestoppt"
else
    echo "   ⚠️  gradlew nicht gefunden"
fi
echo ""

# Schritt 3: Projekt-Caches löschen
echo "3️⃣  Bereinige Projekt-Caches..."
rm -rf .gradle 2>/dev/null && echo "   ✓ .gradle gelöscht"
rm -rf .idea 2>/dev/null && echo "   ✓ .idea gelöscht"
rm -rf build 2>/dev/null && echo "   ✓ build gelöscht"
rm -rf app/build 2>/dev/null && echo "   ✓ app/build gelöscht"
echo "   ✅ Projekt-Caches gelöscht"
echo ""

# Schritt 4: Gradle System-Caches löschen
echo "4️⃣  Bereinige Gradle System-Caches..."
rm -rf ~/.gradle/caches/transforms-* 2>/dev/null && echo "   ✓ transforms-cache gelöscht"
rm -rf ~/.gradle/caches/build-cache-* 2>/dev/null && echo "   ✓ build-cache gelöscht"
rm -rf ~/.gradle/daemon 2>/dev/null && echo "   ✓ daemon gelöscht"
rm -rf ~/.gradle/caches/[0-9]*/kotlin-dsl 2>/dev/null && echo "   ✓ kotlin-dsl cache gelöscht"
echo "   ✅ Gradle System-Caches gelöscht"
echo ""

# Schritt 5: Android Studio Caches löschen
echo "5️⃣  Bereinige Android Studio Caches..."
rm -rf ~/Library/Caches/Google/AndroidStudio* 2>/dev/null && echo "   ✓ Cache-Verzeichnisse gelöscht"
find ~/Library/Application\ Support/Google/AndroidStudio* -type d -name "caches" -maxdepth 2 -exec rm -rf {}/* \; 2>/dev/null && echo "   ✓ Application Support Caches gelöscht"
find ~/Library/Application\ Support/Google/AndroidStudio* -type d -name "index" -maxdepth 2 -exec rm -rf {}/* \; 2>/dev/null && echo "   ✓ Indizes gelöscht"
echo "   ✅ Android Studio Caches gelöscht"
echo ""

# Schritt 6: Kotlin Compiler Caches löschen
echo "6️⃣  Bereinige Kotlin Compiler Caches..."
rm -rf ~/Library/Application\ Support/kotlin/daemon 2>/dev/null && echo "   ✓ Kotlin Daemon Cache gelöscht"
rm -rf ~/.kotlin 2>/dev/null && echo "   ✓ .kotlin gelöscht"
echo "   ✅ Kotlin Caches gelöscht"
echo ""

# Abschluss
echo "✅✅✅ BEREINIGUNG VOLLSTÄNDIG ABGESCHLOSSEN! ✅✅✅"
echo ""
echo "════════════════════════════════════════════"
echo "📋 NÄCHSTE SCHRITTE:"
echo "════════════════════════════════════════════"
echo ""
echo "1. 🚀 Starte Android Studio NEU"
echo ""
echo "2. 📂 Öffne das KidGuard Projekt"
echo "   Wähle: /Users/knutludtmann/AndroidStudioProjects/KidGuard"
echo ""
echo "3. ⏳ WARTE GEDULDIG auf das Indexing"
echo "   • Beobachte die Status-Leiste unten rechts"
echo "   • 'Setting up run configuration' - WARTEN"
echo "   • 'Configure Kotlin language settings' - WARTEN"
echo "   • 'Updating indexes' - WARTEN (5-10 Minuten)"
echo ""
echo "4. 💡 WICHTIG während des Indexing:"
echo "   • Keine anderen Programme öffnen"
echo "   • MacBook am Stromnetz anschließen"
echo "   • NICHT im Projekt herum klicken"
echo "   • Bildschirm kann dimmen, aber Mac wach lassen"
echo ""
echo "5. ✅ Fertig wenn:"
echo "   • Keine Hintergrundaufgaben mehr laufen"
echo "   • Grüner Haken in der Status-Leiste"
echo ""
echo "════════════════════════════════════════════"
echo ""
