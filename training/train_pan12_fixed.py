#!/usr/bin/env python3
"""
KidGuard Training - FIXED VERSION
==================================
Vereinfachte, stabile Architektur ohne Attention-Bugs

Dataset: 56,952 Train / 132,247 Test (3.6% Grooming)
Ziel: 90-95% Accuracy, 85%+ Grooming Recall
"""

import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

import json
import numpy as np
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences
from sklearn.preprocessing import LabelEncoder
from sklearn.utils.class_weight import compute_class_weight
from sklearn.metrics import classification_report, confusion_matrix
from collections import Counter
from datetime import datetime
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

print("="*70)
print("🚀 KidGuard Training - FIXED VERSION")
print("="*70)
print(f"Start: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

# Configuration
CONFIG = {
    'train_data': 'training/data/pan12_labeled/pan12_train_labeled.json',
    'test_data': 'training/data/pan12_labeled/pan12_test_labeled.json',
    'output_dir': 'training/models/pan12_fixed',
    'vocab_size': 20000,
    'embedding_dim': 128,
    'max_length': 100,
    'lstm_units': 64,
    'epochs': 30,
    'batch_size': 64,
    'learning_rate': 0.001,
}

os.makedirs(CONFIG['output_dir'], exist_ok=True)

# ============================================================================
# LOAD DATA
# ============================================================================
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

# ============================================================================
# ENCODE LABELS
# ============================================================================
print("\n🏷️  Encoding Labels...")

le = LabelEncoder()
le.fit(y_train + y_test)
y_train_enc = le.transform(y_train)
y_test_enc = le.transform(y_test)
num_classes = len(le.classes_)

print(f"✅ Classes: {num_classes} - {list(le.classes_)}")

# Save label mapping
label_mapping = {int(i): str(label) for i, label in enumerate(le.classes_)}
with open(os.path.join(CONFIG['output_dir'], 'label_mapping.json'), 'w') as f:
    json.dump(label_mapping, f, indent=2)

# ============================================================================
# TOKENIZATION
# ============================================================================
print("\n📝 Tokenization...")

tokenizer = Tokenizer(num_words=CONFIG['vocab_size'], oov_token='<OOV>')
tokenizer.fit_on_texts(X_train)

X_train_seq = tokenizer.texts_to_sequences(X_train)
X_test_seq = tokenizer.texts_to_sequences(X_test)

X_train_pad = pad_sequences(X_train_seq, maxlen=CONFIG['max_length'], padding='post')
X_test_pad = pad_sequences(X_test_seq, maxlen=CONFIG['max_length'], padding='post')

print(f"✅ Vocab: {min(len(tokenizer.word_index), CONFIG['vocab_size']):,}")
print(f"✅ Train shape: {X_train_pad.shape}")
print(f"✅ Test shape: {X_test_pad.shape}")

# Save tokenizer config
tokenizer_config = {
    'vocab_size': CONFIG['vocab_size'],
    'max_length': CONFIG['max_length'],
    'oov_token': '<OOV>'
}
with open(os.path.join(CONFIG['output_dir'], 'tokenizer_config.json'), 'w') as f:
    json.dump(tokenizer_config, f, indent=2)

# ============================================================================
# CLASS WEIGHTS
# ============================================================================
print("\n⚖️  Computing Class Weights...")

class_weights_array = compute_class_weight(
    'balanced',
    classes=np.unique(y_train_enc),
    y=y_train_enc
)
class_weights = {i: float(w) for i, w in enumerate(class_weights_array)}

for i, w in class_weights.items():
    print(f"   {le.classes_[i]:15s}: {w:.4f}")

# ============================================================================
# BUILD MODEL (SIMPLE & STABLE)
# ============================================================================
print("\n🏗️  Building Model (Simple BiLSTM)...")

model = keras.Sequential([
    layers.Embedding(
        input_dim=CONFIG['vocab_size'],
        output_dim=CONFIG['embedding_dim'],
        input_length=CONFIG['max_length']
    ),
    layers.Bidirectional(layers.LSTM(CONFIG['lstm_units'], return_sequences=True)),
    layers.Bidirectional(layers.LSTM(CONFIG['lstm_units'] // 2)),
    layers.Dense(128, activation='relu'),
    layers.Dropout(0.5),
    layers.Dense(64, activation='relu'),
    layers.Dropout(0.3),
    layers.Dense(num_classes, activation='softmax')
])

model.compile(
    optimizer=keras.optimizers.Adam(learning_rate=CONFIG['learning_rate']),
    loss='sparse_categorical_crossentropy',
    metrics=['accuracy']
)

# Build model explicitly
model.build(input_shape=(None, CONFIG['max_length']))
model.summary()
print(f"\n✅ Total Parameters: {model.count_params():,}")

# ============================================================================
# TRAINING
# ============================================================================
print("\n" + "="*70)
print("🚀 TRAINING")
print("="*70)

# Custom Callback für aussagekräftige Logs
class DetailedLoggingCallback(keras.callbacks.Callback):
    def on_epoch_begin(self, epoch, logs=None):
        print(f"\n{'='*70}")
        print(f"📍 EPOCH {epoch+1}/{CONFIG['epochs']} STARTED - {datetime.now().strftime('%H:%M:%S')}")
        print(f"{'='*70}")

    def on_epoch_end(self, epoch, logs=None):
        print(f"\n{'─'*70}")
        print(f"✅ EPOCH {epoch+1} COMPLETE - {datetime.now().strftime('%H:%M:%S')}")
        print(f"   📊 Train Accuracy: {logs.get('accuracy', 0):.4f} ({logs.get('accuracy', 0)*100:.2f}%)")
        print(f"   📊 Val Accuracy:   {logs.get('val_accuracy', 0):.4f} ({logs.get('val_accuracy', 0)*100:.2f}%)")
        print(f"   📉 Train Loss:     {logs.get('loss', 0):.4f}")
        print(f"   📉 Val Loss:       {logs.get('val_loss', 0):.4f}")
        print(f"{'─'*70}")

        # Progress estimate
        remaining = CONFIG['epochs'] - (epoch + 1)
        print(f"   ⏳ Remaining: {remaining} epochs")

callbacks = [
    DetailedLoggingCallback(),
    keras.callbacks.EarlyStopping(
        monitor='val_accuracy',
        patience=5,
        restore_best_weights=True,
        verbose=1
    ),
    keras.callbacks.ModelCheckpoint(
        filepath=os.path.join(CONFIG['output_dir'], 'best_model.keras'),
        monitor='val_accuracy',
        save_best_only=True,
        verbose=1
    ),
    keras.callbacks.ReduceLROnPlateau(
        monitor='val_loss',
        factor=0.5,
        patience=3,
        verbose=1
    ),
    keras.callbacks.CSVLogger(
        os.path.join(CONFIG['output_dir'], 'training_history.csv')
    )
]

history = model.fit(
    X_train_pad, y_train_enc,
    validation_split=0.15,
    epochs=CONFIG['epochs'],
    batch_size=CONFIG['batch_size'],
    class_weight=class_weights,
    callbacks=callbacks,
    verbose=2  # Epoch-level logs only (cleaner output)
)

# ============================================================================
# EVALUATION
# ============================================================================
print("\n" + "="*70)
print("📊 EVALUATION")
print("="*70)

# Predictions
y_pred = model.predict(X_test_pad, batch_size=128)
y_pred_classes = np.argmax(y_pred, axis=1)

# Test Accuracy
test_loss, test_acc = model.evaluate(X_test_pad, y_test_enc, verbose=0)
print(f"\n🎯 Test Accuracy: {test_acc:.4f} ({test_acc*100:.2f}%)")

# Classification Report
print("\n📋 Classification Report:")
print("-"*70)
report = classification_report(
    y_test_enc, y_pred_classes,
    target_names=le.classes_,
    digits=4
)
print(report)

with open(os.path.join(CONFIG['output_dir'], 'classification_report.txt'), 'w') as f:
    f.write(f"Test Accuracy: {test_acc:.4f} ({test_acc*100:.2f}%)\n\n")
    f.write(report)

# Confusion Matrix
cm = confusion_matrix(y_test_enc, y_pred_classes)
print("\n📊 Confusion Matrix:")
print(cm)

# Plot Confusion Matrix
plt.figure(figsize=(8, 6))
plt.imshow(cm, interpolation='nearest', cmap='Blues')
plt.title('Confusion Matrix')
plt.colorbar()
tick_marks = np.arange(len(le.classes_))
plt.xticks(tick_marks, le.classes_, rotation=45)
plt.yticks(tick_marks, le.classes_)
plt.xlabel('Predicted')
plt.ylabel('True')
for i in range(len(le.classes_)):
    for j in range(len(le.classes_)):
        plt.text(j, i, str(cm[i, j]), ha='center', va='center')
plt.tight_layout()
plt.savefig(os.path.join(CONFIG['output_dir'], 'confusion_matrix.png'), dpi=150)
print("✅ Confusion Matrix saved")

# Training History Plot
plt.figure(figsize=(12, 4))

plt.subplot(1, 2, 1)
plt.plot(history.history['accuracy'], label='Train')
plt.plot(history.history['val_accuracy'], label='Validation')
plt.title('Accuracy')
plt.xlabel('Epoch')
plt.ylabel('Accuracy')
plt.legend()
plt.grid(True)

plt.subplot(1, 2, 2)
plt.plot(history.history['loss'], label='Train')
plt.plot(history.history['val_loss'], label='Validation')
plt.title('Loss')
plt.xlabel('Epoch')
plt.ylabel('Loss')
plt.legend()
plt.grid(True)

plt.tight_layout()
plt.savefig(os.path.join(CONFIG['output_dir'], 'training_history.png'), dpi=150)
print("✅ Training History saved")

# ============================================================================
# EXPORT TFLITE
# ============================================================================
print("\n📦 Exporting TFLite Model...")

converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

tflite_path = os.path.join(CONFIG['output_dir'], 'kidguard_model.tflite')
with open(tflite_path, 'wb') as f:
    f.write(tflite_model)

model_size_mb = len(tflite_model) / (1024 * 1024)
print(f"✅ TFLite saved: {tflite_path}")
print(f"   Size: {model_size_mb:.2f} MB")

# ============================================================================
# FINAL SUMMARY
# ============================================================================
print("\n" + "="*70)
print("🎉 TRAINING COMPLETE!")
print("="*70)

# Extract Grooming metrics
from sklearn.metrics import precision_recall_fscore_support
precision, recall, f1, support = precision_recall_fscore_support(
    y_test_enc, y_pred_classes, labels=[0, 1]
)

grooming_idx = list(le.classes_).index('grooming') if 'grooming' in le.classes_ else 0

print(f"\n📊 FINAL METRICS:")
print(f"   Overall Accuracy:    {test_acc:.4f} ({test_acc*100:.2f}%)")
print(f"   Grooming Precision:  {precision[grooming_idx]:.4f} ({precision[grooming_idx]*100:.2f}%)")
print(f"   Grooming Recall:     {recall[grooming_idx]:.4f} ({recall[grooming_idx]*100:.2f}%)")
print(f"   Grooming F1-Score:   {f1[grooming_idx]:.4f} ({f1[grooming_idx]*100:.2f}%)")

print(f"\n📁 Output Files:")
print(f"   {CONFIG['output_dir']}/best_model.keras")
print(f"   {CONFIG['output_dir']}/kidguard_model.tflite")
print(f"   {CONFIG['output_dir']}/classification_report.txt")
print(f"   {CONFIG['output_dir']}/confusion_matrix.png")
print(f"   {CONFIG['output_dir']}/training_history.png")
print(f"   {CONFIG['output_dir']}/label_mapping.json")

print(f"\n⏰ End: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
print("="*70)

# Success check
if recall[grooming_idx] >= 0.85:
    print("\n🎯 EXCELLENT: 85%+ Grooming Recall achieved!")
elif recall[grooming_idx] >= 0.75:
    print("\n✅ GOOD: 75%+ Grooming Recall!")
else:
    print(f"\n⚠️ Grooming Recall: {recall[grooming_idx]*100:.1f}% - Consider more training data")
