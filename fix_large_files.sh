#!/bin/bash
set -e

cd /Users/knutludtmann/AndroidStudioProjects/KidGuard

echo "🚨 Entferne große Dateien aus Git-History..."

# Füge .gitignore hinzu falls nicht vorhanden
if [ ! -f .gitignore ]; then
    cat > .gitignore << 'EOF'
# Build-Verzeichnisse
.gradle/
build/
app/build/
*/build/
.idea/
local.properties

# Python venv
ml/venv/
training/venv/
venv/

# Große Dateien
*.xml
*.dylib
*.keras
*.json

# Training-Daten
training/data/
training/models/
pan12-*.xml

# Logs
*.log
EOF
    echo "✅ .gitignore erstellt"
fi

# Entferne große Dateien aus Git (nur wenn sie im aktuellen Branch sind)
echo "📝 Entferne große Dateien aus dem Index..."

git rm --cached -r ml/venv/ 2>/dev/null || true
git rm --cached -r training/data/ 2>/dev/null || true
git rm --cached -r training/models/ 2>/dev/null || true
git rm --cached pan12-*.xml 2>/dev/null || true

echo "✅ Dateien aus Index entfernt"

# Commit die Änderungen
git add .gitignore
git commit -m "chore: Entferne große Dateien und füge .gitignore hinzu

- ml/venv/ aus Repository entfernt
- training/data/ aus Repository entfernt
- training/models/ aus Repository entfernt
- pan12-*.xml Dateien aus Repository entfernt
- .gitignore hinzugefügt" || echo "Keine Änderungen zum Committen"

echo ""
echo "📤 Versuche erneut zu pushen..."
git push origin main || echo "⚠️ Push fehlgeschlagen"

echo ""
echo "ℹ️ HINWEIS:"
echo "Falls der Push weiterhin fehlschlägt, müssen die Dateien aus der Git-History entfernt werden:"
echo "git filter-branch --force --index-filter 'git rm --cached --ignore-unmatch pan12-*.xml' --prune-empty --tag-name-filter cat -- --all"
echo ""
echo "✅ Fertig!"
