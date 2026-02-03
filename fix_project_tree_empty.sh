#!/bin/zsh
# ========================================
# FIX: Project Tree lädt keine Dateien
# ========================================

echo "🔧 FIX: Project Tree lädt nicht"
echo "================================="
echo ""

cd /Users/knutludtmann/AndroidStudioProjects/KidGuard || exit 1

echo "📋 Mögliche Ursachen:"
echo "   1. Indexing läuft noch"
echo "   2. Project Structure beschädigt"
echo "   3. .idea Cache korrupt"
echo "   4. Gradle Sync fehlgeschlagen"
echo ""

# Prüfe ob Android Studio läuft
AS_RUNNING=$(pgrep -f "Android Studio" | wc -l | xargs)

if [ "$AS_RUNNING" -gt "0" ]; then
    echo "⚠️  Android Studio läuft gerade"
    echo ""
    echo "WICHTIGE SCHRITTE IN ANDROID STUDIO (JETZT):"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "1️⃣  PRÜFE UNTEN RECHTS:"
    echo "   Läuft noch 'Indexing' oder 'Gradle Sync'?"
    echo "   → FALLS JA: Warte bis fertig! (Kann 5-10 Min dauern)"
    echo ""
    echo "2️⃣  PRÜFE PROJECT TOOL WINDOW:"
    echo "   Oben links bei 'Project' - siehst du einen Dropdown?"
    echo "   → Klicke darauf und wähle: 'Project' oder 'Android'"
    echo ""
    echo "3️⃣  FALLS IMMER NOCH LEER:"
    echo "   a) Klicke: View → Tool Windows → Project"
    echo "   b) Oder drücke: ⌘ + 1 (Cmd + 1)"
    echo ""
    echo "4️⃣  GRADLE SYNC ERZWINGEN:"
    echo "   File → Sync Project with Gradle Files"
    echo "   → Warte 1-2 Minuten"
    echo ""
    echo "5️⃣  FALLS IMMER NOCH NICHT SICHTBAR:"
    echo "   File → Invalidate Caches → Invalidate and Restart"
    echo "   → Nach Neustart 5 Min warten"
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
    echo "💡 HÄUFIGSTE URSACHE: Indexing läuft noch!"
    echo "   → Einfach 5-10 Minuten warten"
    echo ""

    read -p "Drücke ENTER wenn du Android Studio geschlossen hast..."
    echo ""
fi

echo "6️⃣  Bereinige .idea Verzeichnis..."
if [ -d .idea ]; then
    echo "   → Sichere alte .idea..."
    mv .idea .idea.backup.$(date +%s) 2>/dev/null
    echo "   ✅ .idea gesichert und entfernt"
else
    echo "   ✅ .idea existiert nicht"
fi
echo ""

echo "7️⃣  Erstelle frische .idea Struktur..."
mkdir -p .idea
cat > .idea/.gitignore << 'EOF'
# Android Studio - dynamisch generierte Dateien
workspace.xml
tasks.xml
usage.statistics.xml
shelf/
modules.xml
*.iml
EOF
echo "   ✅ Basis-Struktur erstellt"
echo ""

echo "8️⃣  Bereinige Gradle Cache..."
rm -rf .gradle/configuration-cache 2>/dev/null
rm -rf .gradle/*/fileChanges 2>/dev/null
rm -rf .gradle/*/fileHashes 2>/dev/null
echo "   ✅ Gradle Cache bereinigt"
echo ""

echo "✅ BEREINIGUNG ABGESCHLOSSEN!"
echo ""
echo "════════════════════════════════════════════"
echo "📋 JETZT IN ANDROID STUDIO:"
echo "════════════════════════════════════════════"
echo ""
echo "1. Starte Android Studio (falls geschlossen)"
echo ""
echo "2. Öffne das Projekt:"
echo "   File → Open → /Users/knutludtmann/AndroidStudioProjects/KidGuard"
echo ""
echo "3. WICHTIG: WARTE auf Gradle Sync (1-2 Min)"
echo "   → Unten rechts beobachten!"
echo ""
echo "4. Nach Gradle Sync: Warte auf Indexing (3-5 Min)"
echo "   → Unten rechts: 'Indexing...' muss fertig sein"
echo ""
echo "5. DANN sollte Project Tree gefüllt sein!"
echo ""
echo "6. Falls IMMER NOCH leer:"
echo "   → Klicke auf 'Project' Dropdown oben links"
echo "   → Wähle 'Android' statt 'Project'"
echo ""
echo "7. Falls das nicht hilft:"
echo "   → File → Invalidate Caches → Invalidate and Restart"
echo ""
echo "════════════════════════════════════════════"
echo ""
echo "💡 NORMAL: 5-10 Minuten warten nach Öffnen ist NORMAL!"
echo ""
