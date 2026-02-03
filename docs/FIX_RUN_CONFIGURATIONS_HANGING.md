# 🔧 SOFORT-FIX: "Setting up run configurations" hängt WÄHREND Android Studio läuft

**Datum:** 26. Januar 2026  
**Problem:** "Setting up run configurations" läuft endlos in den Background Tasks

---

## ⚡ OPTION 1: Fix OHNE Android Studio zu schließen (SCHNELL)

Wenn Android Studio gerade läuft und "Setting up run configurations" endlos dreht:

### Schritt-für-Schritt:

1. **Stoppe den hängenden Task:**
   - Klicke unten rechts auf den Background Task
   - Oder: Klicke auf das ⚙️ Icon unten rechts
   - Suche "Setting up run configurations"
   - Klicke auf das ❌ oder "Cancel" falls möglich

2. **Invalidate Caches:**
   ```
   File → Invalidate Caches → Invalidate and Restart
   ```
   - ⚠️ WICHTIG: Wähle "Invalidate and Restart", NICHT "Just Restart"
   - Warte nach Neustart 2-3 Minuten

3. **Falls immer noch hängt - Manuelle Run Configuration:**
   - Klicke oben: `Run → Edit Configurations...`
   - Lösche ALLE vorhandenen Konfigurationen (falls welche da sind)
   - Klicke auf `+` (Plus) → `Android App`
   - Setze:
     - **Name:** `app`
     - **Module:** `KidGuard.app`
   - Klicke `OK`
   - Schließe das Fenster
   
   Das sollte den Task sofort beenden! ✅

---

## 🛑 OPTION 2: Komplette Bereinigung (GRÜNDLICH)

Wenn Option 1 nicht hilft, schließe Android Studio komplett und führe aus:

```bash
cd /Users/knutludtmann/AndroidStudioProjects/KidGuard
./fix_run_configurations.sh
```

**Das Skript macht:**
1. ✅ Löscht alle Run Configuration Caches
2. ✅ Löscht .idea komplett
3. ✅ Löscht Gradle Caches
4. ✅ Optimiert gradle.properties
5. ✅ Erstellt frische Basis-Struktur

**Dann:**
1. Starte Android Studio neu
2. Öffne das Projekt
3. Warte auf ersten Gradle Sync (1-2 Min)
4. Danach: `File → Invalidate Caches → Just Restart`
5. Warte geduldig (5-10 Min)

---

## 🔍 DIAGNOSE: Warum hängt es?

### Häufige Ursachen:

1. **Beschädigte Run Configuration Cache**
   - Android Studio versucht alte Konfiguration zu laden
   - Lösung: Cache löschen (Option 2)

2. **Gradle Sync Problem**
   - Gradle hat noch nicht fertig synced
   - Lösung: Warte bis Gradle Sync FERTIG ist (unten rechts prüfen)

3. **org.gradle.configureondemand=true Problem**
   - Diese Einstellung kann Probleme verursachen
   - ✅ Bereits auf `false` geändert in gradle.properties

4. **Zu viele parallele Indexing Tasks**
   - Android Studio überlastet
   - Lösung: Invalidate Caches + Restart

---

## 📋 Checkliste während Android Studio läuft:

- [ ] Ist Gradle Sync fertig? (unten rechts prüfen)
- [ ] Gibt es rote Fehler im Build-Output? (unten Tabs prüfen)
- [ ] Läuft noch "Indexing" oder andere Background Tasks?
- [ ] Habe ich geduldig 3-5 Minuten gewartet?

**Wenn JA zu allen:** → Nutze Option 1 (Manuelle Run Configuration)

**Wenn NEIN zu irgendeinem:** → Warte noch, ist normal!

---

## 💡 PRO-TIPPS

### Während des Wartens:

✅ **TUN:**
- MacBook am Stromnetz
- Andere Apps schließen
- Kaffee holen ☕
- Atmen 🧘

❌ **NICHT TUN:**
- Im Code herum klicken
- Dateien öffnen/schließen
- Build auslösen
- Settings ändern
- Ungeduldig werden 😤

### Zeitplan (NORMAL):

```
0:00 - Projekt öffnen
0:30 - Gradle Sync läuft
2:00 - Gradle Sync FERTIG ✅
2:10 - "Setting up run configurations" startet
2:30 - "Configure Kotlin" läuft
4:00 - "Updating indexes" läuft
7:00 - ALLES FERTIG ✅
```

**Total: 7-10 Minuten ist VÖLLIG NORMAL!** 🎉

---

## 🆘 Immer noch Problem?

Wenn nach 15 Minuten immer noch "Setting up run configurations" läuft:

1. **Force Stop:**
   ```bash
   # In neuem Terminal:
   pkill -9 -f "gradle.launcher.daemon"
   ```

2. **Dann in Android Studio:**
   ```
   File → Invalidate Caches → Invalidate and Restart
   ```

3. **Nach Neustart:**
   - Warte bis Gradle Sync FERTIG
   - Dann manuell Run Configuration erstellen (siehe Option 1, Schritt 3)

---

## ✅ Erfolgsprüfung

Du weißt, dass es geklappt hat, wenn:

- ✅ Keine Background Tasks mehr laufen (unten rechts leer)
- ✅ Grüner Haken in der Statusleiste
- ✅ Build-Toolbar ist aktiv
- ✅ **▶️ Run Button ist grün und klickbar**
- ✅ Im Dropdown steht "app" als Run Configuration

**Dann kannst du loslegen!** 🚀

---

## 📚 Verwandte Dateien

- `fix_run_configurations.sh` - Automatisches Fix-Skript
- `force_cleanup_android_studio.sh` - Komplette Bereinigung
- `BACKGROUND_PROCESSES_SOLVED.md` - Hintergrundprozess-Probleme

---

**Status:** Problem erkannt und Lösungen bereitgestellt  
**Letzte Aktualisierung:** 26. Januar 2026
