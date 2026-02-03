#!/usr/bin/env python3
"""
TFLite Export mit Workaround für BiLSTM LLVM Error
===================================================
Lösung für: "LLVM ERROR: Failed to infer result type(s)"
"""

import os
import tensorflow as tf
from tensorflow import keras

print("="*70)
print("📦 TFLite Export mit BiLSTM Workaround")
print("="*70)

MODEL_PATH = "training/models/pan12_fixed/best_model.keras"
OUTPUT_PATH = "training/models/pan12_fixed/kidguard_model.tflite"

# Check if model exists
if not os.path.exists(MODEL_PATH):
    print(f"❌ Model not found: {MODEL_PATH}")
    exit(1)

print(f"\n📂 Loading model from: {MODEL_PATH}")
model = keras.models.load_model(MODEL_PATH)
print("✅ Model loaded")

print("\n📦 Converting to TFLite...")
print("   Using SELECT_TF_OPS workaround for BiLSTM...")

converter = tf.lite.TFLiteConverter.from_keras_model(model)

# WICHTIG: Diese Flags lösen das LLVM Problem
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS,  # Standard TFLite Ops
    tf.lite.OpsSet.SELECT_TF_OPS      # TensorFlow Ops als Fallback (für LSTM)
]

# Verhindert aggressive Optimierung die LSTM bricht
converter._experimental_lower_tensor_list_ops = False

# Optional: Optimization für kleinere Datei
# converter.optimizations = [tf.lite.Optimize.DEFAULT]

try:
    tflite_model = converter.convert()

    with open(OUTPUT_PATH, "wb") as f:
        f.write(tflite_model)

    model_size_mb = len(tflite_model) / (1024 * 1024)

    print("\n" + "="*70)
    print("✅ TFLITE EXPORT ERFOLGREICH!")
    print("="*70)
    print(f"📦 Saved to: {OUTPUT_PATH}")
    print(f"📏 Size: {model_size_mb:.2f} MB")
    print(f"⚙️  Contains SELECT_TF_OPS (BiLSTM support)")

    print("\n⚠️  WICHTIG:")
    print("   Das Model nutzt SELECT_TF_OPS für BiLSTM.")
    print("   In Android benötigst du: implementation 'org.tensorflow:tensorflow-lite-select-tf-ops'")
    print("="*70)

except Exception as e:
    print(f"\n❌ FEHLER beim Export: {e}")
    print("\nFallback: Versuche mit standard Converter...")

    # Fallback ohne Optimierung
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,
        tf.lite.OpsSet.SELECT_TF_OPS
    ]

    tflite_model = converter.convert()

    with open(OUTPUT_PATH, "wb") as f:
        f.write(tflite_model)

    print(f"✅ Fallback erfolgreich: {OUTPUT_PATH}")
