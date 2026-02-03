#!/usr/bin/env python3
"""Simpler TFLite Export"""
import tensorflow as tf

model = tf.keras.models.load_model('training/models/pan12_fixed/best_model.keras')
print("Model loaded")

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS,
    tf.lite.OpsSet.SELECT_TF_OPS
]
converter._experimental_lower_tensor_list_ops = False

print("Converting...")
tflite_model = converter.convert()

output_path = 'training/models/pan12_fixed/kidguard_model.tflite'
with open(output_path, 'wb') as f:
    f.write(tflite_model)

print(f"✅ Saved: {output_path} ({len(tflite_model)/1024/1024:.2f} MB)")
