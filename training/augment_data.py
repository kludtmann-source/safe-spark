#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
KidGuard Data Augmentation - Grooming-Klassen erweitern
Ziel: 150+ Samples pro Grooming-Klasse
"""
import json
import random
import time
from pathlib import Path
from collections import Counter
from tqdm import tqdm

try:
    from deep_translator import GoogleTranslator
    TRANSLATOR_AVAILABLE = True
except ImportError:
    TRANSLATOR_AVAILABLE = False
    print("⚠️  deep-translator nicht installiert")
    print("   pip install deep-translator")

random.seed(42)


class DataAugmenter:
    """Data Augmentation für unbalanciertes Grooming-Dataset"""

    def __init__(self):
        self.de_en = GoogleTranslator(source='de', target='en') if TRANSLATOR_AVAILABLE else None
        self.en_de = GoogleTranslator(source='en', target='de') if TRANSLATOR_AVAILABLE else None

        # Deutsche Synonyme für Grooming-Kontext
        self.synonyms = {
            'allein': ['alleine', 'solo', 'für dich', 'ganz allein'],
            'bild': ['foto', 'pic', 'selfie', 'bild von dir'],
            'schicken': ['senden', 'zeigen', 'geben', 'schicke mir'],
            'geheimnis': ['secret', 'zwischen uns', 'privat', 'nur für uns'],
            'geschenk': ['präsent', 'überraschung', 'belohnung'],
            'reif': ['erwachsen', 'mature', 'älter'],
            'verstehen': ['kapieren', 'checken', 'nachvollziehen'],
            'besonders': ['speziell', 'einzigartig', 'anders']
        }

    def back_translate(self, text):
        """Back-Translation: DE → EN → DE"""
        if not TRANSLATOR_AVAILABLE:
            return text

        try:
            time.sleep(0.5)
            en_text = self.de_en.translate(text)
            time.sleep(0.5)
            de_text = self.en_de.translate(en_text)
            return de_text
        except Exception as e:
            print(f"\n⚠️  Back-Translation Fehler: {str(e)}")
            return text

    def synonym_replace(self, text):
        """Ersetzt Wörter durch Synonyme"""
        words = text.lower().split()
        indices = [i for i, w in enumerate(words) if w in self.synonyms]

        if not indices:
            return text

        # Ersetze max 2 Wörter
        for idx in random.sample(indices, min(2, len(indices))):
            words[idx] = random.choice(self.synonyms[words[idx]])

        return ' '.join(words)

    def augment_dataset(self, input_file, output_file, target=150):
        """Augmentiert komplettes Dataset"""
        print(f"\n{'='*70}")
        print(f"🔄 DATA AUGMENTATION")
        print(f"{'='*70}")
        print(f"\n📥 Lade: {input_file}")

        with open(input_file, 'r', encoding='utf-8') as f:
            data = json.load(f)

        print(f"✅ {len(data)} Samples geladen")

        # Analysiere Label-Distribution
        label_counts = Counter([d['label'] for d in data])

        print(f"\n📊 Aktuelle Label-Distribution:")
        for label, count in sorted(label_counts.items()):
            print(f"   Label {label}: {count}")

        # Identifiziere Grooming-Klassen (Labels 1-5, nicht 0=SAFE)
        grooming_labels = [l for l in label_counts.keys() if l != 0]

        print(f"\n🎯 Ziel: {target} Samples pro Grooming-Klasse")

        # Augmentiere unterrepräsentierte Klassen
        augmented_data = list(data)  # Start mit Original

        for label in grooming_labels:
            current_count = label_counts[label]

            if current_count >= target:
                print(f"   Label {label}: {current_count} ✅ (bereits genug)")
                continue

            needed = target - current_count
            print(f"   Label {label}: {current_count} → brauche {needed} mehr")

            # Finde alle Samples dieser Klasse
            label_samples = [s for s in data if s['label'] == label]

            if not label_samples:
                print(f"   ⚠️  Keine Samples für Label {label}")
                continue

            # Augmentiere bis Target erreicht
            augmented_count = 0

            with tqdm(total=needed, desc=f"Augmentiere Label {label}", unit="samples") as pbar:
                while augmented_count < needed:
                    # Sample zufälliges Original
                    original = random.choice(label_samples)

                    # Wähle Augmentation-Methode
                    method = random.choice(['back', 'syn', 'syn'])  # 2/3 Synonym, 1/3 Back-Translation

                    # Augmentiere
                    aug = original.copy()

                    if method == 'back' and TRANSLATOR_AVAILABLE:
                        aug['text'] = self.back_translate(original['text'])
                    else:
                        aug['text'] = self.synonym_replace(original['text'])

                    aug['augmented'] = True
                    aug['augmentation_method'] = method

                    # Nur hinzufügen wenn Text sich geändert hat
                    if aug['text'] != original['text']:
                        augmented_data.append(aug)
                        augmented_count += 1
                        pbar.update(1)

        # Shuffle
        random.shuffle(augmented_data)

        # Speichere
        output_path = Path(output_file)
        output_path.parent.mkdir(parents=True, exist_ok=True)

        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(augmented_data, f, indent=2, ensure_ascii=False)

        print(f"\n✅ Augmentation abgeschlossen!")
        print(f"💾 Gespeichert: {output_file}")

        # Final Statistics
        final_counts = Counter([d['label'] for d in augmented_data])

        print(f"\n📊 Finale Label-Distribution:")
        for label, count in sorted(final_counts.items()):
            original = label_counts.get(label, 0)
            added = count - original
            print(f"   Label {label}: {count} (+{added})")

        print(f"\n📊 Total:")
        print(f"   Original: {len(data)}")
        print(f"   Augmented: {len(augmented_data)}")
        print(f"   Increase: +{len(augmented_data) - len(data)} samples")


def main():
    """Hauptfunktion - Augmentiert Training-Dataset"""

    print("="*70)
    print("🔄 KIDGUARD DATA AUGMENTATION")
    print("="*70)
    print("\nZiel: Grooming-Klassen auf je 150+ Samples bringen")
    print("Methoden: Back-Translation + Synonym-Replacement\n")

    if not TRANSLATOR_AVAILABLE:
        print("⚠️  deep-translator nicht installiert!")
        print("   Installiere mit: pip install deep-translator")
        print("   → Nur Synonym-Replacement verfügbar\n")

    # Initialisiere Augmenter
    augmenter = DataAugmenter()

    # Augmentiere Training-Set
    input_file = 'training/data/combined/kidguard_german_train.json'
    output_file = 'training/data/augmented/kidguard_augmented_train.json'

    if not Path(input_file).exists():
        print(f"❌ Input-Datei nicht gefunden: {input_file}")
        print(f"   Führe zuerst aus: python3 training/translate_dataset.py")
        return

    augmenter.augment_dataset(
        input_file=input_file,
        output_file=output_file,
        target=150  # Ziel: 150 Samples pro Grooming-Klasse
    )

    print("\n" + "="*70)
    print("✅ DATA AUGMENTATION ABGESCHLOSSEN")
    print("="*70)
    print(f"\n🎯 Nächster Schritt:")
    print(f"   python3 training/train_model.py")


if __name__ == "__main__":
    main()
