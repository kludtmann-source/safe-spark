#!/usr/bin/env python3
"""
Phase 3: BKA/LKA Deep Dive - Grooming Stage Detection
Trainiert ein TensorFlow Lite Model zur Erkennung der "Six Stages of Grooming"
"""

import json
import numpy as np
import tensorflow as tf
from tensorflow import keras
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import os

# Konfiguration
DATA_FILE = '../data/grooming_stages_dataset.json'
MODEL_OUTPUT = '../models/grooming_detector.tflite'
VOCAB_SIZE = 5000
MAX_LENGTH = 50
EMBEDDING_DIM = 64

class GroomingDetector:
    def __init__(self):
        self.label_encoder = LabelEncoder()
        self.tokenizer = keras.preprocessing.text.Tokenizer(
            num_words=VOCAB_SIZE,
            oov_token='<OOV>'
        )

    def load_data(self):
        """Lade Grooming-Stages Dataset"""
        print("📂 Lade Dataset...")

        with open(DATA_FILE, 'r', encoding='utf-8') as f:
            data = json.load(f)

        texts = [item['text'] for item in data]
        labels = [item['label'] for item in data]

        print(f"✅ {len(texts)} Beispiele geladen")
        print(f"📊 Verteilung: {dict(zip(*np.unique(labels, return_counts=True)))}")

        return texts, labels

    def preprocess_data(self, texts, labels):
        """Tokenisierung und Encoding"""
        print("🔄 Preprocessing...")

        # Tokenize texts
        self.tokenizer.fit_on_texts(texts)
        sequences = self.tokenizer.texts_to_sequences(texts)
        padded = keras.preprocessing.sequence.pad_sequences(
            sequences,
            maxlen=MAX_LENGTH,
            padding='post',
            truncating='post'
        )

        # Encode labels
        encoded_labels = self.label_encoder.fit_transform(labels)

        print(f"✅ Vocabulary size: {len(self.tokenizer.word_index)}")
        print(f"✅ Classes: {list(self.label_encoder.classes_)}")

        return padded, encoded_labels

    def build_model(self, num_classes):
        """Erstelle Multi-Stage Classification Model"""
        print("🏗️ Baue Model...")

        model = keras.Sequential([
            # Embedding Layer
            keras.layers.Embedding(
                VOCAB_SIZE,
                EMBEDDING_DIM,
                input_length=MAX_LENGTH
            ),

            # Bidirectional LSTM für Kontext-Verständnis
            keras.layers.Bidirectional(
                keras.layers.LSTM(64, return_sequences=True)
            ),
            keras.layers.Dropout(0.3),

            # Zweite LSTM-Schicht
            keras.layers.Bidirectional(
                keras.layers.LSTM(32)
            ),
            keras.layers.Dropout(0.3),

            # Dense Layers
            keras.layers.Dense(64, activation='relu'),
            keras.layers.Dropout(0.2),
            keras.layers.Dense(num_classes, activation='softmax')
        ])

        model.compile(
            optimizer='adam',
            loss='sparse_categorical_crossentropy',
            metrics=['accuracy']
        )

        print("✅ Model erstellt")
        model.summary()

        return model

    def train(self, X, y, epochs=50, validation_split=0.2):
        """Trainiere das Model"""
        print(f"🎓 Starte Training ({epochs} Epochen)...")

        num_classes = len(np.unique(y))
        model = self.build_model(num_classes)

        # Callbacks
        early_stop = keras.callbacks.EarlyStopping(
            monitor='val_loss',
            patience=5,
            restore_best_weights=True
        )

        # Training
        history = model.fit(
            X, y,
            epochs=epochs,
            validation_split=validation_split,
            callbacks=[early_stop],
            verbose=1
        )

        print("✅ Training abgeschlossen!")

        return model, history

    def convert_to_tflite(self, model):
        """Konvertiere zu TensorFlow Lite mit Optimierung"""
        print("📦 Konvertiere zu TFLite...")

        # TFLite Converter
        converter = tf.lite.TFLiteConverter.from_keras_model(model)

        # Optimierungen für On-Device Performance
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        converter.target_spec.supported_types = [tf.float16]

        # Konvertierung
        tflite_model = converter.convert()

        # Speichern
        os.makedirs(os.path.dirname(MODEL_OUTPUT), exist_ok=True)
        with open(MODEL_OUTPUT, 'wb') as f:
            f.write(tflite_model)

        size_mb = len(tflite_model) / (1024 * 1024)
        print(f"✅ TFLite Model gespeichert: {MODEL_OUTPUT}")
        print(f"📏 Größe: {size_mb:.2f} MB")

        if size_mb > 5:
            print("⚠️ Warnung: Model > 5MB! Weitere Optimierung empfohlen.")

        return tflite_model

    def evaluate_model(self, model, X_test, y_test):
        """Evaluiere Model-Performance"""
        print("📊 Evaluiere Model...")

        loss, accuracy = model.evaluate(X_test, y_test, verbose=0)

        print(f"✅ Test Accuracy: {accuracy*100:.2f}%")
        print(f"✅ Test Loss: {loss:.4f}")

        # Prediction Examples
        predictions = model.predict(X_test[:5])

        print("\n🔍 Beispiel-Predictions:")
        for i in range(min(5, len(X_test))):
            predicted_class = self.label_encoder.classes_[np.argmax(predictions[i])]
            true_class = self.label_encoder.classes_[y_test[i]]
            confidence = np.max(predictions[i]) * 100

            print(f"  {i+1}. Predicted: {predicted_class} ({confidence:.1f}%) | True: {true_class}")

    def save_metadata(self):
        """Speichere Tokenizer und Label Encoder für Inference"""
        print("💾 Speichere Metadata...")

        metadata = {
            'labels': list(self.label_encoder.classes_),
            'vocab_size': VOCAB_SIZE,
            'max_length': MAX_LENGTH,
            'embedding_dim': EMBEDDING_DIM,
            'word_index': self.tokenizer.word_index
        }

        metadata_file = '../models/grooming_detector_metadata.json'
        with open(metadata_file, 'w', encoding='utf-8') as f:
            json.dump(metadata, f, ensure_ascii=False, indent=2)

        print(f"✅ Metadata gespeichert: {metadata_file}")


def main():
    print("=" * 60)
    print("🛡️  KID-GUARD: Phase 3 - Grooming Stage Detection Training")
    print("=" * 60)

    detector = GroomingDetector()

    # 1. Daten laden
    texts, labels = detector.load_data()

    # 2. Preprocessing
    X, y = detector.preprocess_data(texts, labels)

    # 3. Train/Test Split
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    print(f"📊 Training Samples: {len(X_train)}")
    print(f"📊 Test Samples: {len(X_test)}")

    # 4. Model Training
    model, history = detector.train(X_train, y_train, epochs=50)

    # 5. Evaluation
    detector.evaluate_model(model, X_test, y_test)

    # 6. TFLite Konvertierung
    tflite_model = detector.convert_to_tflite(model)

    # 7. Metadata speichern
    detector.save_metadata()

    print("\n" + "=" * 60)
    print("✅ Training erfolgreich abgeschlossen!")
    print("=" * 60)
    print(f"📦 Model: {MODEL_OUTPUT}")
    print(f"📊 Accuracy: {history.history['accuracy'][-1]*100:.2f}%")
    print("🚀 Bereit für Integration in Android App!")


if __name__ == '__main__':
    main()
