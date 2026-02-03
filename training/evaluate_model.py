#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
KidGuard Model Evaluation Script

Ausführliche Evaluation mit:
- Classification Report pro Klasse
- Confusion Matrix Visualisierung
- Per-Class Recall (kritisch!)
- False-Negative-Analyse

KRITISCH: Grooming-Recall > 95%!
"""

import json
import numpy as np
import tensorflow as tf
from pathlib import Path
from sklearn.metrics import (
    classification_report,
    confusion_matrix, 
    precision_recall_fscore_support
)
import matplotlib.pyplot as plt
import seaborn as sns


class ModelEvaluator:
    """Evaluiert trainiertes KidGuard Model"""
    
    def __init__(self, model_path: str, metadata_path: str):
        """
        Args:
            model_path: Pfad zum .keras Model
            metadata_path: Pfad zur metadata.json
        """
        print(f"📥 Lade Model: {model_path}")
        self.model = tf.keras.models.load_model(model_path, compile=False)
        
        print(f"📥 Lade Metadata: {metadata_path}")
        with open(metadata_path) as f:
            self.metadata = json.load(f)
        
        self.label_mapping = self.metadata['label_mapping']
        self.reverse_mapping = {v: k for k, v in self.label_mapping.items()}
        self.tokenizer = self._build_tokenizer()
    
    def _build_tokenizer(self):
        """Rekonstruiert Tokenizer aus Metadata"""
        tokenizer = tf.keras.preprocessing.text.Tokenizer(
            num_words=self.metadata['vocab_size']
        )
        tokenizer.word_index = self.metadata['word_index']
        return tokenizer
    
    def load_test_data(self, test_file: str):
        """Lädt Test-Daten"""
        print(f"\n📥 Lade Test-Daten: {test_file}")
        
        with open(test_file) as f:
            data = json.load(f)
        
        X = [d['text'] for d in data]
        y = [self.label_mapping.get(d['label'], 0) for d in data]
        
        print(f"✅ {len(X)} Test-Samples geladen")
        
        return X, y, data
    
    def tokenize(self, texts):
        """Tokenisiert Texte"""
        seq = self.tokenizer.texts_to_sequences(texts)
        return tf.keras.preprocessing.sequence.pad_sequences(
            seq, maxlen=self.metadata['max_length'], padding='post'
        )
    
    def evaluate(self, X_test, y_test):
        """Führt vollständige Evaluation durch"""
        print(f"\n{'='*70}")
        print("🔍 MODEL EVALUATION")
        print(f"{'='*70}")
        
        # Tokenisiere
        X_test_tok = self.tokenize(X_test)
        
        # Predictions
        print(f"\n🤖 Predictions...")
        y_pred_proba = self.model.predict(X_test_tok, verbose=0)
        y_pred = np.argmax(y_pred_proba, axis=1)
        
        # Classification Report
        print(f"\n📊 CLASSIFICATION REPORT:")
        print("="*70)
        
        target_names = [self.reverse_mapping[i] for i in range(len(self.label_mapping))]
        report = classification_report(
            y_test, y_pred,
            target_names=target_names,
            digits=4
        )
        print(report)
        
        # Per-Class Metriken
        precision, recall, f1, support = precision_recall_fscore_support(
            y_test, y_pred, average=None, labels=range(len(self.label_mapping))
        )
        
        print(f"\n📊 PER-CLASS RECALL (KRITISCH):")
        print("="*70)
        for i, label_name in enumerate(target_names):
            emoji = "✅" if recall[i] >= 0.95 else "⚠️ "
            print(f"{emoji} {label_name:20s}: {recall[i]:.2%} (Support: {int(support[i])})")
        
        # Grooming-Recall (Klassen 1-5)
        grooming_indices = [i for i in range(1, len(self.label_mapping))]
        grooming_mask = np.isin(y_test, grooming_indices)
        
        if grooming_mask.sum() > 0:
            grooming_y_true = np.array(y_test)[grooming_mask]
            grooming_y_pred = y_pred[grooming_mask]
            grooming_recall = np.mean(grooming_y_pred == grooming_y_true)
            
            print(f"\n🎯 GESAMT-GROOMING-RECALL:")
            emoji = "✅" if grooming_recall >= 0.95 else "❌"
            print(f"{emoji} {grooming_recall:.2%} (Ziel: > 95%)")
            
            if grooming_recall < 0.95:
                print(f"\n⚠️  WARNUNG: Recall zu niedrig für Production!")
        
        # Confusion Matrix
        self.plot_confusion_matrix(y_test, y_pred, target_names)
        
        # False Negative Analysis
        self.analyze_false_negatives(X_test, y_test, y_pred, y_pred_proba)
        
        return y_pred, y_pred_proba
    
    def plot_confusion_matrix(self, y_true, y_pred, labels):
        """Visualisiert Confusion Matrix"""
        print(f"\n📊 Erstelle Confusion Matrix...")
        
        cm = confusion_matrix(y_true, y_pred)
        
        plt.figure(figsize=(12, 10))
        sns.heatmap(
            cm, annot=True, fmt='d', cmap='Blues',
            xticklabels=labels, yticklabels=labels
        )
        plt.title('Confusion Matrix', fontsize=16, fontweight='bold')
        plt.ylabel('True Label')
        plt.xlabel('Predicted Label')
        plt.xticks(rotation=45, ha='right')
        plt.tight_layout()
        
        save_path = 'training/reports/confusion_matrix.png'
        Path(save_path).parent.mkdir(parents=True, exist_ok=True)
        plt.savefig(save_path, dpi=300, bbox_inches='tight')
        plt.close()
        
        print(f"✅ Confusion Matrix: {save_path}")
    
    def analyze_false_negatives(self, X_test, y_true, y_pred, y_pred_proba):
        """Analysiert False Negatives (verpasste Grooming-Messages)"""
        print(f"\n🔍 FALSE NEGATIVE ANALYSIS:")
        print("="*70)
        
        # Finde False Negatives (Grooming als SAFE klassifiziert)
        grooming_indices = [i for i in range(1, len(self.label_mapping))]
        
        false_negatives = []
        for i, (true_label, pred_label) in enumerate(zip(y_true, y_pred)):
            # True = Grooming, Predicted = SAFE
            if true_label in grooming_indices and pred_label == 0:
                false_negatives.append({
                    'index': i,
                    'text': X_test[i],
                    'true_label': self.reverse_mapping[true_label],
                    'pred_label': self.reverse_mapping[pred_label],
                    'confidence': y_pred_proba[i][pred_label]
                })
        
        print(f"\n❌ {len(false_negatives)} False Negatives gefunden")
        print(f"   (Grooming-Messages fälschlich als SAFE klassifiziert)\n")
        
        if false_negatives:
            print("📝 Top-10 False Negatives:")
            for i, fn in enumerate(false_negatives[:10], 1):
                print(f"\n{i}. Text: '{fn['text'][:60]}...'")
                print(f"   True: {fn['true_label']} → Predicted: {fn['pred_label']}")
                print(f"   Confidence: {fn['confidence']:.2%}")
            
            # Speichere vollständige Liste
            fn_path = 'training/reports/false_negatives.json'
            with open(fn_path, 'w', encoding='utf-8') as f:
                json.dump(false_negatives, f, indent=2, ensure_ascii=False)
            
            print(f"\n💾 Vollständige Liste: {fn_path}")
        else:
            print("✅ Keine False Negatives! Perfekt!")


def main():
    """Hauptfunktion"""
    print("="*70)
    print("🔍 KIDGUARD MODEL EVALUATION")
    print("="*70)
    
    # Pfade
    model_path = 'training/models/kidguard_best.keras'
    metadata_path = 'app/src/main/assets/kidguard_metadata.json'
    test_file = 'training/data/combined/kidguard_german_test.json'
    
    # Prüfe ob Dateien existieren
    if not Path(model_path).exists():
        print(f"❌ Model nicht gefunden: {model_path}")
        print(f"   Führe zuerst aus: python3 training/train_model.py")
        return
    
    if not Path(test_file).exists():
        print(f"❌ Test-Daten nicht gefunden: {test_file}")
        print(f"   Führe zuerst aus: python3 training/translate_dataset.py")
        return
    
    # Evaluiere
    evaluator = ModelEvaluator(model_path, metadata_path)
    X_test, y_test, test_data = evaluator.load_test_data(test_file)
    y_pred, y_pred_proba = evaluator.evaluate(X_test, y_test)
    
    print("\n" + "="*70)
    print("✅ EVALUATION ABGESCHLOSSEN")
    print("="*70)
    print(f"\n📂 Reports:")
    print(f"   - training/reports/confusion_matrix.png")
    print(f"   - training/reports/false_negatives.json")


if __name__ == "__main__":
    main()
