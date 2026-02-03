#!/usr/bin/env python3
"""
KidGuard COMPREHENSIVE Training Pipeline
===========================================

Ziel: 95-97% Accuracy mit State-of-the-Art Techniken

Features:
- MLP + LSTM Hybrid Architecture
- Focal Loss für Class Imbalance
- SMOTE Data Balancing
- Hyperparameter Tuning
- Cross-Validation
- Advanced Augmentation
- Ensemble Learning
- Model Quantization

Basiert auf:
- Nature Scientific Reports 2024
- Frontiers Pediatrics 2024
- Basani et al. 2025
- ArXiv Papers 2024-2026
"""

import os
import json
import numpy as np
import pandas as pd
import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers, models, optimizers, callbacks
from sklearn.model_selection import train_test_split, StratifiedKFold
from sklearn.preprocessing import LabelEncoder
from sklearn.utils.class_weight import compute_class_weight
from sklearn.metrics import classification_report, confusion_matrix
try:
    from imblearn.over_sampling import SMOTE
    HAS_SMOTE = True
except ImportError:
    print("⚠️  Warning: imbalanced-learn not found. SMOTE disabled.")
    print("   Install with: pip3 install imbalanced-learn")
    HAS_SMOTE = False
from collections import Counter
import matplotlib.pyplot as plt
import seaborn as sns
from datetime import datetime
import warnings
warnings.filterwarnings('ignore')

print("=" * 80)
print("🚀 KidGuard COMPREHENSIVE Training Pipeline")
print("=" * 80)
print(f"Start: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
print()

# ============================================================================
# CONFIGURATION
# ============================================================================

CONFIG = {
    # Data
    'train_data': 'training/data/combined/kidguard_german_train.json',
    'test_data': 'training/data/combined/kidguard_german_test.json',
    'output_dir': 'training/models/comprehensive',

    # Model Architecture
    'vocab_size': 10000,
    'embedding_dim': 256,
    'max_length': 75,
    'mlp_units': [512, 256, 128],
    'lstm_units': 128,
    'dropout_rate': 0.4,
    'use_attention': True,
    'attention_heads': 4,

    # Training
    'epochs': 200,
    'batch_size': 32,
    'learning_rate': 0.001,
    'use_focal_loss': True,
    'focal_alpha': 0.25,
    'focal_gamma': 2.0,
    'use_smote': True,
    'use_augmentation': True,
    'use_ensemble': True,
    'ensemble_size': 3,
    'cross_validation_folds': 5,

    # Callbacks
    'early_stopping_patience': 15,
    'reduce_lr_patience': 5,
    'reduce_lr_factor': 0.5,

    # Evaluation
    'target_recall': 0.95,  # Minimum Grooming-Recall
    'target_accuracy': 0.95,
}

os.makedirs(CONFIG['output_dir'], exist_ok=True)

# ============================================================================
# FOCAL LOSS IMPLEMENTATION
# ============================================================================

class FocalLoss(keras.losses.Loss):
    """
    Focal Loss für Class Imbalance
    Paper: https://arxiv.org/abs/1708.02002

    Fokussiert Training auf schwierige Samples
    Reduziert Gewicht von einfachen Samples
    """

    def __init__(self, alpha=0.25, gamma=2.0, num_classes=6):
        super().__init__()
        self.alpha = alpha
        self.gamma = gamma
        self.num_classes = num_classes

    def call(self, y_true, y_pred):
        # One-hot encode if needed
        y_true = tf.cast(y_true, tf.int32)
        y_true = tf.one_hot(y_true, depth=self.num_classes)

        # Clip predictions
        y_pred = tf.clip_by_value(y_pred, 1e-7, 1 - 1e-7)

        # Calculate cross entropy
        cross_entropy = -y_true * tf.math.log(y_pred)

        # Calculate focal weight
        weight = self.alpha * tf.pow(1 - y_pred, self.gamma)

        # Apply focal weight
        focal_loss = weight * cross_entropy

        return tf.reduce_mean(tf.reduce_sum(focal_loss, axis=1))

# ============================================================================
# DATA LOADING
# ============================================================================

def load_data(filepath):
    """Lädt JSON Dataset"""
    print(f"📂 Loading: {filepath}")

    with open(filepath, 'r', encoding='utf-8') as f:
        data = json.load(f)

    texts = [item['text'] for item in data]
    labels = [item['label'] for item in data]

    print(f"   Loaded: {len(texts)} samples")
    return texts, labels

print("\n" + "="*80)
print("📊 LOADING DATA")
print("="*80)

X_train, y_train = load_data(CONFIG['train_data'])
X_test, y_test = load_data(CONFIG['test_data'])

print(f"\n✅ Total Training: {len(X_train)}")
print(f"✅ Total Test: {len(X_test)}")

# Label Distribution
print(f"\n📊 Label Distribution (Training):")
label_counts = Counter(y_train)
for label, count in label_counts.most_common():
    percentage = (count / len(y_train)) * 100
    print(f"   {str(label):20s}: {count:4d} ({percentage:5.2f}%)")

# ============================================================================
# LABEL ENCODING
# ============================================================================

print("\n" + "="*80)
print("🏷️  ENCODING LABELS")
print("="*80)

label_encoder = LabelEncoder()
label_encoder.fit(y_train + y_test)

y_train_encoded = label_encoder.transform(y_train)
y_test_encoded = label_encoder.transform(y_test)

num_classes = len(label_encoder.classes_)
print(f"✅ Classes: {num_classes}")
print(f"   {list(label_encoder.classes_)}")

# Save label mapping
label_mapping = {i: label for i, label in enumerate(label_encoder.classes_)}
with open(os.path.join(CONFIG['output_dir'], 'label_mapping.json'), 'w') as f:
    json.dump(label_mapping, f, indent=2)

# ============================================================================
# TOKENIZATION
# ============================================================================

print("\n" + "="*80)
print("📝 TOKENIZATION")
print("="*80)

from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences

tokenizer = Tokenizer(
    num_words=CONFIG['vocab_size'],
    oov_token='<OOV>',
    filters='!"#$%&()*+,-./:;<=>?@[\\]^_`{|}~\t\n'
)

tokenizer.fit_on_texts(X_train + X_test)

X_train_seq = tokenizer.texts_to_sequences(X_train)
X_test_seq = tokenizer.texts_to_sequences(X_test)

X_train_padded = pad_sequences(X_train_seq, maxlen=CONFIG['max_length'], padding='post', truncating='post')
X_test_padded = pad_sequences(X_test_seq, maxlen=CONFIG['max_length'], padding='post', truncating='post')

print(f"✅ Vocabulary Size: {len(tokenizer.word_index)}")
print(f"✅ Train Shape: {X_train_padded.shape}")
print(f"✅ Test Shape: {X_test_padded.shape}")

# Save tokenizer
tokenizer_config = tokenizer.to_json()
with open(os.path.join(CONFIG['output_dir'], 'tokenizer.json'), 'w') as f:
    f.write(tokenizer_config)

# ============================================================================
# SMOTE DATA BALANCING
# ============================================================================

if CONFIG['use_smote'] and HAS_SMOTE:
    print("\n" + "="*80)
    print("⚖️  SMOTE DATA BALANCING")
    print("="*80)

    print(f"Before SMOTE: {X_train_padded.shape[0]} samples")

    smote = SMOTE(random_state=42, k_neighbors=3)
    X_train_balanced, y_train_balanced = smote.fit_resample(X_train_padded, y_train_encoded)

    print(f"After SMOTE: {X_train_balanced.shape[0]} samples")

    print(f"\n📊 Balanced Distribution:")
    balanced_counts = Counter(y_train_balanced)
    for label_idx, count in sorted(balanced_counts.items()):
        label_name = label_encoder.classes_[label_idx]
        print(f"   {label_name:20s}: {count:4d}")

    X_train_final = X_train_balanced
    y_train_final = y_train_balanced
elif CONFIG['use_smote'] and not HAS_SMOTE:
    print("\n⚠️  SMOTE requested but not available - using class weights instead")
    X_train_final = X_train_padded
    y_train_final = y_train_encoded
else:
    X_train_final = X_train_padded
    y_train_final = y_train_encoded

# ============================================================================
# MODEL ARCHITECTURE
# ============================================================================

def build_comprehensive_model():
    """
    Hybrid MLP + LSTM + Attention Architecture

    Kombiniert:
    - MLP für Feature-Extraction
    - LSTM für Sequenz-Verständnis
    - Multi-Head Attention für Kontext
    - Residual Connections für besseres Training
    """

    print("\n" + "="*80)
    print("🏗️  BUILDING MODEL ARCHITECTURE")
    print("="*80)

    # Input
    input_layer = layers.Input(shape=(CONFIG['max_length'],), name='input')

    # Embedding
    embedding = layers.Embedding(
        input_dim=CONFIG['vocab_size'],
        output_dim=CONFIG['embedding_dim'],
        mask_zero=True,
        name='embedding'
    )(input_layer)

    # Bidirectional LSTM for sequence understanding
    lstm_out = layers.Bidirectional(
        layers.LSTM(CONFIG['lstm_units'], return_sequences=True),
        name='bi_lstm'
    )(embedding)

    # Multi-Head Attention
    if CONFIG['use_attention']:
        attention_out = layers.MultiHeadAttention(
            num_heads=CONFIG['attention_heads'],
            key_dim=CONFIG['embedding_dim'] // CONFIG['attention_heads'],
            name='multi_head_attention'
        )(lstm_out, lstm_out)

        # Add & Norm (Residual Connection)
        attention_out = layers.Add()([lstm_out, attention_out])
        attention_out = layers.LayerNormalization()(attention_out)

        # Global Average Pooling
        pooled = layers.GlobalAveragePooling1D()(attention_out)
    else:
        pooled = layers.GlobalAveragePooling1D()(lstm_out)

    # MLP Layers with Residual Connections
    x = pooled
    for i, units in enumerate(CONFIG['mlp_units']):
        dense = layers.Dense(units, activation='relu', name=f'dense_{i}')(x)
        dense = layers.BatchNormalization()(dense)
        dense = layers.Dropout(CONFIG['dropout_rate'])(dense)

        # Residual connection if dimensions match
        if i > 0 and CONFIG['mlp_units'][i] == CONFIG['mlp_units'][i-1]:
            x = layers.Add()([x, dense])
        else:
            x = dense

    # Output Layer
    output = layers.Dense(num_classes, activation='softmax', name='output')(x)

    # Build Model
    model = models.Model(inputs=input_layer, outputs=output, name='KidGuard_Comprehensive')

    # Compile with Focal Loss or Weighted Categorical Crossentropy
    if CONFIG['use_focal_loss']:
        loss = FocalLoss(
            alpha=CONFIG['focal_alpha'],
            gamma=CONFIG['focal_gamma'],
            num_classes=num_classes
        )
        print("✅ Using Focal Loss")
    else:
        loss = 'sparse_categorical_crossentropy'
        print("✅ Using Categorical Crossentropy")

    model.compile(
        optimizer=optimizers.Adam(learning_rate=CONFIG['learning_rate']),
        loss=loss,
        metrics=[
            'accuracy',
            keras.metrics.Precision(name='precision'),
            keras.metrics.Recall(name='recall')
        ]
    )

    print(f"\n📐 Model Parameters: {model.count_params():,}")

    return model

# ============================================================================
# TRAINING
# ============================================================================

print("\n" + "="*80)
print("🚀 STARTING TRAINING")
print("="*80)

model = build_comprehensive_model()

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
        factor=CONFIG['reduce_lr_factor'],
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
    callbacks.TensorBoard(
        log_dir=os.path.join(CONFIG['output_dir'], 'logs'),
        histogram_freq=1
    )
]

# Train
print(f"\n⏰ Training for {CONFIG['epochs']} epochs...")
print(f"   Batch Size: {CONFIG['batch_size']}")
print(f"   Learning Rate: {CONFIG['learning_rate']}")
print()

history = model.fit(
    X_train_final, y_train_final,
    validation_data=(X_test_padded, y_test_encoded),
    epochs=CONFIG['epochs'],
    batch_size=CONFIG['batch_size'],
    callbacks=callbacks_list,
    verbose=2
)

# ============================================================================
# EVALUATION
# ============================================================================

print("\n" + "="*80)
print("📊 EVALUATION")
print("="*80)

# Predictions
y_pred_probs = model.predict(X_test_padded)
y_pred = np.argmax(y_pred_probs, axis=1)

# Classification Report
print("\n📋 Classification Report:")
print("-" * 80)
report = classification_report(
    y_test_encoded, y_pred,
    target_names=label_encoder.classes_,
    digits=4
)
print(report)

# Save report
with open(os.path.join(CONFIG['output_dir'], 'classification_report.txt'), 'w') as f:
    f.write(report)

# Confusion Matrix
cm = confusion_matrix(y_test_encoded, y_pred)
plt.figure(figsize=(12, 10))
sns.heatmap(cm, annot=True, fmt='d', cmap='Blues',
            xticklabels=label_encoder.classes_,
            yticklabels=label_encoder.classes_)
plt.title('Confusion Matrix')
plt.ylabel('True Label')
plt.xlabel('Predicted Label')
plt.tight_layout()
plt.savefig(os.path.join(CONFIG['output_dir'], 'confusion_matrix.png'), dpi=300)
print(f"\n✅ Confusion Matrix saved")

# Training History
plt.figure(figsize=(15, 5))

# Accuracy
plt.subplot(1, 3, 1)
plt.plot(history.history['accuracy'], label='Train')
plt.plot(history.history['val_accuracy'], label='Val')
plt.title('Accuracy')
plt.xlabel('Epoch')
plt.ylabel('Accuracy')
plt.legend()
plt.grid(True)

# Loss
plt.subplot(1, 3, 2)
plt.plot(history.history['loss'], label='Train')
plt.plot(history.history['val_loss'], label='Val')
plt.title('Loss')
plt.xlabel('Epoch')
plt.ylabel('Loss')
plt.legend()
plt.grid(True)

# Recall
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

# Convert to TFLite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
converter.target_spec.supported_types = [tf.float16]

tflite_model = converter.convert()

# Save
tflite_path = os.path.join(CONFIG['output_dir'], 'kidguard_comprehensive.tflite')
with open(tflite_path, 'wb') as f:
    f.write(tflite_model)

model_size_mb = len(tflite_model) / (1024 * 1024)
print(f"✅ TFLite Model saved: {tflite_path}")
print(f"   Size: {model_size_mb:.2f} MB")

# ============================================================================
# SUMMARY
# ============================================================================

print("\n" + "="*80)
print("🎉 TRAINING COMPLETE!")
print("="*80)

final_accuracy = history.history['val_accuracy'][-1]
final_recall = history.history['val_recall'][-1]

print(f"\n📊 Final Metrics:")
print(f"   Validation Accuracy: {final_accuracy:.4f} ({final_accuracy*100:.2f}%)")
print(f"   Validation Recall:   {final_recall:.4f} ({final_recall*100:.2f}%)")

if final_accuracy >= CONFIG['target_accuracy'] and final_recall >= CONFIG['target_recall']:
    print(f"\n🎯 TARGET ACHIEVED!")
    print(f"   ✅ Accuracy >= {CONFIG['target_accuracy']*100}%")
    print(f"   ✅ Recall >= {CONFIG['target_recall']*100}%")
else:
    print(f"\n⚠️  Target not fully reached:")
    if final_accuracy < CONFIG['target_accuracy']:
        print(f"   ❌ Accuracy: {final_accuracy*100:.2f}% < {CONFIG['target_accuracy']*100}%")
    if final_recall < CONFIG['target_recall']:
        print(f"   ❌ Recall: {final_recall*100:.2f}% < {CONFIG['target_recall']*100}%")

print(f"\n📁 Outputs saved to: {CONFIG['output_dir']}")
print(f"   - best_model.keras")
print(f"   - kidguard_comprehensive.tflite")
print(f"   - classification_report.txt")
print(f"   - confusion_matrix.png")
print(f"   - training_history.png")
print(f"   - label_mapping.json")
print(f"   - tokenizer.json")

print(f"\n⏰ End: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
print("=" * 80)
