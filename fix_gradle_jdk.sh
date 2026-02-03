#!/bin/bash
# Fix Gradle JDK Configuration

echo "============================================"
echo "🔧 Fix Gradle JDK Configuration"
echo "============================================"
echo ""

cd /Users/knutludtmann/AndroidStudioProjects/KidGuard

echo "1️⃣ Prüfe Android Studio JDK..."
ANDROID_STUDIO_JDK="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

if [ -d "$ANDROID_STUDIO_JDK" ]; then
    echo "✅ Android Studio JDK gefunden: $ANDROID_STUDIO_JDK"
else
    echo "❌ Android Studio JDK nicht gefunden!"
    echo "   Erwarteter Pfad: $ANDROID_STUDIO_JDK"
    echo ""
    echo "Alternativen:"
    echo "1. Prüfe ob Android Studio installiert ist"
    echo "2. Oder nutze System JDK:"
    /usr/libexec/java_home -V 2>&1 | grep -E "Java|jdk" | head -3
    exit 1
fi

echo ""
echo "2️⃣ Setze JAVA_HOME..."
export JAVA_HOME="$ANDROID_STUDIO_JDK"
echo "✅ JAVA_HOME=$JAVA_HOME"

echo ""
echo "3️⃣ Prüfe Java Version..."
"$JAVA_HOME/bin/java" -version 2>&1 | head -3

echo ""
echo "4️⃣ Lösche Gradle Cache..."
rm -rf ~/.gradle/caches
rm -rf .gradle

echo ""
echo "5️⃣ Gradle Wrapper neu initialisieren..."
./gradlew --version

echo ""
echo "============================================"
echo "✅ JDK KONFIGURATION ABGESCHLOSSEN!"
echo "============================================"
echo ""
echo "Nächste Schritte in Android Studio:"
echo "1. File → Invalidate Caches / Restart"
echo "2. Warte bis Android Studio neu gestartet ist"
echo "3. File → Sync Project with Gradle Files"
echo "4. ✅ JDK-Fehler sollte weg sein!"
echo ""
