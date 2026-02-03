# ✅ Quick Action Checkliste - Starte JETZT!

**Ziel:** In 3-4 Arbeitstagen zum funktionsfähigen MVP

---

## 📋 TAG 1: Unit-Tests & Security (6-7 Stunden)

### ☐ Morning Session (3-4h): Unit-Tests Setup

#### 1. Test-Struktur erstellen
```bash
cd /Users/knutludtmann/AndroidStudioProjects/KidGuard
mkdir -p app/src/test/java/com/example/safespark/ml
mkdir -p app/src/test/java/com/example/safespark/auth
```

#### 2. Dependencies hinzufügen
Öffne `app/build.gradle.kts` und ergänze:
```kotlin
dependencies {
    // ...existing dependencies...
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
}
```

#### 3. Ersten Test schreiben: MLGroomingDetectorTest.kt
```bash
touch app/src/test/java/com/example/safespark/ml/MLGroomingDetectorTest.kt
```

#### 4. Tests ausführen
```bash
./gradlew test
```

**Erwartetes Ergebnis:** ✅ Mindestens 5 Tests laufen durch

---

### ☐ Afternoon Session (2-3h): Security Fix

#### 5. EncryptedSharedPreferences Dependency
```kotlin
// app/build.gradle.kts
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

#### 6. ParentAuthManager.kt updaten
- Ersetze normale SharedPreferences mit EncryptedSharedPreferences
- Teste Migration von alten PINs

#### 7. Test
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Teste PIN-Eingabe
```

**Erwartetes Ergebnis:** ✅ PIN ist verschlüsselt gespeichert

---

## 📋 TAG 2: Datenbank Setup (4-5 Stunden)

### ☐ Morning Session (2-3h): Room Database

#### 8. Room Dependencies
```kotlin
// app/build.gradle.kts
val room_version = "2.6.1"
implementation("androidx.room:room-runtime:$room_version")
implementation("androidx.room:room-ktx:$room_version")
ksp("androidx.room:room-compiler:$room_version")
```

#### 9. KSP Plugin hinzufügen
```kotlin
// app/build.gradle.kts (Top-Level)
plugins {
    // ...existing plugins...
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}
```

#### 10. Entity erstellen
```bash
mkdir -p app/src/main/java/com/example/safespark/data
touch app/src/main/java/com/example/safespark/data/RiskEvent.kt
touch app/src/main/java/com/example/safespark/data/RiskEventDao.kt
touch app/src/main/java/com/example/safespark/data/KidGuardDatabase.kt
```

---

### ☐ Afternoon Session (2h): DB Integration

#### 11. GuardianAccessibilityService.kt updaten
- Speichere RiskEvents in DB statt nur Logging

#### 12. Test
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Teste Texteingabe → Prüfe DB
adb shell run-as safesparkk
cd databases
ls -l  # Sollte kidguard_database zeigen
```

**Erwartetes Ergebnis:** ✅ Risiken werden in DB gespeichert

---

## 📋 TAG 3-4: Dashboard UI (12-16 Stunden)

### ☐ Day 3 Morning (4h): Basis-Layout

#### 13. Fragment erstellen
```bash
mkdir -p app/src/main/java/com/example/safespark/ui
touch app/src/main/java/com/example/safespark/ui/DashboardFragment.kt
touch app/src/main/java/com/example/safespark/ui/DashboardViewModel.kt
```

#### 14. Layouts erstellen
```bash
mkdir -p app/src/main/res/layout
touch app/src/main/res/layout/fragment_dashboard.xml
touch app/src/main/res/layout/item_risk_event.xml
```

#### 15. ViewModel Dependencies
```kotlin
// app/build.gradle.kts
implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
implementation("androidx.fragment:fragment-ktx:1.6.2")
```

---

### ☐ Day 3 Afternoon (4h): RecyclerView

#### 16. RiskEventAdapter erstellen
```bash
touch app/src/main/java/com/example/safespark/ui/RiskEventAdapter.kt
```

#### 17. MainActivity.kt updaten
- Zeige DashboardFragment statt leerem Screen
- Navigation Setup

#### 18. Test
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Erwartetes Ergebnis:** ✅ Dashboard zeigt Liste von Risiken

---

### ☐ Day 4 Morning (2-3h): Detail-Screen

#### 19. RiskDetailActivity erstellen
```bash
touch app/src/main/java/com/example/safespark/ui/RiskDetailActivity.kt
touch app/src/main/res/layout/activity_risk_detail.xml
```

#### 20. Dismiss-Funktionalität
- Button zum Ignorieren von False Positives
- Update in DB

---

### ☐ Day 4 Afternoon (2-3h): Einstellungen

#### 21. SettingsFragment erstellen
```bash
touch app/src/main/java/com/example/safespark/ui/SettingsFragment.kt
touch app/src/main/res/xml/preferences.xml
```

#### 22. PreferenceScreen implementieren
- Risiko-Schwelle (SeekBarPreference)
- Benachrichtigungen an/aus
- App-Whitelist

#### 23. Final Test
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Erwartetes Ergebnis:** ✅ Vollständiges Dashboard mit Historie & Einstellungen

---

## 🎯 ERFOLGS-KRITERIEN

Nach diesen 3-4 Tagen solltest du haben:

### ✅ Funktional
- [ ] Unit-Tests laufen (mindestens 5)
- [ ] PIN ist verschlüsselt (EncryptedSharedPreferences)
- [ ] Datenbank speichert Risiko-Events
- [ ] Dashboard zeigt Liste der Risiken
- [ ] Detail-Screen für einzelne Risiken
- [ ] Einstellungen funktionieren

### ✅ Code-Qualität
- [ ] Keine TODO-Kommentare für Sicherheit
- [ ] Tests dokumentieren erwartes Verhalten
- [ ] DB-Migrationen vorbereitet

### ✅ UX
- [ ] App ist nicht mehr "leer"
- [ ] Eltern können Historie sehen
- [ ] Schwelle anpassbar

---

## 🚀 NACH DEN 4 TAGEN

### Sofort einsatzbereit:
```bash
# Release-Build erstellen
./gradlew assembleRelease

# APK signieren (Keystore erstellen falls nötig)
keytool -genkey -v -keystore safespark-release.keystore \
  -alias safespark -keyalg RSA -keysize 2048 -validity 10000

# Build & Sign
./gradlew bundleRelease
```

### Play Store Vorbereitung:
1. **Screenshots erstellen** (Emulator oder Gerät)
2. **Privacy Policy schreiben** (Template nutzen)
3. **App-Beschreibung** (Deutsch + Englisch)
4. **Content Rating Questionnaire** ausfüllen

---

## 📊 Fortschritts-Tracking

### TAG 1 - Unit-Tests & Security
- [ ] Tests-Setup ✅
- [ ] Dependencies hinzugefügt ✅
- [ ] MLGroomingDetectorTest.kt ✅
- [ ] KidGuardEngineTest.kt ✅
- [ ] EncryptedSharedPreferences ✅
- [ ] Security-Tests ✅

**Status:** ___ / 6 erledigt

---

### TAG 2 - Datenbank
- [ ] Room Dependencies ✅
- [ ] KSP Plugin ✅
- [ ] RiskEvent Entity ✅
- [ ] RiskEventDao ✅
- [ ] KidGuardDatabase ✅
- [ ] Integration in Service ✅
- [ ] DB-Tests ✅

**Status:** ___ / 7 erledigt

---

### TAG 3 - Dashboard UI (Teil 1)
- [ ] DashboardFragment ✅
- [ ] DashboardViewModel ✅
- [ ] Layouts erstellt ✅
- [ ] RecyclerView Setup ✅
- [ ] RiskEventAdapter ✅
- [ ] LiveData Anbindung ✅

**Status:** ___ / 6 erledigt

---

### TAG 4 - Dashboard UI (Teil 2)
- [ ] RiskDetailActivity ✅
- [ ] Dismiss-Funktionalität ✅
- [ ] SettingsFragment ✅
- [ ] PreferenceScreen ✅
- [ ] Schwellen-Anpassung ✅
- [ ] Final Testing ✅

**Status:** ___ / 6 erledigt

---

## 💡 TIPPS FÜR EFFIZIENZ

### 1. Nutze Android Studio Templates
```
File → New → Fragment (ViewModel) → Wähle Template
```

### 2. Kopiere & Passe an
- Nutze bestehende Kotlin-Dateien als Vorlage
- ML_MODEL_DOCUMENTATION.md hat Code-Beispiele

### 3. Teste kontinuierlich
```bash
# Schnell-Test nach jeder Änderung
./gradlew assembleDebug && adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 4. Logcat immer offen
```bash
# In separatem Terminal
adb logcat | grep -E "KidGuard|MainActivity|Dashboard"
```

---

## 🆘 HILFE BEI PROBLEMEN

### Problem: Tests schlagen fehl
```bash
# Detaillierte Logs
./gradlew test --info
# Prüfe build/reports/tests/test/index.html
```

### Problem: Room-Compiler-Fehler
```bash
# Clean & Rebuild
./gradlew clean
./gradlew build --refresh-dependencies
```

### Problem: APK installiert nicht
```bash
# Deinstalliere alte Version
adb uninstall safesparkk
# Neu installieren
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Problem: UI zeigt nichts an
```bash
# Logcat prüfen
adb logcat | grep -E "E/|W/|KidGuard"
```

---

## ✅ FERTIG?

### Wenn alle 4 Tage abgeschlossen:

#### Gratulation! 🎉

Du hast jetzt:
- ✅ Funktionierendes Dashboard
- ✅ Datenpersistenz
- ✅ Unit-Tests
- ✅ Sichere PIN-Speicherung
- ✅ MVP-Ready App

#### Nächste Schritte:
1. **Freunde/Familie testen lassen** (Beta-Testing)
2. **Feedback sammeln**
3. **Priorität 2 Features** (siehe EMPFEHLUNGEN_ROADMAP.md)
4. **Play Store Submission vorbereiten**

---

## 📞 Weitere Fragen?

→ Siehe **EMPFEHLUNGEN_ROADMAP.md** für detaillierte Erklärungen  
→ Siehe **ML_MODEL_DOCUMENTATION.md** für ML-Details  
→ Siehe **README.md** für allgemeine Infos

---

**Erstellt:** 26. Januar 2026  
**Version:** 1.0  
**Geschätzter Zeitaufwand:** 24-33 Stunden = 3-4 Arbeitstage

**VIEL ERFOLG! 🚀**
