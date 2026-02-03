#!/bin/zsh
# ========================================
# Hardware-Check für Android Studio
# ========================================

echo "💻 Hardware-Check für Android Studio"
echo "====================================="
echo ""

# Mac Modell und Jahr
echo "1️⃣  Mac Informationen:"
MODEL=$(system_profiler SPHardwareDataType | grep "Model Name" | awk -F': ' '{print $2}')
CHIP=$(system_profiler SPHardwareDataType | grep "Chip" | awk -F': ' '{print $2}')
if [ -z "$CHIP" ]; then
    CHIP=$(system_profiler SPHardwareDataType | grep "Processor Name" | awk -F': ' '{print $2}')
fi
CORES=$(sysctl -n hw.ncpu)
RAM_GB=$(( $(sysctl -n hw.memsize) / 1024 / 1024 / 1024 ))

echo "   Model:      $MODEL"
echo "   Prozessor:  $CHIP"
echo "   CPU Kerne:  $CORES"
echo "   RAM:        ${RAM_GB} GB"
echo ""

# Bewertung
echo "2️⃣  Bewertung für Android Studio:"
echo ""

# CPU Bewertung
if [[ "$CHIP" =~ "Apple M" ]]; then
    echo "   CPU:  🟢 EXCELLENT (Apple Silicon)"
    echo "         → 3-4x schneller als Intel!"
    SCORE_CPU=5
elif [[ "$CHIP" =~ "Intel Core i9" ]]; then
    echo "   CPU:  🟢 SEHR GUT (Intel i9)"
    SCORE_CPU=4
elif [[ "$CHIP" =~ "Intel Core i7" ]]; then
    echo "   CPU:  🟡 GUT (Intel i7)"
    echo "         → M-Series wäre 2-3x schneller"
    SCORE_CPU=3
elif [[ "$CHIP" =~ "Intel Core i5" ]]; then
    echo "   CPU:  🟠 OKAY (Intel i5)"
    echo "         → Spürbar langsamer bei Android Studio"
    SCORE_CPU=2
else
    echo "   CPU:  🔴 LANGSAM"
    echo "         → Upgrade stark empfohlen"
    SCORE_CPU=1
fi
echo ""

# RAM Bewertung
if [ $RAM_GB -ge 32 ]; then
    echo "   RAM:  🟢 EXCELLENT (${RAM_GB} GB)"
    echo "         → Perfekt für große Projekte!"
    SCORE_RAM=5
elif [ $RAM_GB -ge 16 ]; then
    echo "   RAM:  🟢 SEHR GUT (${RAM_GB} GB)"
    echo "         → Ideal für Android Studio"
    SCORE_RAM=4
elif [ $RAM_GB -ge 12 ]; then
    echo "   RAM:  🟡 GUT (${RAM_GB} GB)"
    echo "         → Ausreichend, aber 16GB wären besser"
    SCORE_RAM=3
elif [ $RAM_GB -ge 8 ]; then
    echo "   RAM:  🟠 MINIMUM (${RAM_GB} GB)"
    echo "         → Android Studio braucht viel RAM"
    echo "         → Schließe andere Apps!"
    SCORE_RAM=2
else
    echo "   RAM:  🔴 ZU WENIG (${RAM_GB} GB)"
    echo "         → Upgrade dringend empfohlen"
    SCORE_RAM=1
fi
echo ""

# CPU Kerne Bewertung
if [ $CORES -ge 8 ]; then
    echo "   Kerne: 🟢 EXCELLENT ($CORES Kerne)"
    SCORE_CORES=5
elif [ $CORES -ge 6 ]; then
    echo "   Kerne: 🟢 SEHR GUT ($CORES Kerne)"
    SCORE_CORES=4
elif [ $CORES -ge 4 ]; then
    echo "   Kerne: 🟡 GUT ($CORES Kerne)"
    SCORE_CORES=3
elif [ $CORES -ge 2 ]; then
    echo "   Kerne: 🟠 MINIMUM ($CORES Kerne)"
    echo "          → Mehr Kerne = schnellerer Build"
    SCORE_CORES=2
else
    echo "   Kerne: 🔴 ZU WENIG ($CORES Kerne)"
    SCORE_CORES=1
fi
echo ""

# Festplatte
echo "3️⃣  Festplatten-Info:"
DISK_FREE=$(df -h / | awk 'NR==2 {print $4}')
echo "   Freier Speicher: $DISK_FREE"

FREE_GB=$(df -g / | awk 'NR==2 {print $4}')
if [ $FREE_GB -lt 20 ]; then
    echo "   ⚠️  WARNUNG: Wenig Speicher frei!"
    echo "      → Android Studio braucht min. 50GB"
    echo "      → Lösche alte Dateien/Apps"
fi
echo ""

# Gesamt-Score
TOTAL_SCORE=$(( (SCORE_CPU + SCORE_RAM + SCORE_CORES) / 3 ))

echo "4️⃣  Gesamt-Bewertung:"
echo ""
if [ $TOTAL_SCORE -ge 4 ]; then
    echo "   🟢 EXCELLENT"
    echo "   Dein Mac ist perfekt für Android Studio!"
    echo ""
    echo "   Erwartete Zeiten:"
    echo "   • Gradle Sync:      30-60 Sek"
    echo "   • Indexing:         2-5 Min"
    echo "   • Clean Build:      30-60 Sek"
elif [ $TOTAL_SCORE -ge 3 ]; then
    echo "   🟡 GUT"
    echo "   Dein Mac schafft Android Studio, aber nicht optimal."
    echo ""
    echo "   Erwartete Zeiten:"
    echo "   • Gradle Sync:      1-3 Min"
    echo "   • Indexing:         5-10 Min"
    echo "   • Clean Build:      1-2 Min"
    echo ""
    echo "   💡 Upgrade-Empfehlung:"
    if [ $SCORE_RAM -lt 4 ]; then
        echo "   → Mehr RAM (min. 16GB) würde am meisten helfen"
    fi
    if [[ ! "$CHIP" =~ "Apple M" ]]; then
        echo "   → M2/M3 Mac wäre 2-3x schneller"
    fi
else
    echo "   🟠 LANGSAM"
    echo "   Dein Mac hat Probleme mit Android Studio."
    echo ""
    echo "   Erwartete Zeiten:"
    echo "   • Gradle Sync:      2-5 Min"
    echo "   • Indexing:         10-20 Min"
    echo "   • Clean Build:      2-5 Min"
    echo ""
    echo "   ⚠️  DRINGEND EMPFOHLEN:"
    if [ $SCORE_RAM -lt 3 ]; then
        echo "   → Mehr RAM (min. 16GB)"
    fi
    if [ $SCORE_CPU -lt 3 ]; then
        echo "   → Neuerer Mac mit Apple Silicon"
    fi
    echo ""
    echo "   💰 Beste Investition:"
    echo "   → MacBook Air M2 (16GB RAM) ~1400-1600€"
    echo "   → Macht alles 3-4x schneller!"
fi
echo ""

echo "5️⃣  Optimierungs-Tipps für JETZT:"
echo ""
echo "   ✅ Schließe Chrome/Browser (spart RAM)"
echo "   ✅ Schließe Slack, Teams, etc."
echo "   ✅ MacBook am Strom lassen"
echo "   ✅ Nur Android Studio offen"
echo "   ✅ Andere Apps beenden während Build"

if [ $FREE_GB -lt 50 ]; then
    echo "   ⚠️  Festplatte aufräumen!"
    echo "      → Lösche alte Dateien"
    echo "      → Leere Papierkorb"
    echo "      → ~/Downloads aufräumen"
fi
echo ""

echo "════════════════════════════════════════════"
echo "📖 Mehr Infos: HARDWARE_PERFORMANCE_GUIDE.md"
echo "════════════════════════════════════════════"
echo ""
