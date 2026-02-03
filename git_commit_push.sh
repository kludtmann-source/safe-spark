#!/bin/bash

# Git Commit & Push Script für KidGuard
# Datum: 28. Januar 2026

echo "🚀 Git Commit & Push für KidGuard"
echo "===================================="
echo ""

cd /Users/knutludtmann/AndroidStudioProjects/KidGuard

# Prüfe ob Git initialisiert ist
if [ ! -d ".git" ]; then
    echo "📦 Initialisiere Git Repository..."
    git init
    echo "✅ Git initialisiert"
else
    echo "✅ Git Repository bereits vorhanden"
fi

# Prüfe Git-Konfiguration
echo ""
echo "👤 Prüfe Git-Konfiguration..."
GIT_USER=$(git config user.name)
GIT_EMAIL=$(git config user.email)

if [ -z "$GIT_USER" ]; then
    echo "⚠️ Git user.name nicht konfiguriert"
    echo "   Setze mit: git config --global user.name 'Dein Name'"
    echo "   Oder lokal: git config user.name 'Dein Name'"
    echo ""
    echo "Möchtest du jetzt einen Namen setzen? (j/n)"
    read -r answer
    if [ "$answer" = "j" ]; then
        echo "Gib deinen Namen ein:"
        read -r name
        git config user.name "$name"
        echo "✅ Name gesetzt: $name"
    fi
else
    echo "✅ Git user.name: $GIT_USER"
fi

if [ -z "$GIT_EMAIL" ]; then
    echo "⚠️ Git user.email nicht konfiguriert"
    echo "   Setze mit: git config --global user.email 'deine@email.com'"
    echo ""
    echo "Möchtest du jetzt eine Email setzen? (j/n)"
    read -r answer
    if [ "$answer" = "j" ]; then
        echo "Gib deine Email ein:"
        read -r email
        git config user.email "$email"
        echo "✅ Email gesetzt: $email"
    fi
else
    echo "✅ Git user.email: $GIT_EMAIL"
fi

echo ""
echo "📋 Füge Dateien hinzu..."
git add -A

echo ""
echo "📊 Status:"
CHANGED=$(git status --short | wc -l | tr -d ' ')
echo "   $CHANGED Dateien geändert"

if [ "$CHANGED" -eq 0 ]; then
    echo "✅ Keine Änderungen zum Committen"
    echo ""
    echo "📤 Prüfe Push-Status..."
    git status
    exit 0
fi

echo ""
echo "💾 Erstelle Commit..."
git commit -m "feat: Room Database Integration complete

Priority 1.3 MVP Implementation

New Files:
- RiskEvent.kt (Entity)
- RiskEventDao.kt (13 queries)
- KidGuardDatabase.kt (Singleton)
- RiskEventRepository.kt (Business logic)
- RiskEventDaoTest.kt (7 tests)

Integration:
- GuardianAccessibilityService database persistence
- Real-time risk event storage

Technical:
- Room 2.5.2 with KAPT
- Executors for background ops
- LiveData for UI updates
- No Coroutines (KAPT compatibility)

Documentation:
- 9 comprehensive guides
- Quick-start tutorials
- Troubleshooting

Build:
- Fixed 18 KAPT errors
- BUILD SUCCESSFUL in 2s
- Zero compile errors

Stats:
- 23 files changed
- 17 new files
- ~3500 lines code
- ~8000 lines docs"

if [ $? -eq 0 ]; then
    echo "✅ Commit erfolgreich erstellt"
else
    echo "❌ Commit fehlgeschlagen"
    exit 1
fi

echo ""
echo "📤 Prüfe Remote-Konfiguration..."
REMOTE=$(git config --get remote.origin.url)

if [ -z "$REMOTE" ]; then
    echo "⚠️ Kein Remote 'origin' konfiguriert"
    echo ""
    echo "Um zu GitHub zu pushen, musst du zuerst ein Remote hinzufügen:"
    echo ""
    echo "1. Erstelle ein Repository auf GitHub:"
    echo "   https://github.com/new"
    echo ""
    echo "2. Dann führe aus:"
    echo "   git remote add origin https://github.com/USERNAME/KidGuard.git"
    echo "   git branch -M main"
    echo "   git push -u origin main"
    echo ""
    echo "Oder mit diesem Script:"
    echo "   ./git_setup_quick.sh"
    echo ""
    exit 0
else
    echo "✅ Remote konfiguriert: $REMOTE"
    echo ""
    echo "📤 Pushe zu GitHub..."
    git push

    if [ $? -eq 0 ]; then
        echo ""
        echo "🎉 ERFOLGREICH!"
        echo "✅ Commit erstellt"
        echo "✅ Zu GitHub gepusht"
        echo ""
        echo "🔗 Repository: $REMOTE"
    else
        echo ""
        echo "⚠️ Push fehlgeschlagen"
        echo ""
        echo "Mögliche Gründe:"
        echo "- Keine Internetverbindung"
        echo "- Authentifizierung fehlgeschlagen"
        echo "- Remote existiert nicht"
        echo ""
        echo "Versuche:"
        echo "   git push -u origin main"
        echo ""
    fi
fi

echo ""
echo "📊 Finaler Status:"
git status
