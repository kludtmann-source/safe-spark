#!/bin/bash
echo "🧪 Re-Test: 'bist du heute alleine?' nach Pattern-Fix"
echo "======================================================"
echo ""
echo "✅ App neu installiert mit Assessment Pattern Detection"
echo ""
echo "📱 Starte SafeSpark App..."
adb shell am start -n com.example.safespark/.MainActivity
sleep 2
echo ""
echo "🔍 Logcat gestartet - Schreibe JETZT in WhatsApp:"
echo ""
echo "   📝 'bist du heute alleine?'"
echo ""
echo "Expected Output:"
echo "   ⚠️  CRITICAL Assessment-Pattern erkannt: 'alleine' → Score: 0.85"
echo "   🚨 RISK DETECTED!"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
~/Library/Android/sdk/platform-tools/adb -s 56301FDCR006BT logcat -c
~/Library/Android/sdk/platform-tools/adb -s 56301FDCR006BT logcat | grep --color=always -E "(Assessment-Pattern|RISK DETECTED|Score:|alleine)"
