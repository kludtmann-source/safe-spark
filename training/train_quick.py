#!/usr/bin/env python3
"""Quick Training Script - Simplified Version"""

import json
import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
from sklearn.preprocessing import LabelEncoder
from collections import Counter

print("="*80)
print("🚀 KidGuard Quick Training")
print("="*80)

# Load data
print("\n📂 Loading data...")
with open('training/data/combined/kidguard_german_train.json', 'r') as f:
    train_data = json.load(f)

with open('training/data/combined/kidguard_german_test.json', 'r') as f:
    test_data = json.load(f)

X_train = [item['text'] for item in train_data]
y_train = [item['label'] for item in train_data]
X_test = [item['text'] for item in test_data]
y_test = [item['label'] for item in test_data]

print(f"✅ Train: {len(X_train)}, Test: {len(X_test)}")

# Label distribution
print("\n📊 Label Distribution:")
for label, count in Counter(y_train).most_common():
    print(f"  {label}: {count}")

# Encode labels
le = LabelEncoder()
le.fit(y_train + y_test)
y_train_enc = le.transform(y_train)
y_test_enc = le.transform(y_test)
num_classes = len(le.classes_)

print(f"\n✅ Classes: {num_classes}")
print(f"  {list(le.classes_)}")

# Tokenize
from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences

tokenizer = Tokenizer(num_words=5000, oov_token='<OOV>')
tokenizer.fit_on_texts(X_train + X_test)

X_train_seq = pad_sequences(tokenizer.texts_to_sequences(X_train), maxlen=50)
X_test_seq = pad_sequences(tokenizer.texts_to_sequences(X_test), maxlen=50)

print(f"\n✅ Shapes: Train={X_train_seq.shape}, Test={X_test_seq.shape}")

# Build simple model
print("\n🏗️  Building model...")
model = keras.Sequential([
    layers.Embedding(5000, 128, input_length=50),
    layers.Bidirectional(layers.LSTM(64)),
    layers.Dense(128, activation='relu'),
    layers.Dropout(0.5),
    layers.Dense(num_classes, activation='softmax')
])

model.compile(
    optimizer='adam',
    loss='sparse_categorical_crossentropy',
    metrics=['accuracy']
)

# Build model first
model.build(input_shape=(None, 50))
print(f"✅ Parameters: {model.count_params():,}")

# Train
print("\n🚀 Training...")
history = model.fit(
    X_train_seq, y_train_enc,
    validation_data=(X_test_seq, y_test_enc),
    epochs=50,
    batch_size=32,
    callbacks=[
        keras.callbacks.EarlyStopping(patience=5, restore_best_weights=True)
    ],
    verbose=1
)

# Evaluate
print("\n📊 Evaluation...")
loss, acc = model.evaluate(X_test_seq, y_test_enc)
print(f"\n✅ Test Accuracy: {acc:.4f} ({acc*100:.2f}%)")

# Save
print("\n💾 Saving model...")
model.save('training/models/quick_model.keras')
print("✅ Done!")
