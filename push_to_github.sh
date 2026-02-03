#!/bin/zsh
# ========================================
# PUSH TO GITHUB - kid-guard Repository
# ========================================

echo "🚀 Pushe zu GitHub: kludtmann-source/kid-guard"
echo "==============================================="
echo ""

cd /Users/knutludtmann/AndroidStudioProjects/KidGuard || exit 1

# Prüfe ob Remote existiert
echo "1️⃣  Prüfe Remote-Konfiguration..."
REMOTE_EXISTS=$(git remote -v | grep -c "origin" || echo "0")

if [ "$REMOTE_EXISTS" -eq "0" ]; then
    echo "   → Remote hinzufügen..."
    git remote add origin https://github.com/kludtmann-source/kid-guard.git
    echo "   ✅ Remote hinzugefügt"
else
    echo "   ✅ Remote existiert bereits"
    # Stelle sicher, dass URL korrekt ist
    git remote set-url origin https://github.com/kludtmann-source/kid-guard.git
    echo "   ✅ Remote URL aktualisiert"
fi
echo ""

# Stelle sicher dass Branch main heißt
echo "2️⃣  Setze Branch auf 'main'..."
git branch -M main
echo "   ✅ Branch ist 'main'"
echo ""

# Zeige Status
echo "3️⃣  Status vor Push:"
git status --short | head -10
echo ""

# Push
echo "4️⃣  Pushe zum Repository..."
echo ""
echo "   🔐 Falls nach Authentifizierung gefragt:"
echo "      Username: kludtmann-source"
echo "      Password: [Dein Personal Access Token]"
echo ""
echo "   📝 Token erstellen unter:"
echo "      https://github.com/settings/tokens"
echo ""
read -p "   Drücke ENTER um fortzufahren..."
echo ""

git push -u origin main

if [ $? -eq 0 ]; then
    echo ""
    echo "✅✅✅ ERFOLGREICH ZU GITHUB GEPUSHT! ✅✅✅"
    echo ""
    echo "🌐 Dein Repository:"
    echo "   https://github.com/kludtmann-source/kid-guard"
    echo ""
    echo "📋 Gepushte Dateien:"
    echo "   • 9 Troubleshooting-Dokumentationen"
    echo "   • 6 Automatisierungs-Skripte"
    echo "   • Aktualisiertes README.md"
    echo "   • Optimierte gradle.properties"
    echo ""
else
    echo ""
    echo "❌ Push fehlgeschlagen!"
    echo ""
    echo "Mögliche Ursachen:"
    echo "1. Authentifizierung fehlgeschlagen"
    echo "   → Nutze Personal Access Token, nicht Passwort!"
    echo "   → Erstellen: https://github.com/settings/tokens"
    echo ""
    echo "2. Keine Internet-Verbindung"
    echo "   → Prüfe WLAN"
    echo ""
    echo "3. Merge-Konflikt"
    echo "   → Führe 'git pull origin main' aus"
    echo ""
    echo "Für Hilfe siehe: GIT_COMMIT_SUCCESS.md"
    echo ""
fi
