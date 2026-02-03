# 🚨 SOFORT-HILFE: "Setting up run configurations" hängt JETZT

**Status:** Android Studio läuft, "Setting up run configurations" dreht endlos

---

## ⚡ LÖSUNG 1: Fix WÄHREND Android Studio läuft (EMPFOHLEN)

### In neuem Terminal ausführen:

```bash
cd /Users/knutludtmann/AndroidStudioProjects/KidGuard
./force_stop_run_configurations.sh
```

**Das Skript:**
1. Stoppt Gradle Daemons
2. Stoppt Kotlin Daemon
3. Löscht temporäre Locks
4. Zeigt dir die nächsten Schritte in Android Studio

### Dann in Android Studio:

```
File → Invalidate Caches → Invalidate and Restart
```

**Nach Neustart: 3-5 Minuten warten!**

---

## ⚡ LÖSUNG 2: Manuelle Run Configuration (NOCH SCHNELLER)

Falls Android Studio läuft und du es nicht neu starten willst:

### In Android Studio (während es hängt):

1. **Klicke oben in der Menüleiste:**
   ```
   Run → Edit Configurations...
   ```

2. **Im Dialog:**
   - Siehst du alte Konfigurationen? → Alle löschen (❌)
   - Klicke auf **[+]** (Plus-Zeichen oben links)
   - Wähle: **Android App**

3. **Neue Konfiguration erstellen:**
   ```
   Name:    app
   Module:  KidGuard.app
   ```

4. **Klicke OK**

5. **Schließe den Dialog**

→ **Das sollte "Setting up run configurations" sofort beenden!** ✅

---

## ⏱️ Wie lange ist normal?

| Dauer | Status |
|-------|--------|
| 0-5 Min | ✅ Normal - warte ab |
| 5-10 Min | ⚠️ Etwas langsam, aber noch OK |
| 10-15 Min | ⚠️ Ungewöhnlich, aber kann bei erstem Mal vorkommen |
| 15+ Min | ❌ Problem! Nutze eine der Lösungen oben |

---

## 🔍 Ist der Task wirklich aktiv?

### Prüfen in Android Studio:

1. Klicke unten rechts auf das **⚙️ Background Tasks** Icon
2. Oder klicke direkt auf den hängenden Task
3. Siehst du einen **Progress-Balken der sich bewegt**?
   - **JA** → Es läuft noch, warte ab
   - **NEIN** → Es hängt, nutze Lösung 1 oder 2

---

## 🆘 Immer noch Problem?

Falls **beide Lösungen** nicht helfen:

### Nuclear Option:

```bash
# 1. Force Kill ALLES
pkill -9 -f "Android Studio"

# 2. Warte 5 Sekunden
sleep 5

# 3. Komplette Bereinigung
cd /Users/knutludtmann/AndroidStudioProjects/KidGuard
./force_cleanup_android_studio.sh

# 4. Android Studio neu starten
# 5. Projekt öffnen
# 6. 10 Minuten warten
```

---

## ✅ Erfolgsprüfung

Du weißt, dass es geklappt hat, wenn:

- ✅ Task "Setting up run configurations" ist verschwunden
- ✅ Unten rechts: keine hängenden Background Tasks mehr
- ✅ **▶️ Run Button (grün)** ist klickbar
- ✅ Im Dropdown neben Run Button steht "app"

---

## 💡 Verhindere das Problem in Zukunft

### Beim Öffnen des Projekts:

1. ✅ Android Studio starten
2. ✅ Projekt öffnen
3. ✅ **WARTEN bis Gradle Sync FERTIG ist** (1-2 Min)
4. ✅ **DANN ERST** andere Dinge machen
5. ✅ **NICHT sofort auf Run klicken!**

### Gradle Sync ist fertig wenn:

- ✅ Unten rechts: "BUILD SUCCESSFUL" oder grüner Haken
- ✅ Keine "Gradle Sync" Task mehr läuft
- ✅ Build-Output zeigt keine Aktivität mehr

---

**Datum:** 26. Januar 2026  
**Nächster Schritt:** Nutze Lösung 1 oder 2 oben! ⬆️
