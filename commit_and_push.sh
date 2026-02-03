#!/bin/bash
set -e

cd /Users/knutludtmann/AndroidStudioProjects/KidGuard

echo "🔧 Staging Änderungen..."
git add -A

echo "📝 Committing..."
git commit -m "feat: Explainable AI vollständig implementiert

Neue Features:
- AnalysisResult Data Class mit explanation und detectionMethod
- analyzeTextWithExplanation() Methode
- UI zeigt WARUM Risk erkannt wurde
- Notifications enthalten Erklärungen

Vorteile:
- Eltern verstehen Alarme besser
- Pädagogischer Wert (Grooming-Awareness)
- Minimal Performance-Overhead

Model Quantization:
- Analysiert und dokumentiert
- Niedrige Priorität (Performance OK)

Basierend auf Basani et al. 2025 Paper"

echo "✅ Commit erfolgreich!"
echo ""
echo "📤 Pushe zum Remote..."

# Prüfe ob Remote existiert
if git remote | grep -q origin; then
    git push origin main || git push origin master || echo "⚠️ Push fehlgeschlagen - prüfe Remote-Branch"
else
    echo "⚠️ Kein Remote 'origin' konfiguriert!"
    echo "Füge Remote hinzu mit: git remote add origin <URL>"
fi

echo ""
echo "✅ FERTIG!"
git log --oneline -3
