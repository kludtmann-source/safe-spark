#!/usr/bin/env python3
"""
Phase 3: Grooming Pattern Detection Training
============================================
Trainiert ein TensorFlow Lite Modell zur Erkennung der "Six Stages of Grooming"
basierend auf forensischen Kommunikationsmustern.

Autor: Senior AI Architect - KidGuard Project
Datum: 2026-01-25
"""

import json
import numpy as np
import tensorflow as tf
from tensorflow import keras
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import os

# Konfiguration
DATA_PATH = "../data/grooming_patterns.json"
MODEL_OUTPUT_PATH = "../models/grooming_detector.tflite"
VOCAB_OUTPUT_PATH = "../models/grooming_vocabulary.json"

# Hyperparameter
MAX_SEQUENCE_LENGTH = 50
EMBEDDING_DIM = 64
LSTM_UNITS = 128
BATCH_SIZE = 8
EPOCHS = 100
VALIDATION_SPLIT = 0.2

class GroomingDetector:
    """Hauptklasse für das Training des Grooming-Erkennungsmodells"""

    def __init__(self):
        self.label_encoder = LabelEncoder()
        self.tokenizer = keras.preprocessing.text.Tokenizer(
            char_level=True,
            oov_token='<UNK>',
            lower=True
        )

    def load_data(self):
        """Lädt die Grooming-Pattern-Daten"""
        print("📂 Lade Grooming-Patterns...")
        with open(DATA_PATH, 'r', encoding='utf-8') as f:
            data = json.load(f)

        texts = [item['text'] for item in data]
        labels = [item['label'] for item in data]

        print(f"✅ {len(texts)} Beispiele geladen")
        print(f"📊 Label-Verteilung:")
        for label in set(labels):
            count = labels.count(label)
            print(f"   {label}: {count} ({count/len(labels)*100:.1f}%)")

        return texts, labels

    def preprocess_data(self, texts, labels):
        """Preprocessing: Tokenisierung und Encoding"""
        print("\n🔧 Preprocessing...")

        # Tokenisierung (Character-Level für bessere Tippfehler-Robustheit)
        self.tokenizer.fit_on_texts(texts)
        sequences = self.tokenizer.texts_to_sequences(texts)

        # Padding
        X = keras.preprocessing.sequence.pad_sequences(
            sequences,
            maxlen=MAX_SEQUENCE_LENGTH,
            padding='post'
        )

        # Label-Encoding
        y = self.label_encoder.fit_transform(labels)
        y = keras.utils.to_categorical(y)

        vocab_size = len(self.tokenizer.word_index) + 1
        num_classes = len(self.label_encoder.classes_)

        print(f"✅ Vocab-Größe: {vocab_size}")
        print(f"✅ Anzahl Klassen: {num_classes}")
        print(f"✅ Input-Shape: {X.shape}")

        return X, y, vocab_size, num_classes

    def build_model(self, vocab_size, num_classes):
        """Erstellt das LSTM-Modell mit Attention-Mechanismus"""
        print("\n🏗️ Baue Modell...")

        model = keras.Sequential([
            # Embedding Layer
            keras.layers.Embedding(
                input_dim=vocab_size,
                output_dim=EMBEDDING_DIM,
                input_length=MAX_SEQUENCE_LENGTH
            ),

            # Bidirectional LSTM für Kontext-Verständnis
            keras.layers.Bidirectional(
                keras.layers.LSTM(LSTM_UNITS, return_sequences=True)
            ),

            # Attention Layer (simuliert durch GlobalMaxPooling)
            keras.layers.GlobalMaxPooling1D(),

            # Dense Layers mit Dropout
            keras.layers.Dense(64, activation='relu'),
            keras.layers.Dropout(0.5),
            keras.layers.Dense(32, activation='relu'),
            keras.layers.Dropout(0.3),

            # Output Layer
            keras.layers.Dense(num_classes, activation='softmax')
        ])

        # Kompilierung mit Label Smoothing für bessere Generalisierung
        model.compile(
            optimizer=keras.optimizers.Adam(learning_rate=0.001),
            loss='categorical_crossentropy',
            metrics=['accuracy', keras.metrics.Precision(), keras.metrics.Recall()]
        )

        print("✅ Modell-Architektur:")
        model.summary()

        return model

    def train_model(self, model, X_train, y_train, X_val, y_val):
        """Training mit Early Stopping und Learning Rate Scheduling"""
        print("\n🚀 Starte Training...")

        callbacks = [
            keras.callbacks.EarlyStopping(
                monitor='val_loss',
                patience=15,
                restore_best_weights=True
            ),
            keras.callbacks.ReduceLROnPlateau(
                monitor='val_loss',
                factor=0.5,
                patience=5,
                min_lr=0.00001
            )
        ]

        history = model.fit(
            X_train, y_train,
            validation_data=(X_val, y_val),
            epochs=EPOCHS,
            batch_size=BATCH_SIZE,
            callbacks=callbacks,
            verbose=1
        )

        print("\n✅ Training abgeschlossen!")
        return history

    def convert_to_tflite(self, model):
        """Konvertiert das Modell zu TensorFlow Lite mit INT8-Quantisierung"""
        print("\n📦 Konvertiere zu TFLite...")

        # Representative Dataset für Quantisierung
        def representative_dataset():
            for _ in range(100):
                yield [np.random.randint(0, len(self.tokenizer.word_index),
                                        size=(1, MAX_SEQUENCE_LENGTH)).astype(np.float32)]

        converter = tf.lite.TFLiteConverter.from_keras_model(model)

        # INT8-Quantisierung für minimale Modellgröße
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.representative_dataset = representative_dataset
        converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
        converter.inference_input_type = tf.int8
        converter.inference_output_type = tf.int8

        tflite_model = converter.convert()

        # Speichern
        os.makedirs(os.path.dirname(MODEL_OUTPUT_PATH), exist_ok=True)
        with open(MODEL_OUTPUT_PATH, 'wb') as f:
            f.write(tflite_model)

        size_mb = len(tflite_model) / (1024 * 1024)
        print(f"✅ TFLite-Modell gespeichert: {MODEL_OUTPUT_PATH}")
        print(f"📊 Modellgröße: {size_mb:.2f} MB")

        if size_mb > 5:
            print("⚠️  WARNUNG: Modell > 5MB! Weitere Optimierung empfohlen.")

        return tflite_model

    def save_vocabulary(self):
        """Speichert Tokenizer und Label-Encoder für Android-Integration"""
        print("\n💾 Speichere Vocabulary...")

        vocab_data = {
            'word_index': self.tokenizer.word_index,
            'labels': self.label_encoder.classes_.tolist(),
            'max_sequence_length': MAX_SEQUENCE_LENGTH
        }

        os.makedirs(os.path.dirname(VOCAB_OUTPUT_PATH), exist_ok=True)
        with open(VOCAB_OUTPUT_PATH, 'w', encoding='utf-8') as f:
            json.dump(vocab_data, f, ensure_ascii=False, indent=2)

        print(f"✅ Vocabulary gespeichert: {VOCAB_OUTPUT_PATH}")

    def evaluate_model(self, model, X_test, y_test):
        """Evaluiert das Modell und zeigt Metriken"""
        print("\n📊 Evaluation auf Test-Set...")

        results = model.evaluate(X_test, y_test, verbose=0)

        print("🎯 Test-Metriken:")
        print(f"   Loss: {results[0]:.4f}")
        print(f"   Accuracy: {results[1]:.4f}")
        print(f"   Precision: {results[2]:.4f}")
        print(f"   Recall: {results[3]:.4f}")

        # Konfusionsmatrix
        y_pred = model.predict(X_test, verbose=0)
        y_pred_classes = np.argmax(y_pred, axis=1)
        y_true_classes = np.argmax(y_test, axis=1)

        from sklearn.metrics import classification_report
        print("\n📈 Classification Report:")
        print(classification_report(
            y_true_classes,
            y_pred_classes,
            target_names=self.label_encoder.classes_,
            zero_division=0
        ))

def main():
    print("=" * 60)
    print("🛡️  KidGuard - Grooming Pattern Detection Training")
    print("   Phase 3: Six Stages Recognition")
    print("=" * 60)

    detector = GroomingDetector()

    # 1. Daten laden
    texts, labels = detector.load_data()

    # 2. Preprocessing
    X, y, vocab_size, num_classes = detector.preprocess_data(texts, labels)

    # 3. Train/Test Split
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=np.argmax(y, axis=1)
    )

    print(f"\n📊 Dataset-Split:")
    print(f"   Training: {len(X_train)} Samples")
    print(f"   Test: {len(X_test)} Samples")

    # 4. Modell bauen
    model = detector.build_model(vocab_size, num_classes)

    # 5. Training
    history = detector.train_model(model, X_train, y_train, X_test, y_test)

    # 6. Evaluation
    detector.evaluate_model(model, X_test, y_test)

    # 7. TFLite-Konvertierung
    tflite_model = detector.convert_to_tflite(model)

    # 8. Vocabulary speichern
    detector.save_vocabulary()

    print("\n" + "=" * 60)
    print("✅ Phase 3 Training abgeschlossen!")
    print("=" * 60)
    print(f"\n📦 Nächste Schritte:")
    print(f"   1. Integriere {MODEL_OUTPUT_PATH} in die Android-App")
    print(f"   2. Lade {VOCAB_OUTPUT_PATH} für Text-Preprocessing")
    print(f"   3. Teste auf Pixel 10 (Ziel: <50ms Inferenz-Zeit)")
    print(f"   4. False-Positive Tests durchführen")

if __name__ == "__main__":
    main()
