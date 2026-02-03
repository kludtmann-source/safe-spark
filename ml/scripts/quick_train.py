#!/usr/bin/env python3
"""
Quick Training Script für Phase 3 - Mit sofortigem Output
"""

import json
import sys

print("=" * 60)
print("🛡️  KID-GUARD Phase 3 - Quick Training")
print("=" * 60)
print()

# 1. Check Dataset
print("1️⃣  Lade Dataset...")
import os
script_dir = os.path.dirname(os.path.abspath(__file__))
data_path = os.path.join(script_dir, '..', 'data', 'grooming_stages_dataset.json')

try:
    with open(data_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    print(f"✅ {len(data)} Beispiele geladen")
    print(f"✅ Dataset-Pfad: {data_path}")
except Exception as e:
    print(f"❌ Fehler beim Laden: {e}")
    print(f"❌ Versuchter Pfad: {data_path}")
    sys.exit(1)

# 2. Import TensorFlow (dauert etwas)
print()
print("2️⃣  Importiere TensorFlow (kann 10-20 Sekunden dauern)...")
try:
    import tensorflow as tf
    from tensorflow import keras
    import numpy as np
    from sklearn.model_selection import train_test_split
    from sklearn.preprocessing import LabelEncoder
    print(f"✅ TensorFlow {tf.__version__} geladen")
except Exception as e:
    print(f"❌ Fehler beim Import: {e}")
    sys.exit(1)

# 3. Preprocessing
print()
print("3️⃣  Preprocessing...")
texts = [item['text'] for item in data]
labels = [item['label'] for item in data]

tokenizer = keras.preprocessing.text.Tokenizer(num_words=500, oov_token='<OOV>')
tokenizer.fit_on_texts(texts)
sequences = tokenizer.texts_to_sequences(texts)
padded = keras.preprocessing.sequence.pad_sequences(sequences, maxlen=30, padding='post')

label_encoder = LabelEncoder()
encoded_labels = label_encoder.fit_transform(labels)

print(f"✅ Vocabulary: {len(tokenizer.word_index)} Wörter")
print(f"✅ Klassen: {list(label_encoder.classes_)}")

# 4. Train/Test Split
X_train, X_test, y_train, y_test = train_test_split(
    padded, encoded_labels, test_size=0.2, random_state=42
)

print(f"✅ Training: {len(X_train)} Samples")
print(f"✅ Test: {len(X_test)} Samples")

# 5. Model bauen (TFLite-kompatibel)
print()
print("4️⃣  Baue Modell (TFLite-optimiert)...")
model = keras.Sequential([
    keras.layers.Embedding(500, 32, input_length=30),
    keras.layers.Conv1D(64, 3, activation='relu'),
    keras.layers.GlobalMaxPooling1D(),
    keras.layers.Dense(32, activation='relu'),
    keras.layers.Dropout(0.3),
    keras.layers.Dense(16, activation='relu'),
    keras.layers.Dense(len(label_encoder.classes_), activation='softmax')
])

model.compile(
    optimizer='adam',
    loss='sparse_categorical_crossentropy',
    metrics=['accuracy']
)

print("✅ Modell erstellt (Conv1D statt LSTM für TFLite)")

# 6. Training
print()
print("5️⃣  Starte Training (20 Epochen)...")
print("-" * 60)

history = model.fit(
    X_train, y_train,
    epochs=20,
    validation_split=0.2,
    verbose=1,
    callbacks=[
        keras.callbacks.EarlyStopping(monitor='val_loss', patience=3, restore_best_weights=True)
    ]
)

print("-" * 60)
print("✅ Training abgeschlossen!")

# 7. Evaluation
print()
print("6️⃣  Evaluation...")
loss, accuracy = model.evaluate(X_test, y_test, verbose=0)
print(f"✅ Test Accuracy: {accuracy*100:.1f}%")
print(f"✅ Test Loss: {loss:.3f}")

# 8. TFLite Konvertierung
print()
print("7️⃣  Konvertiere zu TFLite...")
models_dir = os.path.join(script_dir, '..', 'models')
os.makedirs(models_dir, exist_ok=True)

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]

# TFLite-kompatible Operationen
converter.target_spec.supported_ops = [
    tf.lite.OpsSet.TFLITE_BUILTINS,
    tf.lite.OpsSet.SELECT_TF_OPS
]
converter._experimental_lower_tensor_list_ops = False

try:
    tflite_model = converter.convert()
except Exception as e:
    print(f"⚠️  Fehler bei Optimierung: {e}")
    print("🔄 Versuche ohne Optimierung...")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,
        tf.lite.OpsSet.SELECT_TF_OPS
    ]
    tflite_model = converter.convert()

model_path = os.path.join(models_dir, 'grooming_detector.tflite')
with open(model_path, 'wb') as f:
    f.write(tflite_model)

size_mb = len(tflite_model) / (1024 * 1024)
print(f"✅ Modell gespeichert: {model_path}")
print(f"✅ Größe: {size_mb:.2f} MB")

if size_mb > 5:
    print("⚠️  Warnung: Modell > 5MB")
else:
    print("✅ Modell < 5MB - Ready for Android!")

# 9. Metadata speichern
print()
print("8️⃣  Speichere Metadata...")
metadata = {
    'labels': list(label_encoder.classes_),
    'vocab_size': 500,
    'max_length': 30,
    'word_index': tokenizer.word_index
}

metadata_path = os.path.join(models_dir, 'grooming_detector_metadata.json')
with open(metadata_path, 'w', encoding='utf-8') as f:
    json.dump(metadata, f, ensure_ascii=False, indent=2)

print(f"✅ Metadata gespeichert: {metadata_path}")

# 10. Test Predictions
print()
print("9️⃣  Test-Predictions:")
print("-" * 60)
test_texts = [
    "hast du die hausaufgaben gemacht?",
    "brauchst du robux?",
    "bist du grad allein?"
]

for text in test_texts:
    seq = tokenizer.texts_to_sequences([text])
    pad = keras.preprocessing.sequence.pad_sequences(seq, maxlen=30, padding='post')
    pred = model.predict(pad, verbose=0)
    predicted_class = label_encoder.classes_[np.argmax(pred[0])]
    confidence = np.max(pred[0]) * 100

    print(f"📝 '{text}'")
    print(f"   → {predicted_class} ({confidence:.1f}%)")
    print()

print("=" * 60)
print("✅ TRAINING ERFOLGREICH ABGESCHLOSSEN!")
print("=" * 60)
print()
print("📦 Output-Dateien:")
print(f"   - {model_path}")
print(f"   - {metadata_path}")
print()
print("🚀 Nächste Schritte:")
print("   1. Kopiere Model in Android App:")
print(f"      cp {model_path} ../app/src/main/assets/")
print("   2. Teste auf Pixel 10")
