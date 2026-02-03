#!/bin/bash
echo "🧪 Quick ML Test nach FLOAT32-Fix"
echo "=================================="
echo ""
echo "1️⃣  Starte App..."
~/Library/Android/sdk/platform-tools/adb -s 56301FDCR006BT shell am start -n com.example.kidguard/.MainActivity
sleep 2
echo ""
echo "2️⃣  Logcat gestartet - Schreibe jetzt in WhatsApp:"
echo "   • 'bist du grad allein?' → sollte RISK DETECTED auslösen"
echo ""
~/Library/Android/sdk/platform-tools/adb -s 56301FDCR006BT logcat -c
~/Library/Android/sdk/platform-tools/adb -s 56301FDCR006BT logcat | grep --color=always -E "(MLGroomingDetector|RISK|Prediction|Score)"
