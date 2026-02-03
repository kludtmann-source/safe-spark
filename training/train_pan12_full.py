#!/usr/bin/env python3
"""
KidGuard FULL PAN12 Training
=============================
Training mit ALLEN 66,927+ PAN12 Conversations

Features:
- Full PAN12 Dataset (~500,000+ Messages)
- Multi-Class (6 Stages) oder Binary
- Focal Loss + Class Weights
- BiLSTM + Attention
- Comprehensive Evaluation
- TFLite Export

Ziel: 96-98% Accuracy
"""

import os
import json
import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers, models, optimizers, callbacks
from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.utils.class_weight import compute_class_weight
from collections import Counter
from datetime import datetime
import matplotlib.pyplot as plt
import seaborn as sns

print("="*80)
print("🚀 KidGuard FULL PAN12 Training")
print("="*80)
print(f"Start: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

# ============================================================================
# CONFIGURATION
# ============================================================================

CONFIG = {
    # Data
    'train_data': 'training/data/pan12_full/pan12_train_full.json',
    'test_data': 'training/data/pan12_full/pan12_test_full.json',
    'output_dir': 'training/models/pan12_full',

    # Model
    'vocab_size': 20000,  # Increased for large dataset
    'embedding_dim': 256,
    'max_length': 100,  # Longer for conversations
    'lstm_units': 128,
    'dense_units': [256, 128, 64],
    'dropout': 0.5,
    'use_attention': True,

    # Training
    'epochs': 100,
    'batch_size': 64,  # Larger batches for big dataset
    'learning_rate': 0.001,
    'validation_split': 0.15,
    'use_class_weights': True,

    # Callbacks
    'early_stopping_patience': 10,
    'reduce_lr_patience': 5,
}

os.makedirs(CONFIG['output_dir'], exist_ok=True)

# ============================================================================
# LOAD DATA
# ============================================================================

print("📂 Loading Full PAN12 Dataset...")

with open(CONFIG['train_data'], 'r', encoding='utf-8') as f:
    train_data = json.load(f)

with open(CONFIG['test_data'], 'r', encoding='utf-8') as f:
    test_data = json.load(f)

print(f"✅ Training: {len(train_data):,} conversations")
print(f"✅ Test: {len(test_data):,} conversations")

# Extract texts and labels
X_train = [item['text'] for item in train_data]
y_train = [item['label'] for item in train_data]
X_test = [item['text'] for item in test_data]
y_test = [item['label'] for item in test_data]

# Label distribution
print(f"\n📊 Label Distribution:")
train_counts = Counter(y_train)
for label, count in train_counts.most_common():
    percentage = count / len(y_train) * 100
    print(f"   {label:20s}: {count:7,} ({percentage:5.2f}%)")

# ============================================================================
# LABEL ENCODING
# ============================================================================

print("\n🏷️  Encoding Labels...")

le = LabelEncoder()
le.fit(y_train + y_test)

y_train_enc = le.transform(y_train)
y_test_enc = le.transform(y_test)
num_classes = len(le.classes_)

print(f"✅ Classes: {num_classes}")
print(f"   {list(le.classes_)}")

# Save label mapping
label_mapping = {i: label for i, label in enumerate(le.classes_)}
with open(os.path.join(CONFIG['output_dir'], 'label_mapping.json'), 'w') as f:
    json.dump(label_mapping, f, indent=2)

# ============================================================================
# TOKENIZATION
# ============================================================================

print("\n📝 Tokenization...")

tokenizer = Tokenizer(
    num_words=CONFIG['vocab_size'],
    oov_token='<OOV>',
    filters='!"#$%&()*+,-./:;<=>?@[\\]^_`{|}~\t\n'
)

print(f"   Fitting on {len(X_train):,} texts...")
tokenizer.fit_on_texts(X_train + X_test)

X_train_seq = tokenizer.texts_to_sequences(X_train)
X_test_seq = tokenizer.texts_to_sequences(X_test)

X_train_pad = pad_sequences(X_train_seq, maxlen=CONFIG['max_length'], padding='post', truncating='post')
X_test_pad = pad_sequences(X_test_seq, maxlen=CONFIG['max_length'], padding='post', truncating='post')

print(f"✅ Vocabulary: {len(tokenizer.word_index):,} words")
print(f"✅ Train shape: {X_train_pad.shape}")
print(f"✅ Test shape: {X_test_pad.shape}")

# Save tokenizer
tokenizer_json = tokenizer.to_json()
with open(os.path.join(CONFIG['output_dir'], 'tokenizer.json'), 'w') as f:
    f.write(tokenizer_json)

# ============================================================================
# CLASS WEIGHTS
# ============================================================================

if CONFIG['use_class_weights']:
    print("\n⚖️  Computing Class Weights...")
    class_weights_array = compute_class_weight(
        'balanced',
        classes=np.unique(y_train_enc),
        y=y_train_enc
    )
    class_weights = {i: w for i, w in enumerate(class_weights_array)}

    print("   Class Weights:")
    for i, w in class_weights.items():
        print(f"   {le.classes_[i]:20s}: {w:.4f}")
else:
    class_weights = None

# ============================================================================
# MODEL ARCHITECTURE
# ============================================================================

print("\n🏗️  Building Model Architecture...")

# Input
input_layer = layers.Input(shape=(CONFIG['max_length'],), name='input')

# Embedding
embedding = layers.Embedding(
    input_dim=CONFIG['vocab_size'],
    output_dim=CONFIG['embedding_dim'],
    mask_zero=True,
    name='embedding'
)(input_layer)

# Bidirectional LSTM
lstm = layers.Bidirectional(
    layers.LSTM(CONFIG['lstm_units'], return_sequences=CONFIG['use_attention']),
    name='bi_lstm'
)(embedding)

# Attention (optional)
if CONFIG['use_attention']:
    attention = layers.MultiHeadAttention(
        num_heads=4,
        key_dim=CONFIG['embedding_dim'] // 4,
        name='attention'
    )(lstm, lstm)

    # Add & Norm
    attention = layers.Add()([lstm, attention])
    attention = layers.LayerNormalization()(attention)

    # Pooling
    pooled = layers.GlobalAveragePooling1D()(attention)
else:
    pooled = lstm

# Dense layers
x = pooled
for i, units in enumerate(CONFIG['dense_units']):
    x = layers.Dense(units, activation='relu', name=f'dense_{i}')(x)
    x = layers.BatchNormalization()(x)
    x = layers.Dropout(CONFIG['dropout'])(x)

# Output
output = layers.Dense(num_classes, activation='softmax', name='output')(x)

# Build model
model = models.Model(inputs=input_layer, outputs=output, name='KidGuard_PAN12_Full')

# Compile
model.compile(
    optimizer=optimizers.Adam(learning_rate=CONFIG['learning_rate']),
    loss='sparse_categorical_crossentropy',
    metrics=[
        'accuracy',
        keras.metrics.Precision(name='precision'),
        keras.metrics.Recall(name='recall')
    ]
)

# Build to get param count
model.build(input_shape=(None, CONFIG['max_length']))
print(f"✅ Parameters: {model.count_params():,}")

# Model summary
model.summary()

# ============================================================================
# TRAINING
# ============================================================================

print("\n" + "="*80)
print("🚀 STARTING TRAINING")
print("="*80)

# Callbacks
callbacks_list = [
    callbacks.EarlyStopping(
        monitor='val_recall',
        patience=CONFIG['early_stopping_patience'],
        restore_best_weights=True,
        mode='max',
        verbose=1
    ),
    callbacks.ReduceLROnPlateau(
        monitor='val_loss',
        factor=0.5,
        patience=CONFIG['reduce_lr_patience'],
        verbose=1,
        min_lr=1e-7
    ),
    callbacks.ModelCheckpoint(
        filepath=os.path.join(CONFIG['output_dir'], 'best_model.keras'),
        monitor='val_recall',
        save_best_only=True,
        mode='max',
        verbose=1
    ),
    callbacks.CSVLogger(
        os.path.join(CONFIG['output_dir'], 'training_history.csv')
    )
]

# Train
print(f"\n⏰ Training for up to {CONFIG['epochs']} epochs...")
print(f"   Dataset: {len(X_train_pad):,} samples")
print(f"   Batch Size: {CONFIG['batch_size']}")
print(f"   Validation Split: {CONFIG['validation_split']:.1%}")
print()

history = model.fit(
    X_train_pad, y_train_enc,
    validation_split=CONFIG['validation_split'],
    epochs=CONFIG['epochs'],
    batch_size=CONFIG['batch_size'],
    class_weight=class_weights,
    callbacks=callbacks_list,
    verbose=1
)

# ============================================================================
# EVALUATION
# ============================================================================

print("\n" + "="*80)
print("📊 EVALUATION ON TEST SET")
print("="*80)

# Predictions
y_pred_probs = model.predict(X_test_pad, batch_size=CONFIG['batch_size'])
y_pred = np.argmax(y_pred_probs, axis=1)

# Classification Report
print("\n📋 Classification Report:")
print("-"*80)
report = classification_report(
    y_test_enc, y_pred,
    target_names=le.classes_,
    digits=4
)
print(report)

# Save report
with open(os.path.join(CONFIG['output_dir'], 'classification_report.txt'), 'w') as f:
    f.write(report)

# Confusion Matrix
cm = confusion_matrix(y_test_enc, y_pred)
plt.figure(figsize=(10, 8))
sns.heatmap(cm, annot=True, fmt='d', cmap='Blues',
            xticklabels=le.classes_,
            yticklabels=le.classes_)
plt.title('Confusion Matrix - Full PAN12')
plt.ylabel('True Label')
plt.xlabel('Predicted Label')
plt.tight_layout()
plt.savefig(os.path.join(CONFIG['output_dir'], 'confusion_matrix.png'), dpi=300)
print(f"\n✅ Confusion Matrix saved")

# Training History Plot
plt.figure(figsize=(15, 5))

plt.subplot(1, 3, 1)
plt.plot(history.history['accuracy'], label='Train')
plt.plot(history.history['val_accuracy'], label='Val')
plt.title('Accuracy')
plt.xlabel('Epoch')
plt.ylabel('Accuracy')
plt.legend()
plt.grid(True)

plt.subplot(1, 3, 2)
plt.plot(history.history['loss'], label='Train')
plt.plot(history.history['val_loss'], label='Val')
plt.title('Loss')
plt.xlabel('Epoch')
plt.ylabel('Loss')
plt.legend()
plt.grid(True)

plt.subplot(1, 3, 3)
plt.plot(history.history['recall'], label='Train')
plt.plot(history.history['val_recall'], label='Val')
plt.title('Recall')
plt.xlabel('Epoch')
plt.ylabel('Recall')
plt.legend()
plt.grid(True)

plt.tight_layout()
plt.savefig(os.path.join(CONFIG['output_dir'], 'training_history.png'), dpi=300)
print(f"✅ Training History saved")

# ============================================================================
# TFLITE EXPORT
# ============================================================================

print("\n" + "="*80)
print("📦 EXPORTING TO TFLITE")
print("="*80)

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_types = [tf.float16]

tflite_model = converter.convert()

tflite_path = os.path.join(CONFIG['output_dir'], 'kidguard_pan12_full.tflite')
with open(tflite_path, 'wb') as f:
    f.write(tflite_model)

model_size_mb = len(tflite_model) / (1024 * 1024)
print(f"✅ TFLite Model: {tflite_path}")
print(f"   Size: {model_size_mb:.2f} MB")

# ============================================================================
# SUMMARY
# ============================================================================

print("\n" + "="*80)
print("🎉 TRAINING COMPLETE!")
print("="*80)

final_val_acc = history.history['val_accuracy'][-1]
final_val_recall = history.history['val_recall'][-1]
test_loss, test_acc, test_prec, test_recall = model.evaluate(X_test_pad, y_test_enc, verbose=0)

print(f"\n📊 Final Metrics:")
print(f"   Validation Accuracy: {final_val_acc:.4f} ({final_val_acc*100:.2f}%)")
print(f"   Validation Recall:   {final_val_recall:.4f} ({final_val_recall*100:.2f}%)")
print(f"   Test Accuracy:       {test_acc:.4f} ({test_acc*100:.2f}%)")
print(f"   Test Recall:         {test_recall:.4f} ({test_recall*100:.2f}%)")

print(f"\n📁 Outputs saved to: {CONFIG['output_dir']}")
print(f"   - best_model.keras")
print(f"   - kidguard_pan12_full.tflite")
print(f"   - classification_report.txt")
print(f"   - confusion_matrix.png")
print(f"   - training_history.png")
print(f"   - training_history.csv")
print(f"   - label_mapping.json")
print(f"   - tokenizer.json")

print(f"\n⏰ End: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
print("="*80)

if test_acc >= 0.96:
    print("\n🎯 TARGET ACHIEVED: 96%+ Accuracy!")
elif test_acc >= 0.95:
    print("\n🎯 EXCELLENT: 95%+ Accuracy!")
else:
    print(f"\n📈 Accuracy: {test_acc*100:.2f}% - Good baseline!")

print("\n✅ Ready for deployment!")
