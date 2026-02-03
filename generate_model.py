#!/usr/bin/env python3
"""
KidGuard TensorFlow Lite Modell Generator (vereinfacht)
"""

import numpy as np
import tensorflow as tf
from tensorflow import keras
import os

print("📚 KidGuard TensorFlow Lite Modell Generator")
print("=" * 50)

# Training-Daten
texts = [
    "Hello friend", "playing games", "good morning", "I love you", "have fun",
    "school today", "feeling happy", "family loves", "reading books", "great time",
    "wonderful day", "learning new", "thank you", "help me", "together",

    "abuse harm", "dangerous content", "exploitation", "violence assault",
    "inappropriate illegal", "adult content", "predator grooming", "harassment",
    "bullying", "threat intimidation", "weapon violence", "drug abuse",
    "pornography", "graphic obscene", "hate discrimination"
]

labels = np.array([0]*15 + [1]*15)

print("🔤 Erstelle Tokenizer...")
tokenizer = keras.preprocessing.text.Tokenizer(num_words=500)
tokenizer.fit_on_texts(texts)

print("📝 Konvertiere zu Sequenzen...")
sequences = tokenizer.texts_to_sequences(texts)
padded = keras.preprocessing.sequence.pad_sequences(sequences, maxlen=256, padding='post')

print("🤖 Erstelle Modell...")
model = keras.Sequential([
    keras.layers.Embedding(500, 16, input_length=256),
    keras.layers.GlobalAveragePooling1D(),
    keras.layers.Dense(16, activation='relu'),
    keras.layers.Dense(8, activation='relu'),
    keras.layers.Dense(1, activation='sigmoid')
])

model.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])

print("⚡ Trainiere...")
model.fit(padded, labels, epochs=30, batch_size=4, verbose=0)

print("🔄 Konvertiere zu TFLite...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
tflite_model = converter.convert()

output_path = "app/src/main/assets/kid_guard_v1.tflite"
os.makedirs(os.path.dirname(output_path), exist_ok=True)

with open(output_path, 'wb') as f:
    f.write(tflite_model)

print(f"✅ Modell gespeichert: {output_path}")
print(f"📦 Größe: {len(tflite_model) / 1024:.2f} KB")
