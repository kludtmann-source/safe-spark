#!/usr/bin/env bash
set -e

cd /Users/knutludtmann/AndroidStudioProjects/SafeSpark

# Konfiguriere Git
git config user.name "Knut Ludtmann"
git config user.email "knut@safespark.app"

# Add alle Änderungen
git add -A

# Commit
git commit -m "🎨 Rebranding: KidGuard → SafeSpark

Vollständige Umbenennung (21 Dateien):

Kotlin (9): MainActivity, KidGuardEngine, GuardianAccessibilityService,
KidGuardDatabase, ChildConsentActivity, OnboardingActivity

Tests (3): KidGuardEngineTest, NotificationHelperTest, MLGroomingDetectorTest

XML (6): strings, themes, manifest, layouts

Docs (3): REBRANDING_COMPLETE, DEPLOYMENT_SUCCESS, GIT_COMMIT_ANLEITUNG

Statistik:
- 35 Kotlin-Referenzen → SafeSpark
- 9 XML-Referenzen → SafeSpark
- Build: 22 MB APK
- Deployed: Pixel 10 ✅"

echo "✅ Commit erfolgreich!"
git log -1 --oneline
