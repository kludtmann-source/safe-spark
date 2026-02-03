#!/usr/bin/env python3
"""
Training mit kombiniertem Dataset (Synthetisch + PASYDA)
=========================================================
Trainiert das Grooming-Detection-Modell mit erweitertem Dataset.
"""

import json
import sys
import os

print("=" * 60)
print("🛡️  KID-GUARD: Training mit Scientific Papers Dataset")
print("=" * 60)
print()

# Import TensorFlow
print("1️⃣  Importiere TensorFlow...")
try:
    import tensorflow as tf
    from tensorflow import keras
    import numpy as np
    from sklearn.model_selection import train_test_split
    from sklearn.preprocessing import LabelEncoder
    print(f"✅ TensorFlow {tf.__version__} geladen")
except Exception as e:
    print(f"❌ Fehler beim Import: {e}")
    sys.exit(1)

def load_combined_dataset():
    """Kombiniert synthetische Daten + PASYDA"""
    print()
    print("2️⃣  Lade Datasets...")

    script_dir = os.path.dirname(os.path.abspath(__file__))

    # Lade synthetische Daten
    synthetic_path = os.path.join(script_dir, '..', 'data', 'grooming_stages_dataset.json')
    try:
        with open(synthetic_path, 'r', encoding='utf-8') as f:
            synthetic = json.load(f)
        print(f"✅ Synthetisch: {len(synthetic)} Beispiele")
    except FileNotFoundError:
        print(f"❌ Synthetisches Dataset nicht gefunden: {synthetic_path}")
        synthetic = []

    # Lade PASYDA (wenn vorhanden)
    pasyda_path = os.path.join(script_dir, '..', 'data', 'scientific_augmented_dataset.json')
    try:
        with open(pasyda_path, 'r', encoding='utf-8') as f:
            pasyda = json.load(f)
        print(f"✅ PASYDA: {len(pasyda)} Beispiele")
    except FileNotFoundError:
        print(f"⚠️  PASYDA nicht gefunden: {pasyda_path}")
        print(f"   Führe erst aus: python scripts/prepare_pasyda.py")
        pasyda = []

    # Kombiniere
    combined = synthetic + pasyda
    print(f"📦 Gesamt: {len(combined)} Beispiele")

    if len(combined) == 0:
        print("❌ Keine Daten zum Trainieren!")
        sys.exit(1)

    return combined

def preprocess_data(data):
    """Preprocessing: Tokenization und Encoding"""
    print()
    print("3️⃣  Preprocessing...")

    texts = [item['text'] for item in data]
    labels = [item['label'] for item in data]

    # Tokenizer
    tokenizer = keras.preprocessing.text.Tokenizer(
        num_words=1000,
        oov_token='<OOV>'
    )
    tokenizer.fit_on_texts(texts)
    sequences = tokenizer.texts_to_sequences(texts)
    padded = keras.preprocessing.sequence.pad_sequences(
        sequences,
        maxlen=50,
        padding='post'
    )

    # Label Encoding
    label_encoder = LabelEncoder()
    encoded_labels = label_encoder.fit_transform(labels)

    print(f"✅ Vocabulary: {len(tokenizer.word_index)} Wörter")
    print(f"✅ Klassen: {list(label_encoder.classes_)}")

    return padded, encoded_labels, tokenizer, label_encoder

def build_model(vocab_size, num_classes):
    """Erstelle verbessertes Modell"""
    print()
    print("4️⃣  Baue Modell...")

    model = keras.Sequential([
        # Embedding
        keras.layers.Embedding(vocab_size, 64, input_length=50),

        # Conv1D Layers
        keras.layers.Conv1D(128, 5, activation='relu'),
        keras.layers.GlobalMaxPooling1D(),

        # Dense Layers
        keras.layers.Dense(64, activation='relu'),
        keras.layers.Dropout(0.5),
        keras.layers.Dense(32, activation='relu'),
        keras.layers.Dropout(0.3),
        keras.layers.Dense(num_classes, activation='softmax')
    ])

    model.compile(
        optimizer='adam',
        loss='sparse_categorical_crossentropy',
        metrics=['accuracy']
    )

    print("✅ Modell erstellt (Conv1D mit mehr Kapazität)")
    return model

def train_model(model, X_train, y_train, X_val, y_val, epochs=50):
    """Training mit Early Stopping"""
    print()
    print(f"5️⃣  Starte Training ({epochs} Epochen max)...")
    print("-" * 60)

    callbacks = [
        keras.callbacks.EarlyStopping(
            monitor='val_loss',
            patience=5,
            restore_best_weights=True
        ),
        keras.callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.5,
            patience=3,
            min_lr=0.00001
        )
    ]

    history = model.fit(
        X_train, y_train,
        validation_data=(X_val, y_val),
        epochs=epochs,
        batch_size=16,
        callbacks=callbacks,
        verbose=1
    )

    print("-" * 60)
    print("✅ Training abgeschlossen!")

    return history

def convert_to_tflite(model):
    """Konvertiere zu TFLite"""
    print()
    print("6️⃣  Konvertiere zu TFLite...")

    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    converter.target_spec.supported_ops = [
        tf.lite.OpsSet.TFLITE_BUILTINS,
        tf.lite.OpsSet.SELECT_TF_OPS
    ]
    converter._experimental_lower_tensor_list_ops = False

    try:
        tflite_model = converter.convert()
    except Exception as e:
        print(f"⚠️  Optimierung fehlgeschlagen: {e}")
        print("🔄 Versuche ohne Optimierung...")
        converter = tf.lite.TFLiteConverter.from_keras_model(model)
        converter.target_spec.supported_ops = [
            tf.lite.OpsSet.TFLITE_BUILTINS,
            tf.lite.OpsSet.SELECT_TF_OPS
        ]
        tflite_model = converter.convert()

    return tflite_model

def save_model(tflite_model, tokenizer, label_encoder):
    """Speichere Modell und Metadata"""
    print()
    print("7️⃣  Speichere Modell...")

    script_dir = os.path.dirname(os.path.abspath(__file__))
    models_dir = os.path.join(script_dir, '..', 'models')
    os.makedirs(models_dir, exist_ok=True)

    # Speichere TFLite
    model_path = os.path.join(models_dir, 'grooming_detector_scientific.tflite')
    with open(model_path, 'wb') as f:
        f.write(tflite_model)

    size_mb = len(tflite_model) / (1024 * 1024)
    print(f"✅ Modell: {model_path}")
    print(f"✅ Größe: {size_mb:.2f} MB")

    if size_mb > 5:
        print("⚠️  Warnung: Modell > 5MB")
    else:
        print("✅ Modell < 5MB - Ready for Android!")

    # Speichere Metadata
    metadata = {
        'labels': list(label_encoder.classes_),
        'vocab_size': 1000,
        'max_length': 50,
        'word_index': tokenizer.word_index
    }

    metadata_path = os.path.join(models_dir, 'grooming_detector_scientific_metadata.json')
    with open(metadata_path, 'w', encoding='utf-8') as f:
        json.dump(metadata, f, ensure_ascii=False, indent=2)

    print(f"✅ Metadata: {metadata_path}")

    return model_path, metadata_path

def main():
    # 1. Lade kombiniertes Dataset
    data = load_combined_dataset()

    # 2. Preprocessing
    X, y, tokenizer, label_encoder = preprocess_data(data)

    # 3. Train/Test Split
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    print()
    print(f"📊 Training: {len(X_train)} Samples")
    print(f"📊 Test: {len(X_test)} Samples")

    # 4. Model bauen
    num_classes = len(label_encoder.classes_)
    model = build_model(vocab_size=1000, num_classes=num_classes)

    # 5. Training
    history = train_model(model, X_train, y_train, X_test, y_test, epochs=50)

    # 6. Evaluation
    print()
    print("8️⃣  Evaluation...")
    loss, accuracy = model.evaluate(X_test, y_test, verbose=0)
    print(f"✅ Test Accuracy: {accuracy*100:.1f}%")
    print(f"✅ Test Loss: {loss:.3f}")

    # 7. TFLite Konvertierung
    tflite_model = convert_to_tflite(model)

    # 8. Speichern
    model_path, metadata_path = save_model(tflite_model, tokenizer, label_encoder)

    # 9. Test Predictions
    print()
    print("9️⃣  Test-Predictions:")
    print("-" * 60)

    test_texts = [
        "hast du die hausaufgaben gemacht?",
        "brauchst du robux?",
        "bist du grad allein?",
        "you seem mature for your age",
        "wanna chat on snapchat?"
    ]

    for text in test_texts:
        seq = tokenizer.texts_to_sequences([text])
        pad = keras.preprocessing.sequence.pad_sequences(seq, maxlen=50, padding='post')
        pred = model.predict(pad, verbose=0)
        predicted_class = label_encoder.classes_[np.argmax(pred[0])]
        confidence = np.max(pred[0]) * 100

        print(f"📝 '{text}'")
        print(f"   → {predicted_class} ({confidence:.1f}%)")
        print()

    print("=" * 60)
    print("✅ TRAINING MIT PASYDA ABGESCHLOSSEN!")
    print("=" * 60)
    print()
    print("📦 Output:")
    print(f"   - {model_path}")
    print(f"   - {metadata_path}")
    print()
    print("🚀 Nächste Schritte:")
    print(f"   1. Kopiere in Android App:")
    print(f"      cp {model_path} ../app/src/main/assets/")
    print(f"   2. Teste auf Pixel 10")

if __name__ == "__main__":
    main()
