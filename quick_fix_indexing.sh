#!/bin/zsh
# SCHNELLE LÖSUNG für "Updating indexes" Problem

echo "⚡ Schnellste Lösung für Indexing-Problem"
echo "========================================"
echo ""

# Prüfe ob Android Studio läuft
if pgrep -f "Android Studio" > /dev/null; then
    echo "⚠️  Android Studio Prozesse laufen noch im Hintergrund!"
    echo "    Beende automatisch alle Prozesse..."
    echo ""

    # Kotlin Daemon stoppen
    pkill -f "kotlin.daemon.KotlinCompileDaemon" 2>/dev/null

    # Gradle Daemons beenden
    pkill -f "gradle.launcher.daemon.bootstrap.GradleDaemon" 2>/dev/null

    # fsnotifier beenden
    pkill -f "fsnotifier" 2>/dev/null

    # Warte kurz
    sleep 2

    # Falls noch Prozesse laufen, force kill
    if pgrep -f "Android Studio" > /dev/null; then
        echo "    Erzwinge Beendigung verbliebener Prozesse..."
        pkill -9 -f "Android Studio" 2>/dev/null
        sleep 1
    fi

    # Finale Prüfung
    if pgrep -f "Android Studio" > /dev/null; then
        echo "❌ FEHLER: Konnte nicht alle Prozesse beenden!"
        echo "   Bitte schließe Android Studio manuell mit ⌘ Cmd+Q"
        exit 1
    fi

    echo "    ✓ Alle Prozesse erfolgreich beendet"
    echo ""
fi

echo "1️⃣  Stoppe Gradle..."
cd /Users/knutludtmann/AndroidStudioProjects/KidGuard
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew --stop 2>/dev/null

echo "2️⃣  Bereinige Projekt-Caches..."
rm -rf .gradle .idea build app/build */build 2>/dev/null

echo "3️⃣  Bereinige Gradle-Caches..."
rm -rf ~/.gradle/caches/transforms-* 2>/dev/null
rm -rf ~/.gradle/caches/build-cache-* 2>/dev/null
rm -rf ~/.gradle/daemon 2>/dev/null

echo "4️⃣  Bereinige Android Studio Caches..."
rm -rf ~/Library/Caches/Google/AndroidStudio* 2>/dev/null
find ~/Library/Application\ Support/Google/AndroidStudio* -type d -name "caches" -exec rm -rf {}/* \; 2>/dev/null
find ~/Library/Application\ Support/Google/AndroidStudio* -type d -name "index" -exec rm -rf {}/* \; 2>/dev/null

echo ""
echo "✅ Fertig!"
echo ""
echo "JETZT:"
echo "1. Starte Android Studio"
echo "2. Öffne das Projekt"
echo "3. Warte 5-10 Minuten auf das Indexing"
echo ""
echo "💡 Tipp: Lass andere Programme geschlossen und das MacBook am Strom"
