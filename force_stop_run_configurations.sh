#!/bin/zsh
# ========================================
# FORCE STOP: "Setting up run configurations"
# Kann ausgeführt werden WÄHREND Android Studio läuft!
# ========================================

echo "🛑 FORCE STOP: Setting up run configurations"
echo "============================================="
echo ""
echo "⚠️  ACHTUNG: Dies beendet Gradle-Prozesse während Android Studio läuft!"
echo ""
read -q "REPLY?Fortfahren? (y/n) "
echo ""
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Abgebrochen."
    exit 0
fi

echo "1️⃣  Stoppe alle Gradle Daemon Prozesse..."
pkill -f "gradle.launcher.daemon.bootstrap.GradleDaemon"
echo "   ✅ Gradle Daemons gestoppt"
echo ""

echo "2️⃣  Stoppe Kotlin Compiler Daemon..."
pkill -f "kotlin.daemon.KotlinCompileDaemon"
echo "   ✅ Kotlin Daemon gestoppt"
echo ""

echo "3️⃣  Lösche temporäre Run Configuration Locks..."
cd /Users/knutludtmann/AndroidStudioProjects/KidGuard
rm -rf .gradle/configuration-cache 2>/dev/null
rm -rf .gradle/*/fileChanges 2>/dev/null
rm -rf .gradle/*/fileHashes 2>/dev/null
echo "   ✅ Locks gelöscht"
echo ""

echo "✅ Prozesse beendet!"
echo ""
echo "════════════════════════════════════════════"
echo "📋 JETZT IN ANDROID STUDIO:"
echo "════════════════════════════════════════════"
echo ""
echo "1. Gehe zu Android Studio"
echo ""
echo "2. Klicke: File → Invalidate Caches"
echo "   → Wähle: 'Invalidate and Restart'"
echo "   → Klicke: 'Invalidate and Restart'"
echo ""
echo "3. Warte nach Neustart 2-3 Minuten"
echo ""
echo "4. Falls IMMER NOCH hängt:"
echo "   a) Run → Edit Configurations..."
echo "   b) Lösche alle Konfigurationen"
echo "   c) Klicke '+' → Android App"
echo "   d) Name: app, Module: KidGuard.app"
echo "   e) OK klicken"
echo ""
echo "   Das sollte es beheben! ✅"
echo ""
echo "════════════════════════════════════════════"
echo ""
