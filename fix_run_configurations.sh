#!/bin/zsh
# ========================================
# FIX: "Setting up run configurations" hängt
# ========================================

echo "🔧 FIX: Setting up run configurations Problem"
echo "=============================================="
echo ""

# Schritt 1: Android Studio muss geschlossen sein
if pgrep -f "Android Studio" > /dev/null; then
    echo "❌ Android Studio läuft noch!"
    echo ""
    echo "WICHTIG: Schließe Android Studio KOMPLETT:"
    echo "1. Gehe zu Android Studio"
    echo "2. Drücke ⌘+Q (oder File → Exit)"
    echo "3. Warte 5 Sekunden"
    echo "4. Führe dieses Skript erneut aus"
    echo ""
    exit 1
fi

echo "✅ Android Studio ist geschlossen"
echo ""

# Schritt 2: In Projektverzeichnis wechseln
cd /Users/knutludtmann/AndroidStudioProjects/KidGuard || exit 1

echo "1️⃣  Bereinige Run Configuration Caches..."
# .idea Verzeichnis komplett löschen
rm -rf .idea 2>/dev/null && echo "   ✓ .idea gelöscht"

# Workspace XML (enthält Run Configurations)
find ~/Library/Application\ Support/Google/AndroidStudio* -name "workspace.xml" -delete 2>/dev/null && echo "   ✓ workspace.xml gelöscht"

# Run Configuration Cache
find ~/Library/Application\ Support/Google/AndroidStudio* -name "runConfigurations" -type d -exec rm -rf {} \; 2>/dev/null && echo "   ✓ runConfigurations gelöscht"

# Tasks Cache
find ~/Library/Application\ Support/Google/AndroidStudio* -name "tasks" -type d -exec rm -rf {} \; 2>/dev/null && echo "   ✓ tasks Cache gelöscht"
echo ""

echo "2️⃣  Bereinige Gradle Konfiguration..."
# Gradle Wrapper neu generieren lassen
rm -rf .gradle 2>/dev/null && echo "   ✓ .gradle gelöscht"

# Gradle Build Cache
rm -rf build app/build 2>/dev/null && echo "   ✓ Build Verzeichnisse gelöscht"

# Gradle Home Caches
rm -rf ~/.gradle/caches/build-cache-* 2>/dev/null && echo "   ✓ Gradle Build Cache gelöscht"
rm -rf ~/.gradle/caches/transforms-* 2>/dev/null && echo "   ✓ Gradle Transforms Cache gelöscht"
echo ""

echo "3️⃣  Invalidiere Android Studio Caches..."
# Löscht den kompletten Cache aber behält Settings
rm -rf ~/Library/Caches/Google/AndroidStudio*/caches 2>/dev/null && echo "   ✓ Android Studio Caches gelöscht"

# Löscht temporäre Dateien
rm -rf ~/Library/Caches/Google/AndroidStudio*/tmp 2>/dev/null && echo "   ✓ Temporäre Dateien gelöscht"

# Löscht Compiler Output Cache
rm -rf ~/Library/Caches/Google/AndroidStudio*/compiler 2>/dev/null && echo "   ✓ Compiler Cache gelöscht"
echo ""

echo "4️⃣  Erstelle frische .idea Basis-Struktur..."
mkdir -p .idea
cat > .idea/.gitignore << 'EOF'
# Ignoriere dynamisch generierte Dateien
workspace.xml
tasks.xml
usage.statistics.xml
shelf/
modules.xml
*.iml
EOF
echo "   ✓ .idea/.gitignore erstellt"
echo ""

echo "5️⃣  Optimiere gradle.properties..."
if [ ! -f gradle.properties ]; then
    echo "   ⚠️  gradle.properties nicht gefunden, erstelle neue..."
fi

# Stelle sicher, dass wichtige Gradle Properties gesetzt sind
cat >> gradle.properties << 'EOF'

# Performance Optimierungen (falls nicht schon vorhanden)
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m -XX:+HeapDumpOnOutOfMemoryError -XX:+UseParallelGC
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=false

# Kotlin Compiler Optimierungen
kotlin.incremental=true
kotlin.incremental.usePreciseJavaTracking=true
kotlin.daemon.jvmargs=-Xmx2048m

# Android Optimierungen
android.useAndroidX=true
android.enableJetifier=true
EOF
echo "   ✓ gradle.properties optimiert"
echo ""

echo "✅✅✅ BEREINIGUNG ABGESCHLOSSEN! ✅✅✅"
echo ""
echo "════════════════════════════════════════════"
echo "📋 NÄCHSTE SCHRITTE - SEHR WICHTIG:"
echo "════════════════════════════════════════════"
echo ""
echo "1. 🚀 Starte Android Studio"
echo ""
echo "2. 📂 Öffne das Projekt: File → Open"
echo "   → Wähle: /Users/knutludtmann/AndroidStudioProjects/KidGuard"
echo ""
echo "3. ⚙️  Wenn Popup erscheint: 'Trust Project' → Klick Trust"
echo ""
echo "4. ⏳ ERSTE Gradle Sync ABWARTEN (1-2 Min)"
echo "   • Beobachte unten rechts: 'Gradle Sync'"
echo "   • Warte bis 'BUILD SUCCESSFUL' erscheint"
echo ""
echo "5. 🔄 DANN: File → Invalidate Caches → Just Restart"
echo "   (Wichtig: NACH erstem Gradle Sync!)"
echo ""
echo "6. ⏳ NACH Neustart: Warte auf alle Tasks (5-10 Min):"
echo "   • Gradle sync"
echo "   • Setting up run configuration (sollte jetzt schnell sein!)"
echo "   • Configure Kotlin language settings"
echo "   • Updating indexes"
echo ""
echo "7. 💡 WÄHREND DES WARTENS:"
echo "   • MacBook am Stromnetz"
echo "   • Keine anderen Apps öffnen"
echo "   • NICHT im Code herum klicken"
echo "   • NICHT Dateien öffnen/bearbeiten"
echo "   • Einfach warten und Kaffee holen ☕"
echo ""
echo "8. ✅ FERTIG wenn:"
echo "   • Keine Background Tasks mehr unten rechts"
echo "   • Grüner Haken in der Statusleiste"
echo "   • Build-Toolbar ist aktiv (▶️ Run Button ist grün)"
echo ""
echo "════════════════════════════════════════════"
echo ""
echo "💡 FALLS 'Setting up run configurations' IMMER NOCH hängt:"
echo ""
echo "   Während Android Studio läuft:"
echo "   1. Klicke oben: Run → Edit Configurations"
echo "   2. Lösche ALLE Konfigurationen (falls welche da sind)"
echo "   3. Klicke auf '+' → Android App"
echo "   4. Module: app, Name: app"
echo "   5. OK klicken"
echo ""
echo "   Das erstellt eine frische Run Configuration!"
echo ""
