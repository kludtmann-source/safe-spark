#!/bin/zsh
# ========================================
# DIAGNOSE: Android Studio Background Tasks Problem
# ========================================

echo "🔍 Android Studio Background Tasks Diagnose"
echo "============================================="
echo ""

cd /Users/knutludtmann/AndroidStudioProjects/KidGuard || exit 1

# 1. Prozesse prüfen
echo "1️⃣  Prüfe laufende Prozesse..."
AS_PROCESSES=$(pgrep -f "Android Studio" | wc -l | xargs)
GRADLE_PROCESSES=$(pgrep -f "gradle.launcher.daemon" | wc -l | xargs)
KOTLIN_PROCESSES=$(pgrep -f "kotlin.daemon" | wc -l | xargs)

if [ "$AS_PROCESSES" -gt "0" ]; then
    echo "   ⚠️  Android Studio Prozesse: $AS_PROCESSES"
    echo "   PIDs: $(pgrep -f 'Android Studio' | xargs)"
else
    echo "   ✅ Keine Android Studio Prozesse"
fi

if [ "$GRADLE_PROCESSES" -gt "0" ]; then
    echo "   ⚠️  Gradle Daemons: $GRADLE_PROCESSES"
else
    echo "   ✅ Keine Gradle Daemons"
fi

if [ "$KOTLIN_PROCESSES" -gt "0" ]; then
    echo "   ⚠️  Kotlin Daemons: $KOTLIN_PROCESSES"
else
    echo "   ✅ Keine Kotlin Daemons"
fi
echo ""

# 2. Gradle Status
echo "2️⃣  Prüfe Gradle Status..."
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
if [ -x ./gradlew ]; then
    GRADLE_STATUS=$(./gradlew --status 2>/dev/null | grep -c "IDLE\|BUSY" || echo "0")
    if [ "$GRADLE_STATUS" -gt "0" ]; then
        echo "   ⚠️  Gradle Daemons aktiv:"
        ./gradlew --status 2>/dev/null | grep -E "PID|IDLE|BUSY" | head -5
    else
        echo "   ✅ Keine aktiven Gradle Daemons"
    fi
else
    echo "   ⚠️  gradlew nicht gefunden oder nicht ausführbar"
fi
echo ""

# 3. Projekt-Cache Status
echo "3️⃣  Prüfe Projekt-Caches..."
if [ -d .gradle ]; then
    GRADLE_SIZE=$(du -sh .gradle 2>/dev/null | cut -f1)
    echo "   📁 .gradle: $GRADLE_SIZE"
else
    echo "   ✅ .gradle nicht vorhanden (frisch)"
fi

if [ -d .idea ]; then
    IDEA_SIZE=$(du -sh .idea 2>/dev/null | cut -f1)
    echo "   📁 .idea: $IDEA_SIZE"
else
    echo "   ✅ .idea nicht vorhanden (frisch)"
fi

if [ -d build ]; then
    BUILD_SIZE=$(du -sh build 2>/dev/null | cut -f1)
    echo "   📁 build: $BUILD_SIZE"
else
    echo "   ✅ build nicht vorhanden (frisch)"
fi
echo ""

# 4. Gradle Properties prüfen
echo "4️⃣  Prüfe gradle.properties Einstellungen..."
if grep -q "org.gradle.configureondemand=true" gradle.properties 2>/dev/null; then
    echo "   ❌ org.gradle.configureondemand=true (PROBLEM!)"
    echo "      → Sollte auf 'false' gesetzt sein"
else
    echo "   ✅ org.gradle.configureondemand korrekt"
fi

if grep -q "org.gradle.parallel=true" gradle.properties 2>/dev/null; then
    echo "   ✅ org.gradle.parallel=true (gut)"
fi

if grep -q "org.gradle.caching=true" gradle.properties 2>/dev/null; then
    echo "   ✅ org.gradle.caching=true (gut)"
fi
echo ""

# 5. Android Studio Cache Status
echo "5️⃣  Prüfe Android Studio Caches..."
AS_CACHE_DIRS=$(find ~/Library/Caches/Google -maxdepth 1 -type d -name "AndroidStudio*" 2>/dev/null | wc -l | xargs)
if [ "$AS_CACHE_DIRS" -gt "0" ]; then
    echo "   📁 Android Studio Cache Verzeichnisse: $AS_CACHE_DIRS"
    AS_CACHE_SIZE=$(du -sh ~/Library/Caches/Google/AndroidStudio* 2>/dev/null | tail -1 | cut -f1)
    echo "   📊 Gesamtgröße: $AS_CACHE_SIZE"
else
    echo "   ✅ Keine Android Studio Caches (frisch bereinigt)"
fi
echo ""

# 6. Gradle Home Caches
echo "6️⃣  Prüfe Gradle System Caches..."
if [ -d ~/.gradle/caches ]; then
    GRADLE_CACHE_SIZE=$(du -sh ~/.gradle/caches 2>/dev/null | cut -f1)
    echo "   📁 ~/.gradle/caches: $GRADLE_CACHE_SIZE"

    TRANSFORMS=$(find ~/.gradle/caches -maxdepth 1 -type d -name "transforms-*" 2>/dev/null | wc -l | xargs)
    if [ "$TRANSFORMS" -gt "0" ]; then
        echo "   📁 Transform Caches: $TRANSFORMS Verzeichnisse"
    fi
else
    echo "   ✅ ~/.gradle/caches nicht vorhanden"
fi
echo ""

# 7. Empfehlungen
echo "════════════════════════════════════════════"
echo "📋 DIAGNOSE-ERGEBNIS & EMPFEHLUNGEN:"
echo "════════════════════════════════════════════"
echo ""

TOTAL_ISSUES=0

if [ "$AS_PROCESSES" -gt "0" ] || [ "$GRADLE_PROCESSES" -gt "0" ] || [ "$KOTLIN_PROCESSES" -gt "0" ]; then
    echo "❌ PROBLEM: Hintergrundprozesse laufen noch"
    echo "   LÖSUNG: ./force_cleanup_android_studio.sh"
    echo ""
    TOTAL_ISSUES=$((TOTAL_ISSUES + 1))
fi

if grep -q "org.gradle.configureondemand=true" gradle.properties 2>/dev/null; then
    echo "❌ PROBLEM: org.gradle.configureondemand=true"
    echo "   LÖSUNG: In gradle.properties auf 'false' ändern"
    echo ""
    TOTAL_ISSUES=$((TOTAL_ISSUES + 1))
fi

if [ -d .gradle ] && [ -d .idea ]; then
    echo "⚠️  HINWEIS: Alte Caches vorhanden"
    echo "   EMPFEHLUNG: ./fix_run_configurations.sh ausführen"
    echo ""
fi

if [ "$AS_CACHE_DIRS" -gt "2" ]; then
    echo "⚠️  HINWEIS: Viele Android Studio Cache-Verzeichnisse"
    echo "   EMPFEHLUNG: Bereinigung durchführen"
    echo ""
fi

if [ "$TOTAL_ISSUES" -eq "0" ]; then
    echo "✅✅✅ ALLES GUT! Keine kritischen Probleme gefunden."
    echo ""
    echo "Falls 'Setting up run configurations' trotzdem hängt:"
    echo "1. Warte mindestens 5-10 Minuten (ist oft normal!)"
    echo "2. Prüfe ob Gradle Sync fertig ist"
    echo "3. Falls immer noch hängt: ./force_stop_run_configurations.sh"
    echo ""
fi

echo "════════════════════════════════════════════"
echo ""
echo "💡 QUICK ACTIONS:"
echo ""
echo "Wenn 'Setting up run configurations' JETZT hängt:"
echo "  → ./force_stop_run_configurations.sh"
echo ""
echo "Für komplette Bereinigung (Android Studio geschlossen):"
echo "  → ./force_cleanup_android_studio.sh"
echo ""
echo "Für Run Configuration Fix (Android Studio geschlossen):"
echo "  → ./fix_run_configurations.sh"
echo ""
