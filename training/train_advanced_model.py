#!/usr/bin/env python3
"""
KidGuard Advanced Model Training
Trainiert verbessertes Modell mit 937 Samples für komplexe Grooming-Erkennung
"""

import json
import numpy as np
import tensorflow as tf
from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Embedding, LSTM, Bidirectional, Dense, Dropout, GlobalAveragePooling1D
from tensorflow.keras.callbacks import EarlyStopping, ReduceLROnPlateau
from tensorflow.keras.optimizers import Adam
from sklearn.utils.class_weight import compute_class_weight
from collections import Counter
import os

print("🚀 KidGuard Advanced Model Training")
print("=" * 60)

# Konfiguration
CONFIG = {
    'vocab_size': 5000,
    'embedding_dim': 128,
    'max_length': 50,
    'epochs': 100,
    'batch_size': 32,
    'learning_rate': 0.001,
    'dropout': 0.5
}

# Lade deutsches Dataset
print("\n📂 Lade Dataset...")
with open('data/combined/kidguard_german_train.json', 'r', encoding='utf-8') as f:
    train_data = json.load(f)

with open('data/combined/kidguard_test.json', 'r', encoding='utf-8') as f:
    test_data = json.load(f)

print(f"✅ Training Samples: {len(train_data)}")
print(f"✅ Test Samples: {len(test_data)}")

# Extrahiere Texte und Labels
texts = [item['text'] for item in train_data]
labels = [item['label'] for item in train_data]

test_texts = [item['text'] for item in test_data]
test_labels = [item['label'] for item in test_data]

# Label Distribution
label_counts = Counter(labels)
print(f"\n📊 Label Distribution:")
for label, count in label_counts.items():
    print(f"   {label}: {count} ({count/len(labels)*100:.1f}%)")

# Class Weights für unbalanciertes Dataset
unique_labels = sorted(list(set(labels)))
class_weights = compute_class_weight('balanced', classes=np.array(unique_labels), y=np.array(labels))
class_weight_dict = {i: weight for i, weight in enumerate(class_weights)}

print(f"\n⚖️ Class Weights: {class_weight_dict}")

# Tokenizer
print(f"\n🔤 Erstelle Tokenizer (Vocab: {CONFIG['vocab_size']})...")
tokenizer = Tokenizer(
    num_words=CONFIG['vocab_size'],
    oov_token='<OOV>',
    filters='!"#$%&()*+,-./:;<=>?@[\\]^_`{|}~\t\n',
    lower=True
)
tokenizer.fit_on_texts(texts)

# Sequences
train_sequences = tokenizer.texts_to_sequences(texts)
train_padded = pad_sequences(train_sequences, maxlen=CONFIG['max_length'], padding='post', truncating='post')

test_sequences = tokenizer.texts_to_sequences(test_texts)
test_padded = pad_sequences(test_sequences, maxlen=CONFIG['max_length'], padding='post', truncating='post')

# Convert labels to numpy
y_train = np.array(labels)
y_test = np.array(test_labels)

print(f"✅ Train Shape: {train_padded.shape}")
print(f"✅ Test Shape: {test_padded.shape}")

# Advanced Model Architecture
print(f"\n🏗️ Baue Advanced Model...")
model = Sequential([
    # Embedding Layer
    Embedding(
        input_dim=CONFIG['vocab_size'],
        output_dim=CONFIG['embedding_dim'],
        input_length=CONFIG['max_length']
    ),

    # Bidirectional LSTM für besseres Kontext-Verständnis
    Bidirectional(LSTM(64, return_sequences=True)),
    Dropout(CONFIG['dropout']),

    # Weitere LSTM-Schicht
    Bidirectional(LSTM(32)),
    Dropout(CONFIG['dropout']),

    # Dense Layers
    Dense(64, activation='relu'),
    Dropout(0.3),
    Dense(32, activation='relu'),

    # Output (Binary)
    Dense(1, activation='sigmoid')
])

# Compile mit Custom Optimizer
optimizer = Adam(learning_rate=CONFIG['learning_rate'])
model.compile(
    optimizer=optimizer,
    loss='binary_crossentropy',
    metrics=[
        'accuracy',
        tf.keras.metrics.Precision(name='precision'),
        tf.keras.metrics.Recall(name='recall'),
        tf.keras.metrics.AUC(name='auc')
    ]
)

model.summary()

# Callbacks
callbacks = [
    EarlyStopping(
        monitor='val_recall',
        patience=15,
        restore_best_weights=True,
        mode='max',
        verbose=1
    ),
    ReduceLROnPlateau(
        monitor='val_loss',
        factor=0.5,
        patience=5,
        min_lr=0.00001,
        verbose=1
    )
]

# Training
print(f"\n🎯 Starte Training ({CONFIG['epochs']} Epochs)...")
print("=" * 60)

history = model.fit(
    train_padded,
    y_train,
    batch_size=CONFIG['batch_size'],
    epochs=CONFIG['epochs'],
    validation_data=(test_padded, y_test),
    class_weight=class_weight_dict,
    callbacks=callbacks,
    verbose=1
)

# Evaluation
print("\n📊 Final Evaluation:")
results = model.evaluate(test_padded, y_test, verbose=0)
print(f"✅ Test Loss: {results[0]:.4f}")
print(f"✅ Test Accuracy: {results[1]:.4f}")
print(f"✅ Test Precision: {results[2]:.4f}")
print(f"✅ Test Recall: {results[3]:.4f}")
print(f"✅ Test AUC: {results[4]:.4f}")

# Export TFLite
print("\n💾 Exportiere TFLite Modell...")

# Converter mit Optimierungen
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_types = [tf.float32]

tflite_model = converter.convert()

# Speichern
output_path = '../app/src/main/assets/grooming_detector_advanced.tflite'
with open(output_path, 'wb') as f:
    f.write(tflite_model)

print(f"✅ TFLite Modell gespeichert: {output_path}")
print(f"   Größe: {len(tflite_model) / 1024:.1f} KB")

# Metadata
metadata = {
    'vocab_size': CONFIG['vocab_size'],
    'max_length': CONFIG['max_length'],
    'embedding_dim': CONFIG['embedding_dim'],
    'word_index': {word: idx for word, idx in tokenizer.word_index.items() if idx < CONFIG['vocab_size']},
    'classes': ['SAFE', 'GROOMING'],
    'threshold': 0.7,
    'training_samples': len(train_data),
    'test_accuracy': float(results[1]),
    'test_recall': float(results[3]),
    'model_type': 'BiLSTM-Advanced',
    'trained_on': '28.01.2026'
}

metadata_path = '../app/src/main/assets/grooming_detector_advanced_metadata.json'
with open(metadata_path, 'w', encoding='utf-8') as f:
    json.dump(metadata, f, indent=2, ensure_ascii=False)

print(f"✅ Metadata gespeichert: {metadata_path}")

print("\n" + "=" * 60)
print("🎉 TRAINING ABGESCHLOSSEN!")
print("=" * 60)
print(f"\n📋 Zusammenfassung:")
print(f"   Modell: BiLSTM (Bidirectional)")
print(f"   Training Samples: {len(train_data)}")
print(f"   Test Accuracy: {results[1]*100:.1f}%")
print(f"   Test Recall: {results[3]*100:.1f}%")
print(f"   TFLite Größe: {len(tflite_model) / 1024:.1f} KB")
print(f"\n🚀 Nächste Schritte:")
print(f"   1. Update MLGroomingDetector.kt:")
print(f"      MODEL_FILE = 'grooming_detector_advanced.tflite'")
print(f"   2. Rebuild App")
print(f"   3. Teste auf Pixel 10!")
