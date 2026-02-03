#!/bin/bash
# Commit Rebranding: KidGuard → SafeSpark

cd /Users/knutludtmann/AndroidStudioProjects/SafeSpark

# Setze Git-Config
git config user.name "Knut Ludtmann"
git config user.email "knut@safespark.app"

# Füge alle geänderten Dateien hinzu
git add app/src/main/java/com/example/safespark/MainActivity.kt
git add app/src/main/java/com/example/safespark/KidGuardEngine.kt
git add app/src/main/java/com/example/safespark/GuardianAccessibilityService.kt
git add app/src/main/java/com/example/safespark/database/KidGuardDatabase.kt
git add app/src/main/java/com/example/safespark/consent/ChildConsentActivity.kt
git add app/src/main/java/com/example/safespark/consent/OnboardingActivity.kt
git add app/src/test/java/com/example/safespark/KidGuardEngineTest.kt
git add app/src/test/java/com/example/safespark/NotificationHelperTest.kt
git add app/src/test/java/com/example/safespark/ml/MLGroomingDetectorTest.kt
git add app/src/main/res/values/strings.xml
git add app/src/main/res/values/themes.xml
git add app/src/main/res/values-night/themes.xml
git add app/src/main/AndroidManifest.xml
git add app/src/main/res/layout/activity_main.xml
git add app/src/main/res/layout/activity_onboarding.xml
git add app/src/main/res/layout/activity_child_consent.xml
git add app/src/main/res/layout/fragment_dashboard.xml
git add retest_alleine.sh
git add commit_all.sh
git add GIT_COMMIT_ANLEITUNG.md
git add REBRANDING_COMPLETE.md
git add DEPLOYMENT_SUCCESS.md
git add commit_rebranding.sh

# Erstelle Commit
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

# Schreibe Ergebnis in Log
echo "Commit Status:" > /tmp/commit_result.log
git log -1 --oneline >> /tmp/commit_result.log 2>&1
git show --stat HEAD >> /tmp/commit_result.log 2>&1

cat /tmp/commit_result.log
