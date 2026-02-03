#!/bin/bash

# Check für wichtige untracked files
# Usage: ./check_untracked.sh

echo "🔍 KidGuard - Checking for important untracked files..."
echo "======================================================"
echo ""

cd ~/AndroidStudioProjects/KidGuard

# 1. Count total untracked
TOTAL=$(git ls-files --others --exclude-standard | wc -l)
echo "📊 Total untracked files: $TOTAL"
echo ""

# 2. Find wichtige untracked files
echo "✅ Important untracked files (should be committed):"
echo "---------------------------------------------------"

IMPORTANT=$(git ls-files --others --exclude-standard | \
  grep -E '\.(kt|java|md|sh|py|gradle)$' | \
  grep -v build | \
  grep -v venv | \
  grep -v '\.idea')

if [ -z "$IMPORTANT" ]; then
    echo "✅ Keine wichtigen untracked files gefunden!"
    echo "   → Alles scheint bereits committed zu sein."
else
    echo "$IMPORTANT"
    echo ""
    echo "💡 Diese Files solltest du committen mit:"
    echo "   git add <file>"
fi

echo ""
echo "---------------------------------------------------"

# 3. Find unwichtige untracked (nur Info)
echo ""
echo "❌ Unwichtige untracked files (in .gitignore):"
echo "---------------------------------------------------"

UNIMPORTANT=$(git ls-files --others --exclude-standard | \
  grep -E 'build/|venv/|\.gradle/|\.apk$|\.aab$' | \
  wc -l)

echo "📦 Build files: $UNIMPORTANT (werden automatisch ignoriert)"
echo ""

# 4. Git status summary
echo "---------------------------------------------------"
echo "📝 Git Status Summary:"
echo "---------------------------------------------------"
git status --short | head -10

if [ $(git status --short | wc -l) -eq 0 ]; then
    echo "✅ Working directory is clean!"
    echo "   Alles committed und gepusht."
fi

echo ""
echo "======================================================"
echo "✅ Check complete!"
echo ""
echo "💡 Next steps:"
echo "   1. Wenn wichtige files gefunden: git add <file>"
echo "   2. Commit: git commit -m 'docs: Add missing files'"
echo "   3. Push: git push origin main"
