#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
KidGuard Training Script - Optimiert für unbalanciertes Dataset

Features:
- Class Weights für unbalancierte Daten
- Focal Loss Option
- Erweiterte Metriken (Precision, Recall, F1 pro Klasse)
- Early Stopping auf Validation Recall
- Confusion Matrix Visualisierung

KRITISCH: Grooming-Recall muss > 95% sein!
"""

import json
import numpy as np
import tensorflow as tf
from pathlib import Path
from typing import Dict, List, Tuple
from sklearn.model_selection import train_test_split
from sklearn.utils.class_weight import compute_class_weight
from sklearn.metrics import classification_report, confusion_matrix
import matplotlib.pyplot as plt
import seaborn as sns

# Konfiguration
np.random.seed(42)
tf.random.set_seed(42)


class FocalLoss(tf.keras.losses.Loss):
    """
    Focal Loss für stark unbalancierte Datasets

    Fokussiert Training auf schwierige Beispiele
    Reduziert Gewicht von einfach klassifizierten Samples
    """

    def __init__(self, gamma=2.0, alpha=0.25, name='focal_loss'):
        """
        Args:
            gamma: Fokussierungs-Parameter (höher = mehr Fokus auf schwierige Samples)
            alpha: Balance-Parameter für Klassen
        """
        super().__init__(name=name)
        self.gamma = gamma
        self.alpha = alpha

    def call(self, y_true, y_pred):
        """Berechnet Focal Loss"""
        # Clip predictions für numerische Stabilität
        epsilon = tf.keras.backend.epsilon()
        y_pred = tf.clip_by_value(y_pred, epsilon, 1.0 - epsilon)

        # Berechne Cross Entropy
        cross_entropy = -y_true * tf.math.log(y_pred)

        # Berechne Focal Loss
        focal_loss = self.alpha * tf.pow(1 - y_pred, self.gamma) * cross_entropy

        return tf.reduce_mean(tf.reduce_sum(focal_loss, axis=-1))


class RecallMetric(tf.keras.metrics.Metric):
    """Custom Recall Metric für Grooming-Klassen"""

    def __init__(self, name='grooming_recall', **kwargs):
        super().__init__(name=name, **kwargs)
        self.true_positives = self.add_weight(name='tp', initializer='zeros')
        self.false_negatives = self.add_weight(name='fn', initializer='zeros')

    def update_state(self, y_true, y_pred, sample_weight=None):
        """Update Recall State"""
        # Nehme Klasse mit höchster Wahrscheinlichkeit
        y_pred_classes = tf.argmax(y_pred, axis=-1)
        y_true_classes = tf.argmax(y_true, axis=-1)

        # Grooming = Klassen 1-5 (nicht STAGE_SAFE=0)
        is_grooming_true = tf.cast(y_true_classes > 0, tf.float32)
        is_grooming_pred = tf.cast(y_pred_classes > 0, tf.float32)

        # True Positives und False Negatives
        tp = tf.reduce_sum(is_grooming_true * is_grooming_pred)
        fn = tf.reduce_sum(is_grooming_true * (1 - is_grooming_pred))

        self.true_positives.assign_add(tp)
        self.false_negatives.assign_add(fn)

    def result(self):
        """Berechne Recall"""
        return self.true_positives / (self.true_positives + self.false_negatives + 1e-7)

    def reset_state(self):
        """Reset für neue Epoch"""
        self.true_positives.assign(0.)
        self.false_negatives.assign(0.)


class KidGuardTrainer:
    """
    Training Pipeline für KidGuard Grooming-Detection

    Optimiert für unbalancierte Daten mit 6 Klassen
    """

    def __init__(self, use_focal_loss: bool = False):
        """
        Args:
            use_focal_loss: Nutze Focal Loss statt CrossEntropy
        """
        self.use_focal_loss = use_focal_loss
        self.label_mapping = {
            'STAGE_SAFE': 0,
            'STAGE_TRUST': 1,
            'STAGE_NEEDS': 2,
            'STAGE_ISOLATION': 3,
            'STAGE_ASSESSMENT': 4,
            'STAGE_SEXUAL': 5
        }
        self.reverse_mapping = {v: k for k, v in self.label_mapping.items()}
        self.tokenizer = None
        self.model = None
        self.class_weights = None
        self.history = None

    def load_data(self, train_file: str, test_file: str = None) -> Tuple:
        """
        Lädt und preprocessed Daten

        Args:
            train_file: Pfad zur Training-JSON
            test_file: Optional - Pfad zur Test-JSON

        Returns:
            (X_train, y_train, X_val, y_val, X_test, y_test)
        """
        print(f"\n📥 Lade Training-Daten: {train_file}")

        with open(train_file, 'r', encoding='utf-8') as f:
            train_data = json.load(f)

        print(f"✅ {len(train_data)} Training-Samples geladen")

        # Extrahiere Texte und Labels
        X_train_full = [item['text'] for item in train_data]
        y_train_full = [self.label_mapping.get(item['label'], 0) for item in train_data]

        # Label-Distribution
        print(f"\n📊 Label-Distribution (Training):")
        unique, counts = np.unique(y_train_full, return_counts=True)
        for label_idx, count in zip(unique, counts):
            label_name = self.reverse_mapping[label_idx]
            percentage = count / len(y_train_full) * 100
            print(f"   {label_name}: {count} ({percentage:.1f}%)")

        # Berechne Class Weights für unbalancierte Daten
        self.class_weights = compute_class_weight(
            class_weight='balanced',
            classes=np.unique(y_train_full),
            y=y_train_full
        )
        class_weight_dict = {i: w for i, w in enumerate(self.class_weights)}

        print(f"\n⚖️  Class Weights (für unbalancierte Daten):")
        for i, weight in class_weight_dict.items():
            print(f"   {self.reverse_mapping[i]}: {weight:.2f}")

        # Split in Train/Validation
        X_train, X_val, y_train, y_val = train_test_split(
            X_train_full, y_train_full,
            test_size=0.2,
            stratify=y_train_full,
            random_state=42
        )

        print(f"\n📊 Split:")
        print(f"   Training: {len(X_train)} samples")
        print(f"   Validation: {len(X_val)} samples")

        # Test-Set (falls vorhanden)
        X_test, y_test = None, None
        if test_file and Path(test_file).exists():
            print(f"\n📥 Lade Test-Daten: {test_file}")
            with open(test_file, 'r', encoding='utf-8') as f:
                test_data = json.load(f)

            X_test = [item['text'] for item in test_data]
            y_test = [self.label_mapping.get(item['label'], 0) for item in test_data]
            print(f"✅ {len(X_test)} Test-Samples geladen")

        return X_train, y_train, X_val, y_val, X_test, y_test, class_weight_dict

    def build_tokenizer(self, texts: List[str], vocab_size: int = 5000):
        """Erstellt Tokenizer"""
        print(f"\n🔤 Erstelle Tokenizer (Vocab Size: {vocab_size})...")

        self.tokenizer = tf.keras.preprocessing.text.Tokenizer(
            num_words=vocab_size,
            oov_token='<OOV>',
            filters='!"#$%&()*+,-./:;<=>?@[\\]^_`{|}~\t\n'
        )

        self.tokenizer.fit_on_texts(texts)

        print(f"✅ Tokenizer erstellt mit {len(self.tokenizer.word_index)} unique Wörtern")

        # Zeige häufigste Wörter
        print(f"\n📝 Top-10 häufigste Wörter:")
        word_counts = sorted(self.tokenizer.word_counts.items(),
                             key=lambda x: x[1], reverse=True)[:10]
        for word, count in word_counts:
            print(f"   {word}: {count}x")

    def tokenize_data(self, texts: List[str], maxlen: int = 50) -> np.ndarray:
        """Tokenisiert Texte"""
        sequences = self.tokenizer.texts_to_sequences(texts)
        padded = tf.keras.preprocessing.sequence.pad_sequences(
            sequences,
            maxlen=maxlen,
            padding='post',
            truncating='post'
        )
        return padded

    def build_model(self, vocab_size: int = 5000, embedding_dim: int = 128,
                     maxlen: int = 50, num_classes: int = 6) -> tf.keras.Model:
        """
        Erstellt CNN-basiertes Modell für Text-Klassifikation

        Optimiert für kleine Model-Größe (TFLite)
        """
        print(f"\n🏗️  Baue Model...")

        model = tf.keras.Sequential([
            # Embedding Layer
            tf.keras.layers.Embedding(
                input_dim=vocab_size,
                output_dim=embedding_dim,
                input_length=maxlen,
                name='embedding'
            ),

            # Spatial Dropout für Regularisierung
            tf.keras.layers.SpatialDropout1D(0.3),

            # Conv1D Layers für Feature-Extraktion
            tf.keras.layers.Conv1D(64, 3, activation='relu', padding='same'),
            tf.keras.layers.GlobalMaxPooling1D(),

            # Dense Layers
            tf.keras.layers.Dense(64, activation='relu'),
            tf.keras.layers.Dropout(0.5),

            # Output Layer
            tf.keras.layers.Dense(num_classes, activation='softmax', name='output')
        ])

        # Loss Function
        if self.use_focal_loss:
            loss = FocalLoss(gamma=2.0, alpha=0.25)
            print("   Loss: Focal Loss (für schwierige Samples)")
        else:
            loss = 'sparse_categorical_crossentropy'
            print("   Loss: Sparse Categorical Crossentropy")

        # Compile mit erweiterten Metriken
        model.compile(
            optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
            loss=loss,
            metrics=[
                'accuracy',
                RecallMetric(),  # Custom Grooming-Recall
                tf.keras.metrics.Precision(name='precision'),
                tf.keras.metrics.Recall(name='recall')
            ]
        )

        print(f"✅ Model erstellt")
        print(f"\n📊 Model-Architektur:")
        model.summary()

        # Zeige Parameter-Count
        trainable_params = sum([tf.size(w).numpy() for w in model.trainable_weights])
        print(f"\n📊 Trainierbare Parameter: {trainable_params:,}")

        self.model = model
        return model

    def train(self, X_train, y_train, X_val, y_val, class_weight_dict,
              epochs: int = 50, batch_size: int = 32):
        """
        Trainiert Model mit Early Stopping auf Validation Recall
        """
        print(f"\n🚀 Starte Training...")
        print(f"   Epochs: {epochs}")
        print(f"   Batch Size: {batch_size}")
        print(f"   Early Stopping: Validation Grooming-Recall")

        # Callbacks
        callbacks = [
            # Early Stopping auf Validation Recall (WICHTIG!)
            tf.keras.callbacks.EarlyStopping(
                monitor='val_grooming_recall',
                patience=10,
                mode='max',
                restore_best_weights=True,
                verbose=1
            ),

            # Reduce LR on Plateau
            tf.keras.callbacks.ReduceLROnPlateau(
                monitor='val_grooming_recall',
                factor=0.5,
                patience=5,
                mode='max',
                verbose=1,
                min_lr=1e-6
            ),

            # Model Checkpoint
            tf.keras.callbacks.ModelCheckpoint(
                'training/models/kidguard_best.keras',
                monitor='val_grooming_recall',
                mode='max',
                save_best_only=True,
                verbose=1
            )
        ]

        # Training
        self.history = self.model.fit(
            X_train, y_train,
            validation_data=(X_val, y_val),
            epochs=epochs,
            batch_size=batch_size,
            class_weight=class_weight_dict,  # WICHTIG für unbalancierte Daten!
            callbacks=callbacks,
            verbose=1
        )

        print(f"\n✅ Training abgeschlossen!")

        # Beste Metriken
        best_epoch = np.argmax(self.history.history['val_grooming_recall'])
        best_recall = self.history.history['val_grooming_recall'][best_epoch]
        best_acc = self.history.history['val_accuracy'][best_epoch]

        print(f"\n📊 Beste Metriken (Epoch {best_epoch+1}):")
        print(f"   Validation Accuracy: {best_acc:.4f}")
        print(f"   Validation Grooming-Recall: {best_recall:.4f}")

        if best_recall < 0.95:
            print(f"\n⚠️  WARNUNG: Grooming-Recall < 95% ({best_recall:.2%})")
            print(f"   Ziel: > 95% für Production!")

    def plot_training_history(self, save_path: str = 'training/reports/training_history.png'):
        """Visualisiert Training-Verlauf"""
        if not self.history:
            print("⚠️  Kein Training-History vorhanden")
            return

        print(f"\n📊 Erstelle Training-Visualisierung...")

        fig, axes = plt.subplots(2, 2, figsize=(15, 10))

        # Accuracy
        axes[0, 0].plot(self.history.history['accuracy'], label='Train')
        axes[0, 0].plot(self.history.history['val_accuracy'], label='Validation')
        axes[0, 0].set_title('Model Accuracy')
        axes[0, 0].set_xlabel('Epoch')
        axes[0, 0].set_ylabel('Accuracy')
        axes[0, 0].legend()
        axes[0, 0].grid(True)

        # Loss
        axes[0, 1].plot(self.history.history['loss'], label='Train')
        axes[0, 1].plot(self.history.history['val_loss'], label='Validation')
        axes[0, 1].set_title('Model Loss')
        axes[0, 1].set_xlabel('Epoch')
        axes[0, 1].set_ylabel('Loss')
        axes[0, 1].legend()
        axes[0, 1].grid(True)

        # Grooming Recall
        axes[1, 0].plot(self.history.history['grooming_recall'], label='Train')
        axes[1, 0].plot(self.history.history['val_grooming_recall'], label='Validation')
        axes[1, 0].axhline(y=0.95, color='r', linestyle='--', label='Target (95%)')
        axes[1, 0].set_title('Grooming Recall (KRITISCH)')
        axes[1, 0].set_xlabel('Epoch')
        axes[1, 0].set_ylabel('Recall')
        axes[1, 0].legend()
        axes[1, 0].grid(True)

        # Precision
        if 'precision' in self.history.history:
            axes[1, 1].plot(self.history.history['precision'], label='Train')
            axes[1, 1].plot(self.history.history['val_precision'], label='Validation')
            axes[1, 1].set_title('Model Precision')
            axes[1, 1].set_xlabel('Epoch')
            axes[1, 1].set_ylabel('Precision')
            axes[1, 1].legend()
            axes[1, 1].grid(True)

        plt.tight_layout()
        Path(save_path).parent.mkdir(parents=True, exist_ok=True)
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        plt.close()

        print(f"✅ Visualisierung gespeichert: {save_path}")

    def save_model_tflite(self, save_path: str = 'app/src/main/assets/kidguard_model.tflite'):
        """Konvertiert und speichert Model als TFLite"""
        if not self.model:
            print("⚠️  Kein Model vorhanden")
            return

        print(f"\n📦 Konvertiere zu TensorFlow Lite...")

        # Konvertierung
        converter = tf.lite.TFLiteConverter.from_keras_model(self.model)
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        tflite_model = converter.convert()

        # Speichern
        Path(save_path).parent.mkdir(parents=True, exist_ok=True)
        with open(save_path, 'wb') as f:
            f.write(tflite_model)

        size_kb = len(tflite_model) / 1024
        print(f"✅ TFLite Model gespeichert: {save_path}")
        print(f"📊 Größe: {size_kb:.1f} KB")

        # Speichere auch Tokenizer-Metadata
        metadata = {
            'vocab_size': len(self.tokenizer.word_index) + 1,
            'word_index': self.tokenizer.word_index,
            'label_mapping': self.label_mapping,
            'max_length': 50
        }

        metadata_path = Path(save_path).parent / 'kidguard_metadata.json'
        with open(metadata_path, 'w', encoding='utf-8') as f:
            json.dump(metadata, f, indent=2, ensure_ascii=False)

        print(f"✅ Metadata gespeichert: {metadata_path}")


def main():
    """Hauptfunktion - Vollständige Training-Pipeline"""

    print("="*70)
    print("🛡️  KIDGUARD MODEL TRAINING")
    print("="*70)
    print("\nOptimiert für unbalanciertes Dataset")
    print("Ziel: Grooming-Recall > 95%\n")

    # Initialisiere Trainer
    trainer = KidGuardTrainer(use_focal_loss=False)  # False = Class Weights nutzen

    # 1. Lade Daten
    # WICHTIG: Nutze augmented Daten nach Ausführung von augment_data.py!
    # Fallback auf german_train.json falls augmented noch nicht existiert
    from pathlib import Path

    augmented_path = 'training/data/augmented/kidguard_augmented_train.json'
    german_path = 'training/data/combined/kidguard_german_train.json'

    if Path(augmented_path).exists():
        print(f"✅ Nutze augmented Daten: {augmented_path}")
        train_file = augmented_path
    else:
        print(f"⚠️  Augmented Daten nicht gefunden, nutze: {german_path}")
        print(f"   Führe zuerst aus: python3 training/augment_data.py")
        train_file = german_path

    X_train, y_train, X_val, y_val, X_test, y_test, class_weights = trainer.load_data(
        train_file=train_file,
        test_file='training/data/combined/kidguard_german_test.json'
    )

    # 2. Baue Tokenizer
    trainer.build_tokenizer(X_train, vocab_size=5000)

    # 3. Tokenisiere Daten
    print(f"\n🔢 Tokenisiere Daten...")
    maxlen = 50
    X_train_tok = trainer.tokenize_data(X_train, maxlen=maxlen)
    X_val_tok = trainer.tokenize_data(X_val, maxlen=maxlen)

    if X_test is not None:
        X_test_tok = trainer.tokenize_data(X_test, maxlen=maxlen)

    # 4. Baue Model
    trainer.build_model(vocab_size=5000, embedding_dim=128, maxlen=maxlen, num_classes=6)

    # 5. Training
    trainer.train(
        X_train_tok, np.array(y_train),
        X_val_tok, np.array(y_val),
        class_weights,
        epochs=50,
        batch_size=32
    )

    # 6. Visualisierung
    trainer.plot_training_history()

    # 7. Speichere TFLite Model
    trainer.save_model_tflite()

    print("\n" + "="*70)
    print("✅ TRAINING ABGESCHLOSSEN")
    print("="*70)
    print(f"\n🎯 Nächster Schritt: python3 training/evaluate_model.py")


if __name__ == "__main__":
    main()
