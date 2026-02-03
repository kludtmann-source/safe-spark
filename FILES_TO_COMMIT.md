# 📝 Welche Dateien sollten committed werden?

**Datum:** 28. Januar 2026, 16:15 Uhr

---

## ✅ WICHTIGE DATEIEN - UNBEDINGT COMMITTEN:

### 1. **Dokumentation (Markdown-Files)**
```
✅ OPTION_A_SUCCESS.md          ← NEU! Muss committed werden!
✅ OPTION_A_FINAL_SUMMARY.md    ← Sollte schon committed sein
✅ commit_option_a.sh           ← Sollte schon committed sein
✅ quick_start_option_a.sh      ← Sollte schon committed sein
```

### 2. **Source Code (Kotlin/Java)**
```
✅ app/.../ml/StageProgressionDetector.kt  ← Sollte committed sein
✅ app/.../KidGuardEngine.kt               ← Sollte committed sein
✅ ml_training/quantize_model.py           ← Sollte committed sein
```

### 3. **Konfiguration**
```
✅ .gitignore                              ← Sollte committed sein
✅ 7_PAPERS_FINAL_SUMMARY.md              ← Updated
```

---

## ❌ NICHT COMMITTEN - BEREITS IN .GITIGNORE:

### Build-Verzeichnisse:
```
❌ build/                  ← Build-Artefakte
❌ app/build/              ← App Build
❌ .gradle/                ← Gradle Cache
❌ */build/                ← Alle Build-Ordner
```

### IDE-Dateien (meist schon committed):
```
⚠️  .idea/                 ← Nur wichtige Files
✅ .idea/gradle.xml        ← OK
✅ .idea/misc.xml          ← OK
❌ .idea/workspace.xml     ← NICHT committen
❌ .idea/tasks.xml         ← NICHT committen
```

### Große Dateien:
```
❌ *.xml (PAN12 Datasets)  ← > 100MB, in .gitignore
❌ ml/venv/                ← Python Virtual Env
❌ ml_training/venv/       ← Python Virtual Env
❌ *.dylib                 ← TensorFlow Libraries
❌ *.so                    ← Native Libraries
```

### Temporäre Dateien:
```
❌ *.log                   ← Log-Dateien
❌ *.apk                   ← Build-Artefakte
❌ *.aab                   ← App Bundles
❌ local.properties        ← Lokale Config
❌ .DS_Store               ← macOS Metadata
```

---

## 🔍 WIE FINDE ICH UNTRACKED FILES?

### In Android Studio:
```
1. Öffne "Commit" Tab (Cmd+K oder View → Tool Windows → Commit)
2. Schaue unter "Unversioned Files"
3. Wähle die relevanten aus
```

### Im Terminal:
```bash
cd ~/AndroidStudioProjects/KidGuard

# Alle untracked files
git ls-files --others --exclude-standard

# Nur wichtige (ohne build/)
git ls-files --others --exclude-standard | grep -v build | grep -v venv
```

---

## 🚀 SCHNELL-COMMIT FÜR FEHLENDE FILES:

Wenn `OPTION_A_SUCCESS.md` noch nicht committed ist:

```bash
cd ~/AndroidStudioProjects/KidGuard

# Add nur die wichtige Datei
git add OPTION_A_SUCCESS.md

# Commit
git commit -m "docs: Add OPTION_A_SUCCESS.md - Final summary"

# Push
git push origin main
```

---

## 📊 TYPISCHE ANZAHL UNTRACKED FILES:

```
Android-Projekt nach Build:
- build/       → ~5,000+ Files (NICHT committen!)
- .gradle/     → ~2,000+ Files (NICHT committen!)
- .idea/       → ~50+ Files (nur manche committen)
- app/build/   → ~10,000+ Files (NICHT committen!)
- venv/        → ~15,000+ Files (NICHT committen!)
                 ────────────────
TOTAL:           ~19,000+ Files meist UNWICHTIG!
```

**WICHTIG ZU COMMITTEN:** Nur ~5-10 Files! 📝

---

## ✅ COMMIT-STRATEGIE:

### Commit nur diese Typen:
```
✅ Source Code:     *.kt, *.java
✅ Dokumentation:   *.md (außer BUILD*.md)
✅ Scripts:         *.sh (ausführbare)
✅ Konfiguration:   build.gradle.kts, .gitignore
✅ Resources:       res/*, assets/* (ohne builds)
✅ Tests:           *Test.kt
```

### NICHT committen:
```
❌ Build-Output:    build/, *.apk, *.aab
❌ Dependencies:    venv/, node_modules/
❌ IDE-Temp:        workspace.xml, tasks.xml
❌ Large Files:     *.xml (> 100MB), *.dylib
❌ Logs:            *.log, *.txt
```

---

## 🎯 EMPFEHLUNG:

**Wenn Android Studio 19,933 untracked files zeigt:**

1. **99% davon ignorieren!** (build/, venv/, .gradle/)
2. **Nur diese committen:**
   - `OPTION_A_SUCCESS.md` (falls noch nicht)
   - Andere `.md` Dokumentation
   - Neue `.sh` Scripts
   - Eventuell neue `.kt` Files

3. **Prüfen mit:**
```bash
git status --short
```

4. **Selective Add:**
```bash
git add OPTION_A_SUCCESS.md
git add *.md  # Nur wenn sinnvoll
```

---

## 🔧 FIX FÜR ZU VIELE UNTRACKED FILES:

### Option 1: Gitignore erweitern
```bash
# Füge zu .gitignore hinzu:
echo "*/build/" >> .gitignore
echo "**/venv/" >> .gitignore
echo ".gradle/" >> .gitignore
echo "*.apk" >> .gitignore

git add .gitignore
git commit -m "chore: Extend .gitignore"
```

### Option 2: Nur wichtige Files adden
```bash
# Nur Markdown-Files
git add *.md

# Nur Source-Files
git add app/src/**/*.kt

# Nur Scripts
git add *.sh
```

### Option 3: Interaktiv wählen
```bash
git add -i  # Interactive Mode
```

---

## 📋 QUICK CHECK:

**Führe das aus:**
```bash
cd ~/AndroidStudioProjects/KidGuard

# Zeige nur wichtige untracked files
git ls-files --others --exclude-standard | \
  grep -E '\.(kt|java|md|sh|gradle)$' | \
  grep -v build | \
  head -20
```

**Das sind die Files die du committen solltest!**

---

## ✅ FINALE ANTWORT:

**Von den 19,933 untracked files:**
- **19,900+ Files** → IGNORIEREN (build/, venv/, .gradle/)
- **~5-10 Files** → COMMITTEN (*.md, *.sh, neue *.kt)

**Wahrscheinlich fehlt nur:**
```
OPTION_A_SUCCESS.md  ← Diese Datei!
```

**Quick Fix:**
```bash
git add OPTION_A_SUCCESS.md
git commit -m "docs: Add Option A success summary"
git push origin main
```

---

**Erstellt:** 28. Januar 2026, 16:15 Uhr  
**Status:** Hilfreich? Führe Quick Check aus! 🔍
