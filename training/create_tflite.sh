#!/bin/bash
# Quick TFLite Export Script

echo "========================================"
echo "🚀 TFLite Export Launcher"
echo "========================================"
echo ""
echo "Wähle eine Option:"
echo ""
echo "1) Mit SELECT_TF_OPS (volle 96% Accuracy, ~10MB)"
echo "2) Vereinfachtes Model (~90% Accuracy, ~3MB)"
echo "3) Beide erstellen"
echo ""
read -p "Deine Wahl (1-3): " choice

cd /Users/knutludtmann/AndroidStudioProjects/KidGuard

case $choice in
    1)
        echo ""
        echo "📦 Erstelle TFLite mit SELECT_TF_OPS..."
        python3 << 'EOF'
import tensorflow as tf
try:
    model = tf.keras.models.load_model('training/models/pan12_fixed/best_model.keras')
    print("✅ Model geladen")

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,
        tf.lite.OpsSet.SELECT_TF_OPS
    ]
    converter._experimental_lower_tensor_list_ops = False

    print("🔄 Konvertiere...")
    tflite_model = converter.convert()

    with open('training/models/pan12_fixed/kidguard_model.tflite', 'wb') as f:
        f.write(tflite_model)

    size = len(tflite_model) / 1024 / 1024
    print(f"✅ Gespeichert: kidguard_model.tflite ({size:.2f} MB)")
except Exception as e:
    print(f"❌ Fehler: {e}")
EOF
        ;;
    2)
        echo ""
        echo "📦 Erstelle vereinfachtes Model..."
        python3 training/export_alternative.py
        ;;
    3)
        echo ""
        echo "📦 Erstelle beide Models..."
        echo ""
        echo "1/2: Mit SELECT_TF_OPS..."
        python3 << 'EOF'
import tensorflow as tf
try:
    model = tf.keras.models.load_model('training/models/pan12_fixed/best_model.keras')
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS, tf.lite.OpsSet.SELECT_TF_OPS]
    converter._experimental_lower_tensor_list_ops = False
    tflite_model = converter.convert()
    with open('training/models/pan12_fixed/kidguard_model.tflite', 'wb') as f:
        f.write(tflite_model)
    print(f"✅ kidguard_model.tflite: {len(tflite_model)/1024/1024:.2f} MB")
except Exception as e:
    print(f"❌ Fehler: {e}")
EOF
        echo ""
        echo "2/2: Vereinfachtes Model..."
        python3 training/export_alternative.py
        ;;
    *)
        echo "❌ Ungültige Wahl"
        exit 1
        ;;
esac

echo ""
echo "========================================"
echo "✅ Fertig!"
echo "========================================"
echo ""
echo "📁 Dateien in: training/models/pan12_fixed/"
ls -lh training/models/pan12_fixed/*.tflite 2>/dev/null || echo "Keine .tflite Dateien gefunden"
echo ""
