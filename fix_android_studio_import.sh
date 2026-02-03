#!/bin/zsh
# Fix Android Studio Import - Komplette Bereinigung
# Verwendung: ./fix_android_studio_import.sh

set -e

echo "🔧 Android Studio Import-Problem beheben"
echo "=========================================="

# Farben für Ausgabe
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# JAVA_HOME setzen
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

if [ ! -d "$JAVA_HOME" ]; then
  echo "${RED}❌ Fehler: Android Studio JBR nicht gefunden!${NC}"
  echo "   Erwarteter Pfad: $JAVA_HOME"
  exit 1
fi

echo "${GREEN}✅ Java gefunden: $JAVA_HOME${NC}"
$JAVA_HOME/bin/java -version

# 1. Gradle-Daemon stoppen
echo ""
echo "${YELLOW}📍 Schritt 1: Stoppe Gradle-Daemon...${NC}"
./gradlew --stop || echo "Kein Daemon lief"

# 2. Gradle-Cache sichern und entfernen
echo ""
echo "${YELLOW}📍 Schritt 2: Bereinige Gradle-Caches...${NC}"
if [ -d ~/.gradle ]; then
  BACKUP_NAME=~/.gradle.backup.$(date +%Y%m%d_%H%M%S)
  mv ~/.gradle "$BACKUP_NAME"
  echo "${GREEN}✅ Gradle-Cache gesichert nach: $BACKUP_NAME${NC}"
else
  echo "   Kein ~/.gradle Verzeichnis vorhanden"
fi

# 3. Projekt-Caches entfernen
echo ""
echo "${YELLOW}📍 Schritt 3: Entferne Projekt-Caches...${NC}"
rm -rf .gradle .idea
echo "${GREEN}✅ Projekt-Caches entfernt (.gradle, .idea)${NC}"

# 4. Build-Verzeichnisse bereinigen
echo ""
echo "${YELLOW}📍 Schritt 4: Bereinige Build-Verzeichnisse...${NC}"
rm -rf build app/build
echo "${GREEN}✅ Build-Verzeichnisse entfernt${NC}"

# 5. Android Studio-Caches entfernen (optional, auskommentiert)
echo ""
echo "${YELLOW}📍 Schritt 5: Android Studio-Caches (übersprungen)${NC}"
echo "   Um auch Android Studio-Caches zu löschen, führe aus:"
echo "   ${YELLOW}rm -rf ~/Library/Caches/JetBrains/AndroidStudio*${NC}"
echo "   ${YELLOW}rm -rf ~/Library/Application\\ Support/Google/AndroidStudio*${NC}"
echo "   ${YELLOW}rm -rf ~/Library/Logs/Google/AndroidStudio*${NC}"

# 6. Gradle-Test
echo ""
echo "${YELLOW}📍 Schritt 6: Teste Gradle...${NC}"
./gradlew tasks --console=plain 2>&1 | head -20

echo ""
echo "${GREEN}=========================================="
echo "✅ Bereinigung abgeschlossen!"
echo "==========================================${NC}"
echo ""
echo "Nächste Schritte:"
echo "1. ${YELLOW}Schließe Android Studio komplett${NC} (Cmd+Q)"
echo "2. ${YELLOW}Warte 5 Sekunden${NC}"
echo "3. ${YELLOW}Starte Android Studio neu${NC}"
echo "4. ${YELLOW}Öffne das Projekt${NC} (File → Open → KidGuard)"
echo ""
echo "Falls Probleme weiterhin bestehen:"
echo "  - Lösche Android Studio-Caches manuell (siehe Schritt 5)"
echo "  - Invalidiere IDE Caches: File → Invalidate Caches → Invalidate and Restart"
echo ""
