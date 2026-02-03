#!/bin/bash
set -e

cd /Users/knutludtmann/AndroidStudioProjects/KidGuard

echo "🔍 Git-Repository prüfen..."
if [ ! -d .git ]; then
    echo "📁 Initialisiere Git-Repository..."
    git init
    git config user.name "KidGuard Dev"
    git config user.email "dev@kidguard.app"
fi

echo ""
echo "📋 Staging aller Änderungen..."
# Exclude build folders and IDE files
git add -A
git reset -- app/build/ 2>/dev/null || true
git reset -- .gradle/ 2>/dev/null || true
git reset -- build/ 2>/dev/null || true
git reset -- .idea/ 2>/dev/null || true
git reset -- local.properties 2>/dev/null || true
git reset -- *.apk 2>/dev/null || true
git reset -- *.log 2>/dev/null || true

echo ""
echo "📊 Geänderte Dateien:"
git status --short

echo ""
echo "💾 Erstelle Commits..."

# Commit 1: Core functionality fixes
echo "  1️⃣ Assessment-Pattern Priority Fix..."
git add app/src/main/java/com/example/safespark/KidGuardEngine.kt 2>/dev/null || true
git commit -m "feat: Assessment-Pattern hat jetzt Priorität bei Score-Berechnung

- Assessment-Patterns (z.B. 'alleine', 'zimmer') überschreiben andere Scores
- Wenn Assessment > 0.5, wird dieser Score direkt zurückgegeben
- Verhindert Verwässerung durch gewichtete Berechnung (85% statt 11%)
- Basierend auf Frontiers Pediatrics & ArXiv Papers" 2>/dev/null || true

# Commit 2: Logging improvements
echo "  2️⃣ Logging-Verbesserungen..."
git add app/src/main/java/com/example/safespark/GuardianAccessibilityService.kt 2>/dev/null || true
git add app/src/main/java/com/example/safespark/MainActivity.kt 2>/dev/null || true
git commit -m "feat: Verbesserte In-App-Logs und Version-Marker

- LogBuffer wird direkt in sendRiskNotification() aufgerufen
- Version-Marker für Debugging (VERSION: 2.0-ASSESSMENT-FIX-ACTIVE)
- Score wird als Prozent angezeigt (85% statt 0.85)
- Bessere Fehlerbehandlung für Notifications
- Try-catch mit detaillierten Fehlermeldungen" 2>/dev/null || true

# Commit 3: Paper analysis and documentation
echo "  3️⃣ Paper-Analyse und Dokumentation..."
git add PAPERS_REFLECTION_ANALYSIS.md 2>/dev/null || true
git add FINAL_FIX_SCORE_DISPLAY.md 2>/dev/null || true
git add QUICK_FIX_SCORE.md 2>/dev/null || true
git add FINALE_ANLEITUNG_SCORE_FIX.md 2>/dev/null || true
git add EINFACHSTE_LOESUNG.md 2>/dev/null || true
git commit -m "docs: Umfassende Paper-Analyse und Troubleshooting-Guides

- Analyse von 5 wissenschaftlichen Papers
- Vergleich aktuelle Implementierung vs. Paper-Empfehlungen
- Identifizierung von Verbesserungspotentialen
- Schritt-für-Schritt Troubleshooting-Anleitungen
- Emergency-Fix-Scripts für APK-Installation" 2>/dev/null || true

# Commit 4: Scripts and tools
echo "  4️⃣ Build- und Fix-Scripts..."
git add emergency_fix.sh 2>/dev/null || true
git add fix_score_simple.sh 2>/dev/null || true
git commit -m "build: Emergency-Fix-Scripts für APK-Deployment

- Automatisierte Deinstallation alter APKs
- Cache-Clearing (app/build, .gradle)
- Kompletter Neu-Build
- Accessibility-Service-Aktivierung
- Vereinfachte Version für fehlerfreien Build" 2>/dev/null || true

# Commit 5: Configuration updates
echo "  5️⃣ Gradle-Konfiguration..."
git add gradle.properties 2>/dev/null || true
git commit -m "fix: Gradle JDK-Konfiguration für Android Studio

- Embedded JDK als Standard gesetzt
- Behebt 'Invalid Gradle JDK configuration' Fehler
- org.gradle.java.home auf Android Studio JDK gesetzt" 2>/dev/null || true

# Commit 6: Database and tests
echo "  6️⃣ Tests aktualisiert..."
git add app/src/androidTest/java/com/example/safespark/database/RiskEventDaoTest.kt 2>/dev/null || true
git commit -m "fix: RiskEventDaoTest Type-Mismatch behoben

- assertEquals für nullable Float korrigiert
- Elvis-Operator für Float? zu Float Konversion" 2>/dev/null || true

# Commit remaining files
echo "  7️⃣ Übrige Änderungen..."
git add . 2>/dev/null || true
git commit -m "chore: Weitere Dokumentation und Cleanup

- Aktualisierte Build-Logs
- Session-Zusammenfassungen
- Code-Kommentare verbessert" 2>/dev/null || true

echo ""
echo "✅ Alle Commits erstellt!"
echo ""
echo "📤 Git-Log:"
git log --oneline --graph -7

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ FERTIG!"
echo ""
echo "Alle Änderungen wurden committet."
echo ""
echo "Zum Pushen (falls Remote konfiguriert):"
echo "  git remote add origin <URL>"
echo "  git push -u origin main"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
