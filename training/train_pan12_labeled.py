#!/usr/bin/env python3
"""
KidGuard Training mit korrekten PAN12 Labels
============================================
Training mit Grooming-Pattern-basierten Labels

Dataset:
- 56,952 Training Conversations (3.6% Grooming)
- 132,247 Test Conversations (3.5% Grooming)
- Ziel: 95-97% Accuracy mit Class Balancing
"""

import os
import json
import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers, models, optimizers, callbacks
from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.utils.class_weight import compute_class_weight
from collections import Counter
from datetime import datetime
import matplotlib.pyplot as plt
import seaborn as sns

print("="*80)
print("🚀 KidGuard Training - Labeled PAN12 Dataset")
print("="*80)
print(f"Start: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

# Configuration
CONFIG = {
    'train_data': 'training/data/pan12_labeled/pan12_train_labeled.json',
    'test_data': 'training/data/pan12_labeled/pan12_test_labeled.json',
    'output_dir': 'training/models/pan12_labeled',
    'vocab_size': 20000,
    'embedding_dim': 256,
    'max_length': 100,
    'lstm_units': 128,
    'dense_units': [256, 128, 64],
    'dropout': 0.5,
    'epochs': 50,
    'batch_size': 64,
    'learning_rate': 0.001,
    'validation_split': 0.15,
}

os.makedirs(CONFIG['output_dir'], exist_ok=True)

# Load Data
print("📂 Loading Data...")
with open(CONFIG['train_data'], 'r') as f:
    train_data = json.load(f)
with open(CONFIG['test_data'], 'r') as f:
    test_data = json.load(f)

X_train = [item['text'] for item in train_data]
y_train = [item['label'] for item in train_data]
X_test = [item['text'] for item in test_data]
y_test = [item['label'] for item in test_data]

print(f"✅ Training: {len(X_train):,} samples")
print(f"✅ Test: {len(X_test):,} samples")

# Label Distribution
print(f"\n📊 Label Distribution:")
train_counts = Counter(y_train)
for label, count in train_counts.most_common():
    pct = count / len(y_train) * 100
    print(f"   {label:15s}: {count:7,} ({pct:5.2f}%)")

# Encode Labels
le = LabelEncoder()
le.fit(y_train + y_test)
y_train_enc = le.transform(y_train)
y_test_enc = le.transform(y_test)
num_classes = len(le.classes_)

print(f"\n✅ Classes: {num_classes}")
print(f"   {list(le.classes_)}")

with open(os.path.join(CONFIG['output_dir'], 'label_mapping.json'), 'w') as f:
    json.dump({i: label for i, label in enumerate(le.classes_)}, f, indent=2)

# Tokenization
print("\n📝 Tokenization...")
tokenizer = Tokenizer(num_words=CONFIG['vocab_size'], oov_token='<OOV>')
tokenizer.fit_on_texts(X_train + X_test)

X_train_seq = tokenizer.texts_to_sequences(X_train)
X_test_seq = tokenizer.texts_to_sequences(X_test)

X_train_pad = pad_sequences(X_train_seq, maxlen=CONFIG['max_length'], padding='post')
X_test_pad = pad_sequences(X_test_seq, maxlen=CONFIG['max_length'], padding='post')

print(f"✅ Vocab: {len(tokenizer.word_index):,}")
print(f"✅ Train: {X_train_pad.shape}")
print(f"✅ Test: {X_test_pad.shape}")

with open(os.path.join(CONFIG['output_dir'], 'tokenizer.json'), 'w') as f:
    f.write(tokenizer.to_json())

# Class Weights (CRITICAL for imbalanced data!)
print("\n⚖️  Computing Class Weights...")
class_weights_array = compute_class_weight(
    'balanced',
    classes=np.unique(y_train_enc),
    y=y_train_enc
)
class_weights = {i: w for i, w in enumerate(class_weights_array)}

print("   Class Weights:")
for i, w in class_weights.items():
    print(f"   {le.classes_[i]:15s}: {w:.4f}")

# Build Model
print("\n🏗️  Building Model...")

input_layer = layers.Input(shape=(CONFIG['max_length'],))
embedding = layers.Embedding(CONFIG['vocab_size'], CONFIG['embedding_dim'], mask_zero=True)(input_layer)
lstm = layers.Bidirectional(layers.LSTM(CONFIG['lstm_units'], return_sequences=True))(embedding)

# Multi-Head Attention - FIX: key_dim muss mit lstm_units*2 kompatibel sein
attention = layers.MultiHeadAttention(num_heads=4, key_dim=CONFIG['lstm_units'])(lstm, lstm)
attention = layers.Add()([lstm, attention])
attention = layers.LayerNormalization()(attention)
pooled = layers.GlobalAveragePooling1D()(attention)

# Dense Layers
x = pooled
for units in CONFIG['dense_units']:
    x = layers.Dense(units, activation='relu')(x)
    x = layers.BatchNormalization()(x)
    x = layers.Dropout(CONFIG['dropout'])(x)

output = layers.Dense(num_classes, activation='softmax')(x)

model = models.Model(inputs=input_layer, outputs=output)

model.compile(
    optimizer=optimizers.Adam(learning_rate=CONFIG['learning_rate']),
    loss='sparse_categorical_crossentropy',
    metrics=['accuracy']
)

model.build(input_shape=(None, CONFIG['max_length']))
print(f"✅ Parameters: {model.count_params():,}")

# Training
print("\n" + "="*80)
print("🚀 TRAINING")
print("="*80)

callbacks_list = [
    callbacks.EarlyStopping(monitor='val_accuracy', patience=10, restore_best_weights=True, mode='max', verbose=1),
    callbacks.ReduceLROnPlateau(monitor='val_loss', factor=0.5, patience=5, verbose=1, min_lr=1e-7),
    callbacks.ModelCheckpoint(
        filepath=os.path.join(CONFIG['output_dir'], 'best_model.keras'),
        monitor='val_accuracy',
        save_best_only=True,
        mode='max',
        verbose=1
    ),
    callbacks.CSVLogger(os.path.join(CONFIG['output_dir'], 'training_history.csv'))
]

history = model.fit(
    X_train_pad, y_train_enc,
    validation_split=CONFIG['validation_split'],
    epochs=CONFIG['epochs'],
    batch_size=CONFIG['batch_size'],
    class_weight=class_weights,
    callbacks=callbacks_list,
    verbose=1
)

# Evaluation
print("\n" + "="*80)
print("📊 EVALUATION")
print("="*80)

y_pred_probs = model.predict(X_test_pad, batch_size=128)
y_pred = np.argmax(y_pred_probs, axis=1)

report = classification_report(y_test_enc, y_pred, target_names=le.classes_, digits=4)
print("\n📋 Classification Report:")
print("-"*80)
print(report)

with open(os.path.join(CONFIG['output_dir'], 'classification_report.txt'), 'w') as f:
    f.write(report)

# Confusion Matrix
cm = confusion_matrix(y_test_enc, y_pred)
plt.figure(figsize=(10, 8))
sns.heatmap(cm, annot=True, fmt='d', cmap='Blues', xticklabels=le.classes_, yticklabels=le.classes_)
plt.title('Confusion Matrix')
plt.ylabel('True')
plt.xlabel('Predicted')
plt.tight_layout()
plt.savefig(os.path.join(CONFIG['output_dir'], 'confusion_matrix.png'), dpi=300)
print("✅ Confusion Matrix saved")

# Training History
plt.figure(figsize=(12, 4))

plt.subplot(1, 2, 1)
plt.plot(history.history['accuracy'], label='Train')
plt.plot(history.history['val_accuracy'], label='Val')
plt.title('Accuracy')
plt.xlabel('Epoch')
plt.ylabel('Accuracy')
plt.legend()
plt.grid(True)

plt.subplot(1, 2, 2)
plt.plot(history.history['loss'], label='Train')
plt.plot(history.history['val_loss'], label='Val')
plt.title('Loss')
plt.xlabel('Epoch')
plt.ylabel('Loss')
plt.legend()
plt.grid(True)

plt.tight_layout()
plt.savefig(os.path.join(CONFIG['output_dir'], 'training_history.png'), dpi=300)
print("✅ Training History saved")

# TFLite Export
print("\n📦 Exporting TFLite...")
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

tflite_path = os.path.join(CONFIG['output_dir'], 'kidguard_labeled.tflite')
with open(tflite_path, 'wb') as f:
    f.write(tflite_model)

print(f"✅ TFLite: {tflite_path} ({len(tflite_model)/1024/1024:.2f} MB)")

# Summary
print("\n" + "="*80)
print("🎉 TRAINING COMPLETE!")
print("="*80)

test_loss, test_acc = model.evaluate(X_test_pad, y_test_enc, verbose=0)

# Parse classification report for Grooming metrics
from sklearn.metrics import precision_recall_fscore_support
precision, recall, f1, support = precision_recall_fscore_support(y_test_enc, y_pred, average=None, labels=[0, 1])

grooming_precision = precision[0]
grooming_recall = recall[0]
grooming_f1 = f1[0]

print(f"\n📊 Final Metrics:")
print(f"   Test Accuracy:       {test_acc:.4f} ({test_acc*100:.2f}%)")
print(f"   Grooming Precision:  {grooming_precision:.4f} ({grooming_precision*100:.2f}%)")
print(f"   Grooming Recall:     {grooming_recall:.4f} ({grooming_recall*100:.2f}%)")
print(f"   Grooming F1-Score:   {grooming_f1:.4f} ({grooming_f1*100:.2f}%)")

print(f"\n📁 Outputs: {CONFIG['output_dir']}")
print(f"⏰ End: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
print("="*80)

if grooming_recall >= 0.90:
    print("\n🎯 EXCELLENT: 90%+ Grooming Recall achieved!")
elif grooming_recall >= 0.85:
    print("\n✅ GOOD: 85%+ Grooming Recall!")
else:
    print(f"\n⚠️  Grooming Recall: {grooming_recall*100:.1f}% - Consider tuning")
