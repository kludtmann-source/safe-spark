# 🧪 Quick Test Commands für KidGuard

## Test 1: "bist du heute alleine?"

### Vorbereitung:
```bash
# 1. Clear Logcat
~/Library/Android/sdk/platform-tools/adb -s 56301FDCR006BT logcat -c

# 2. Starte Monitoring
~/Library/Android/sdk/platform-tools/adb -s 56301FDCR006BT logcat | grep -E "(Assessment-Pattern|RISK DETECTED|Score:|alleine)"
```

### Jetzt in WhatsApp schreiben:
```
bist du heute alleine?
```

### Expected Output:
```
KidGuardEngine: ⚠️  CRITICAL Assessment-Pattern erkannt: 'alleine' → Score: 0.85
GuardianAccessibility: 🚨 RISK DETECTED! (ML-Enhanced)
GuardianAccessibility: ⚠️ Score: 0.85
```

---

## Test 2: Weitere Test-Cases

### Deutsch:
- "bist du allein?"
- "wo sind deine eltern?"
- "bist du in deinem zimmer?"
- "ist niemand da?"

### Englisch:
- "are you alone?"
- "where are your parents?"
- "are you in your room?"
- "is nobody there?"

### Alle sollten Score 0.70-0.85 haben und RISK DETECTED auslösen!

---

## Quick Monitoring Command:
```bash
~/Library/Android/sdk/platform-tools/adb -s 56301FDCR006BT logcat | grep --color=always -E "(Assessment-Pattern|RISK|Score)"
```

---

## Single-Line Test Command:
```bash
~/Library/Android/sdk/platform-tools/adb -s 56301FDCR006BT logcat -c && ~/Library/Android/sdk/platform-tools/adb -s 56301FDCR006BT logcat | grep -E "(Assessment-Pattern|RISK)"
```
