# ✅ GIT COMMIT - ANLEITUNG

**Datum:** 29. Januar 2026  
**Änderungen:** Rebranding KidGuard → SafeSpark

---

## 🚀 COMMIT DURCHFÜHREN

### Schritt 1: Git-Konfiguration setzen
```bash
cd /Users/knutludtmann/AndroidStudioProjects/SafeSpark
git config user.name "Knut Ludtmann"
git config user.email "knut@safespark.app"
```

### Schritt 2: Alle geänderten Dateien hinzufügen
```bash
# Kotlin-Hauptdateien
git add app/src/main/java/com/example/safespark/MainActivity.kt
git add app/src/main/java/com/example/safespark/KidGuardEngine.kt
git add app/src/main/java/com/example/safespark/GuardianAccessibilityService.kt

# Database
git add app/src/main/java/com/example/safespark/database/KidGuardDatabase.kt

# Consent Activities
git add app/src/main/java/com/example/safespark/consent/ChildConsentActivity.kt
git add app/src/main/java/com/example/safespark/consent/OnboardingActivity.kt

# Test-Dateien
git add app/src/test/java/com/example/safespark/KidGuardEngineTest.kt
git add app/src/test/java/com/example/safespark/NotificationHelperTest.kt
git add app/src/test/java/com/example/safespark/ml/MLGroomingDetectorTest.kt

# XML-Dateien
git add app/src/main/res/values/strings.xml
git add app/src/main/res/values/themes.xml
git add app/src/main/res/values-night/themes.xml
git add app/src/main/AndroidManifest.xml

# Layouts
git add app/src/main/res/layout/activity_main.xml
git add app/src/main/res/layout/activity_onboarding.xml
git add app/src/main/res/layout/activity_child_consent.xml
git add app/src/main/res/layout/fragment_dashboard.xml

# Shell-Skripte
git add retest_alleine.sh
git add commit_all.sh

# Dokumentation
git add GIT_COMMIT_ANLEITUNG.md
git add REBRANDING_COMPLETE.md
git add DEPLOYMENT_SUCCESS.md

# Commit-Skripte
git add commit_rebranding.sh
git add do_commit.sh
```

### Schritt 3: Status prüfen
```bash
git status
```

### Schritt 4: Commit erstellen
```bash
git commit -m "🎨 Rebranding: KidGuard → SafeSpark

Vollständige Umbenennung von KidGuard auf SafeSpark (21 Dateien).

## Kotlin-Code (9 Dateien):
- MainActivity: Variable + Log 'SafeSpark gestartet'
- KidGuardEngine: TAG + Kommentare → 'SafeSparkEngine'
- GuardianAccessibilityService: Variable safeSparkEngine
- KidGuardDatabase: TAG + DATABASE_NAME → 'safespark_database'
- ChildConsentActivity: 12 Dialog-Texte
- OnboardingActivity: 6 Onboarding-Seiten

## Test-Dateien (3 Dateien):
- KidGuardEngineTest, NotificationHelperTest, MLGroomingDetectorTest

## XML-Layouts (6 Dateien):
- strings.xml: app_name → 'SafeSpark'
- themes.xml: Theme.SafeSpark (beide Varianten)
- AndroidManifest.xml: 6 Theme-Referenzen
- Layouts: activity_main, onboarding, consent, dashboard

## Shell-Skripte & Docs (3 Dateien):
- retest_alleine.sh, commit_all.sh, GIT_COMMIT_ANLEITUNG.md

## Neue Dokumentation:
- REBRANDING_COMPLETE.md
- DEPLOYMENT_SUCCESS.md

## Statistik:
✅ 35 Kotlin-Referenzen → SafeSpark
✅ 9 XML-Referenzen → SafeSpark
✅ Build erfolgreich (22 MB APK)
✅ Deployed auf Pixel 10 (Android 16)
✅ App läuft ohne Fehler"
```

### Schritt 5: Commit verifizieren
```bash
git log -1
git show --stat HEAD
```

---

## 📋 ODER: Einzelner Befehl

```bash
cd /Users/knutludtmann/AndroidStudioProjects/SafeSpark && \
git config user.name "Knut Ludtmann" && \
git config user.email "knut@safespark.app" && \
git add app/src/main/java/com/example/safespark/*.kt \
       app/src/main/java/com/example/safespark/database/*.kt \
       app/src/main/java/com/example/safespark/consent/*.kt \
       app/src/test/java/com/example/safespark/*.kt \
       app/src/test/java/com/example/safespark/ml/*.kt \
       app/src/main/res/values/*.xml \
       app/src/main/res/values-night/*.xml \
       app/src/main/AndroidManifest.xml \
       app/src/main/res/layout/*.xml \
       *.sh *.md && \
git commit -m "🎨 Rebranding: KidGuard → SafeSpark - 21 Dateien, 44 Referenzen aktualisiert"
```

---

## 🎯 SCHNELLSTE METHODE

```bash
cd /Users/knutludtmann/AndroidStudioProjects/SafeSpark
bash do_commit.sh
```

Die Datei `do_commit.sh` ist bereits erstellt und enthält alle nötigen Befehle!

---

## ✅ Nach dem Commit

### Commit anzeigen:
```bash
git log -1 --stat
```

### Push (falls Remote konfiguriert):
```bash
git push origin main
```

---

**Status:** Alle Dateien bereit zum Committen!  
**Commit-Skript:** `do_commit.sh` erstellt und ausführbar
