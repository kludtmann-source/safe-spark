#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
KidGuard Dataset Translation: EN → DE
Übersetzt alle englischen Texte ins Deutsche für deutschsprachige Zielgruppe (7-11 Jahre)

PRIORITÄT: PAN12-Daten sind auf Englisch und müssen übersetzt werden!
"""

import json
import time
from pathlib import Path
from typing import List, Dict
from tqdm import tqdm
import re

# Translation Library (installiere mit: pip install deep-translator)
try:
    from deep_translator import GoogleTranslator
    TRANSLATOR_AVAILABLE = True
except ImportError:
    print("⚠️  deep-translator nicht gefunden!")
    print("   Installiere mit: pip install deep-translator")
    TRANSLATOR_AVAILABLE = False


class DatasetTranslator:
    """
    Übersetzt Dataset von Englisch nach Deutsch

    Features:
    - Batch-Processing mit Rate-Limiting
    - Jugendsprache beibehalten
    - Fehlerbehandlung mit Fallback
    - Progress Tracking
    """

    def __init__(self, rate_limit_delay: float = 0.5):
        """
        Args:
            rate_limit_delay: Pause zwischen Requests (Sekunden)
        """
        self.rate_limit_delay = rate_limit_delay
        self.translator = GoogleTranslator(source='en', target='de') if TRANSLATOR_AVAILABLE else None
        self.translation_errors = []
        self.cache = {}  # Cache für bereits übersetzte Texte

    def is_english(self, text: str) -> bool:
        """
        Prüft ob Text wahrscheinlich Englisch ist

        Heuristik: Häufige englische Wörter
        """
        english_indicators = [
            r'\byou\b', r'\bare\b', r'\byour\b', r'\bthe\b',
            r'\bis\b', r'\bdo\b', r'\bdid\b', r'\bcan\b',
            r'\bwhat\b', r'\bwhere\b', r'\bwhen\b', r'\bhow\b'
        ]

        text_lower = text.lower()
        matches = sum(1 for pattern in english_indicators if re.search(pattern, text_lower))

        # Wenn mindestens 2 englische Indikator-Wörter → wahrscheinlich Englisch
        return matches >= 2

    def translate_text(self, text: str) -> str:
        """
        Übersetzt einzelnen Text EN → DE

        Args:
            text: Englischer Text

        Returns:
            Deutscher Text (oder Original bei Fehler)
        """
        if not text or len(text.strip()) < 2:
            return text

        # Prüfe Cache
        if text in self.cache:
            return self.cache[text]

        # Wenn kein Translator verfügbar → Original zurückgeben
        if not TRANSLATOR_AVAILABLE or not self.translator:
            return text

        try:
            # Rate Limiting
            time.sleep(self.rate_limit_delay)

            # Übersetze
            translated = self.translator.translate(text)

            # Jugendsprache-Optimierungen
            translated = self.optimize_youth_language(translated)

            # Cache speichern
            self.cache[text] = translated

            return translated

        except Exception as e:
            self.translation_errors.append({
                'text': text,
                'error': str(e)
            })
            print(f"\n⚠️  Übersetzungsfehler: {text[:50]}... → {str(e)}")
            return text  # Fallback: Original behalten

    def optimize_youth_language(self, text: str) -> str:
        """
        Optimiert übersetzten Text für Jugendsprache

        Vermeidet steife Übersetzungen, behält Chat-Style
        """
        # Häufige Ersetzungen für natürlichere Jugendsprache
        replacements = {
            'Sie sind': 'du bist',
            'Ihr seid': 'ihr seid',
            'Ihre': 'deine',
            'Ihren': 'deinen',
            'möchtest du': 'willst du',
            'könntest du': 'kannst du',
            'Fotografie': 'Foto',
            'Photograph': 'Foto',
            'Mobiltelefon': 'Handy',
            'Nachricht senden': 'schreiben',
            'Unterhaltung': 'Chat',
        }

        result = text
        for formal, casual in replacements.items():
            result = re.sub(formal, casual, result, flags=re.IGNORECASE)

        return result

    def translate_dataset(self, input_file: str, output_file: str):
        """
        Übersetzt komplettes Dataset

        Args:
            input_file: Pfad zur JSON-Datei (EN)
            output_file: Pfad zur Ausgabe-Datei (DE)
        """
        print(f"\n{'='*70}")
        print(f"📥 Lade Dataset: {input_file}")
        print(f"{'='*70}")

        # Lade JSON
        with open(input_file, 'r', encoding='utf-8') as f:
            data = json.load(f)

        print(f"✅ {len(data)} Samples geladen")

        # Zähle englische Texte
        english_count = sum(1 for item in data if self.is_english(item.get('text', '')))
        print(f"📊 {english_count} englische Texte erkannt → Übersetzung erforderlich")

        if english_count == 0:
            print("✅ Keine Übersetzung nötig - alle Texte bereits deutsch!")
            return

        # Übersetze mit Progress Bar
        print(f"\n🔄 Übersetze {english_count} Texte ins Deutsche...")
        translated_data = []

        for item in tqdm(data, desc="Übersetzung", unit="samples"):
            text = item.get('text', '')

            # Prüfe ob Übersetzung nötig
            needs_translation = self.is_english(text)

            if needs_translation:
                translated_text = self.translate_text(text)

                # Erstelle neues Item mit Translation
                translated_item = {
                    'text': translated_text,
                    'original_text': text,  # Original für Referenz
                    'label': item.get('label', 'STAGE_SAFE'),
                    'source': item.get('source', 'unknown'),
                    'language': 'de'
                }
            else:
                # Text bereits deutsch
                translated_item = {
                    'text': text,
                    'label': item.get('label', 'STAGE_SAFE'),
                    'source': item.get('source', 'unknown'),
                    'language': 'de'
                }

            # Kopiere zusätzliche Felder (stage, context, etc.)
            for key, value in item.items():
                if key not in ['text', 'label', 'source']:
                    translated_item[key] = value

            translated_data.append(translated_item)

        # Speichere übersetztes Dataset
        output_path = Path(output_file)
        output_path.parent.mkdir(parents=True, exist_ok=True)

        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(translated_data, f, indent=2, ensure_ascii=False)

        print(f"\n✅ Übersetzung abgeschlossen!")
        print(f"💾 Gespeichert: {output_file}")
        print(f"📊 {len(translated_data)} Samples")

        # Statistiken
        with_original = sum(1 for item in translated_data if 'original_text' in item)
        print(f"📊 {with_original} Texte übersetzt")
        print(f"📊 {len(translated_data) - with_original} Texte bereits deutsch")

        # Fehler-Report
        if self.translation_errors:
            print(f"\n⚠️  {len(self.translation_errors)} Übersetzungsfehler:")
            for error in self.translation_errors[:5]:  # Zeige max 5
                print(f"   - {error['text'][:50]}... → {error['error']}")

            # Speichere Fehler-Log
            error_log_path = output_path.parent / f"{output_path.stem}_errors.json"
            with open(error_log_path, 'w', encoding='utf-8') as f:
                json.dump(self.translation_errors, f, indent=2, ensure_ascii=False)
            print(f"📄 Fehler-Log: {error_log_path}")

        # Beispiele zeigen
        print(f"\n📝 Beispiele:")
        examples = [item for item in translated_data if 'original_text' in item][:3]
        for i, ex in enumerate(examples, 1):
            print(f"\n{i}. EN: {ex['original_text'][:60]}...")
            print(f"   DE: {ex['text'][:60]}...")
            print(f"   Label: {ex['label']}")


def main():
    """Hauptfunktion - Übersetzt Train und Test Datasets"""

    print("="*70)
    print("🌍 KIDGUARD DATASET TRANSLATION: EN → DE")
    print("="*70)
    print("\nZielgruppe: Deutschsprachige Kinder (7-11 Jahre)")
    print("Quelle: PAN12 (Englisch) + Scientific Papers (Mixed)")
    print("Ziel: Alle Texte auf Deutsch für On-Device-KI\n")

    if not TRANSLATOR_AVAILABLE:
        print("❌ deep-translator nicht installiert!")
        print("\nInstalliere mit:")
        print("   pip install deep-translator")
        return

    # Initialisiere Translator
    translator = DatasetTranslator(rate_limit_delay=0.5)

    # Pfade
    base_dir = Path("training/data/combined")

    # 1. Training Dataset übersetzen
    train_input = base_dir / "kidguard_train.json"
    train_output = base_dir / "kidguard_german_train.json"

    if train_input.exists():
        translator.translate_dataset(str(train_input), str(train_output))
    else:
        print(f"⚠️  Training-Datei nicht gefunden: {train_input}")

    # Reset für Test-Set
    translator.translation_errors = []

    # 2. Test Dataset übersetzen
    test_input = base_dir / "kidguard_test.json"
    test_output = base_dir / "kidguard_german_test.json"

    if test_input.exists():
        print("\n" + "="*70)
        translator.translate_dataset(str(test_input), str(test_output))
    else:
        print(f"⚠️  Test-Datei nicht gefunden: {test_input}")

    print("\n" + "="*70)
    print("✅ ÜBERSETZUNG ABGESCHLOSSEN")
    print("="*70)
    print(f"\n📂 Output:")
    print(f"   - {train_output}")
    print(f"   - {test_output}")
    print(f"\n🎯 Nächster Schritt: python3 training/augment_data.py")


if __name__ == "__main__":
    main()
