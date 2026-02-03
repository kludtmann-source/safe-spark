#!/bin/zsh
# Fix Android Studio "Updating indexes" Problem
# Verwendung: ./fix_indexing.sh

set -e

echo "🔧 Android Studio Indexing-Problem beheben"
echo "============================================"

# Farben
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Prüfe ob Android Studio läuft
if pgrep -f "Android Studio" > /dev/null; then
    echo "${RED}❌ Android Studio läuft noch!${NC}"
    echo ""
    echo "${YELLOW}WICHTIG: Bitte schließe Android Studio KOMPLETT (⌘ Cmd+Q)${NC}"
    echo "Drücke Enter, wenn Android Studio geschlossen ist..."
    read

    # Nochmal prüfen
    if pgrep -f "Android Studio" > /dev/null; then
        echo "${RED}❌ Android Studio läuft immer noch!${NC}"
        echo "Soll ich es beenden? (j/n)"
        read answer
        if [[ "$answer" == "j" || "$answer" == "J" ]]; then
            pkill -9 -f "Android Studio"
            echo "${GREEN}✅ Android Studio beendet${NC}"
            sleep 2
        else
            echo "Bitte schließe Android Studio manuell und führe das Skript erneut aus."
            exit 1
        fi
    fi
fi

# JAVA_HOME setzen
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

echo ""
echo "${YELLOW}📍 Schritt 1: Stoppe alle Gradle-Daemons...${NC}"
cd /Users/knutludtmann/AndroidStudioProjects/KidGuard
./gradlew --stop || echo "   Kein Daemon lief"
echo "${GREEN}✅ Gradle-Daemons gestoppt${NC}"

echo ""
echo "${YELLOW}📍 Schritt 2: Entferne Projekt-spezifische Caches...${NC}"
rm -rf .gradle
rm -rf .idea
rm -rf build
rm -rf app/build
rm -rf */build
echo "${GREEN}✅ Projekt-Caches entfernt${NC}"

echo ""
echo "${YELLOW}📍 Schritt 3: Bereinige Android Studio Caches und Indizes...${NC}"

# Application Support (System Caches)
AS_SUPPORT_DIRS=(
    ~/Library/Application\ Support/Google/AndroidStudio*
)

for dir in "${AS_SUPPORT_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        echo "   Bereinige: $dir"
        # Nur Caches/Index-Verzeichnisse löschen, nicht die gesamte Config
        rm -rf "$dir"/system/caches/* 2>/dev/null || true
        rm -rf "$dir"/system/index/* 2>/dev/null || true
        rm -rf "$dir"/system/compiler/* 2>/dev/null || true
        rm -rf "$dir"/system/conversion/* 2>/dev/null || true
        rm -rf "$dir"/system/external_build_system/* 2>/dev/null || true
        rm -rf "$dir"/system/tmp/* 2>/dev/null || true
        echo "${GREEN}   ✅ Cache bereinigt: $dir${NC}"
    fi
done

# Caches Verzeichnis
AS_CACHE_DIRS=(
    ~/Library/Caches/Google/AndroidStudio*
)

for dir in "${AS_CACHE_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        echo "   Lösche Cache: $dir"
        rm -rf "$dir"
        echo "${GREEN}   ✅ Cache gelöscht: $dir${NC}"
    fi
done

# Logs
AS_LOG_DIRS=(
    ~/Library/Logs/Google/AndroidStudio*
)

for dir in "${AS_LOG_DIRS[@]}"; do
    if [ -d "$dir" ]; then
        echo "   Lösche Logs: $dir"
        rm -rf "$dir"
        echo "${GREEN}   ✅ Logs gelöscht: $dir${NC}"
    fi
done

echo ""
echo "${YELLOW}📍 Schritt 4: Optimiere gradle.properties für besseres Indexing...${NC}"

# Füge Indexing-Optimierungen hinzu, falls nicht vorhanden
if ! grep -q "org.gradle.configureondemand" gradle.properties; then
    cat >> gradle.properties << 'EOF'

# Indexing und Performance-Optimierungen
org.gradle.configureondemand=true
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.daemon=true
EOF
    echo "${GREEN}✅ Performance-Optimierungen hinzugefügt${NC}"
else
    echo "${BLUE}ℹ️  Performance-Optimierungen bereits vorhanden${NC}"
fi

echo ""
echo "${YELLOW}📍 Schritt 5: Erstelle .idea Verzeichnis mit Optimierungen...${NC}"
mkdir -p .idea
cat > .idea/workspace.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<project version="4">
  <component name="PropertiesComponent">
    <property name="show.migrate.to.gradle.popup" value="false" />
  </component>
</project>
EOF
echo "${GREEN}✅ Workspace-Konfiguration erstellt${NC}"

echo ""
echo "${YELLOW}📍 Schritt 6: Erstelle schnellen Gradle Sync...${NC}"
./gradlew tasks --console=plain > /dev/null 2>&1 &
GRADLE_PID=$!
echo "   Gradle-Sync läuft im Hintergrund (PID: $GRADLE_PID)..."
sleep 3
if ps -p $GRADLE_PID > /dev/null; then
    echo "${BLUE}ℹ️  Gradle-Sync läuft noch, das ist OK${NC}"
else
    echo "${GREEN}✅ Gradle-Sync abgeschlossen${NC}"
fi

echo ""
echo "${GREEN}============================================"
echo "✅ Bereinigung abgeschlossen!"
echo "============================================${NC}"
echo ""
echo "${BLUE}Nächste Schritte:${NC}"
echo ""
echo "1. ${YELLOW}Starte Android Studio NEU${NC}"
echo ""
echo "2. ${YELLOW}Öffne das Projekt:${NC}"
echo "   File → Open → /Users/knutludtmann/AndroidStudioProjects/KidGuard"
echo ""
echo "3. ${YELLOW}WICHTIG - Beim ersten Öffnen:${NC}"
echo "   - Warte bis 'Gradle sync' abgeschlossen ist"
echo "   - Indexing wird dann automatisch starten"
echo "   - Das erste Indexing kann 5-10 Minuten dauern"
echo ""
echo "4. ${YELLOW}Falls Indexing immer noch zu lange dauert:${NC}"
echo "   File → Invalidate Caches → 'Clear file system cache and Local History'"
echo "   → 'Invalidate and Restart'"
echo ""
echo "${BLUE}Tipps zur Beschleunigung:${NC}"
echo "   - Schließe andere Programme (Chrome, etc.)"
echo "   - Lass das MacBook am Strom"
echo "   - Nutze kein VPN während des Indexing"
echo "   - Warte bis zum Ende - NICHT unterbrechen!"
echo ""
