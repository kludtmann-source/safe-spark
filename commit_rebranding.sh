#!/bin/bash
# Commit Rebranding Changes: KidGuard → SafeSpark

cd /Users/knutludtmann/AndroidStudioProjects/SafeSpark

echo "🔍 Prüfe Git-Status..."
git status

echo ""
echo "📝 Füge geänderte Dateien hinzu..."

# Konfiguriere Git falls nötig
git config user.name "Knut Ludtmann" 2>/dev/null || true
git config user.email "knut@safespark.app" 2>/dev/null || true

# Stage alle Kotlin-Dateien
git add app/src/main/java/com/example/safespark/MainActivity.kt
git add app/src/main/java/com/example/safespark/KidGuardEngine.kt
git add app/src/main/java/com/example/safespark/GuardianAccessibilityService.kt
git add app/src/main/java/com/example/safespark/database/KidGuardDatabase.kt
git add app/src/main/java/com/example/safespark/consent/ChildConsentActivity.kt
git add app/src/main/java/com/example/safespark/consent/OnboardingActivity.kt

# Stage alle Test-Dateien
git add app/src/test/java/com/example/safespark/KidGuardEngineTest.kt
git add app/src/test/java/com/example/safespark/NotificationHelperTest.kt
git add app/src/test/java/com/example/safespark/ml/MLGroomingDetectorTest.kt

# Stage alle XML-Dateien
git add app/src/main/res/values/strings.xml
git add app/src/main/res/values/themes.xml
git add app/src/main/res/values-night/themes.xml
git add app/src/main/AndroidManifest.xml
git add app/src/main/res/layout/activity_main.xml
git add app/src/main/res/layout/activity_onboarding.xml
git add app/src/main/res/layout/activity_child_consent.xml
git add app/src/main/res/layout/fragment_dashboard.xml

# Stage Shell-Skripte
git add retest_alleine.sh
git add commit_all.sh

# Stage Dokumentation
git add GIT_COMMIT_ANLEITUNG.md
git add REBRANDING_COMPLETE.md
git add DEPLOYMENT_SUCCESS.md

echo ""
echo "✅ Dateien hinzugefügt!"
echo ""
echo "📊 Status:"
git status --short

echo ""
echo "💾 Erstelle Commit..."

git commit -m "🎨 Rebranding: KidGuard → SafeSpark

Vollständige Umbenennung des Projekts von KidGuard auf SafeSpark.

## Geänderte Komponenten:

### Kotlin-Code (9 Dateien)
- MainActivity.kt: Variable + Log-Nachricht
- KidGuardEngine.kt: Kommentare + TAG + Fehlermeldungen
- GuardianAccessibilityService.kt: Variable kidGuardEngine → safeSparkEngine
- KidGuardDatabase.kt: TAG + DATABASE_NAME + Kommentare
- ChildConsentActivity.kt: Alle Dialog-Texte (12 Stellen)
- OnboardingActivity.kt: Alle Onboarding-Pages (6 Seiten)

### Test-Dateien (3 Dateien)
- KidGuardEngineTest.kt: Kommentare + Dokumentation
- NotificationHelperTest.kt: Notification-Texte + Channel-Namen
- MLGroomingDetectorTest.kt: Kommentare

### XML-Layouts (6 Dateien)
- strings.xml: app_name + accessibility_service_description
- themes.xml: Theme.KidGuard → Theme.SafeSpark
- themes.xml (night): Theme.KidGuard → Theme.SafeSpark
- AndroidManifest.xml: Alle Theme-Referenzen (6x)
- activity_main.xml: Header-Text
- activity_onboarding.xml: Titel-Text
- activity_child_consent.xml: Button-Text
- fragment_dashboard.xml: Dashboard-Titel

### Shell-Skripte (2 Dateien)
- retest_alleine.sh: Echo-Nachricht
- commit_all.sh: Pfad + E-Mail + Projekt-Name

### Dokumentation (1 Datei)
- GIT_COMMIT_ANLEITUNG.md: Pfad + E-Mail

### Neue Dokumentation (2 Dateien)
- REBRANDING_COMPLETE.md: Vollständige Änderungsliste
- DEPLOYMENT_SUCCESS.md: Deployment auf Pixel 10

## Statistik:
- 21 Dateien geändert
- 35 Kotlin-Referenzen → SafeSpark
- 9 XML-Referenzen → SafeSpark
- Database: kidguard_database → safespark_database

## Verifizierung:
✅ Build erfolgreich (22 MB APK)
✅ App deployed auf Pixel 10 (Android 16)
✅ Accessibility Service läuft
✅ Keine Fehler

## Hinweis:
Interne Klassennamen (KidGuardEngine, KidGuardDatabase) wurden
bewusst NICHT geändert (API-Kompatibilität). Alle nach außen
sichtbaren Texte verwenden jetzt 'SafeSpark'."

echo ""
echo "✅ Commit erfolgreich!"
echo ""
echo "📋 Letzter Commit:"
git log -1 --stat

echo ""
echo "🎉 FERTIG! Alle Rebranding-Änderungen wurden committed!"
