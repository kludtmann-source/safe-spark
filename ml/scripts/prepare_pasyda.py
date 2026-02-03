#!/usr/bin/env python3
"""
PASYDA Dataset Integration für KidGuard
========================================
Lädt den PASYDA-Datensatz (Perverted-Justice Archive),
übersetzt ihn nach Deutsch und klassifiziert nach Grooming Stages.
"""

import pandas as pd
import json
import os
from pathlib import Path

print("=" * 60)
print("📥 PASYDA Dataset Integration")
print("=" * 60)
print()

# Prüfe ob deep-translator installiert ist
try:
    from deep_translator import GoogleTranslator
    from tqdm import tqdm
    print("✅ Dependencies gefunden")
except ImportError as e:
    print("❌ Fehlende Abhängigkeiten!")
    print("   Installiere mit: pip install deep-translator tqdm")
    exit(1)

def download_pasyda():
    """Lädt PASYDA Dataset von GitHub"""
    print("1️⃣  Lade PASYDA Dataset von GitHub...")

    # Haupt-Dataset URL
    urls = [
        "https://raw.githubusercontent.com/rdelemos/PASYDA/master/data/conversations.csv",
        "https://raw.githubusercontent.com/rdelemos/PASYDA/master/conversations.csv",
        "https://github.com/rdelemos/PASYDA/raw/master/data.zip"  # Fallback
    ]

    df = None
    for url in urls:
        try:
            print(f"   Versuche: {url}")
            df = pd.read_csv(url)
            print(f"✅ Dataset geladen: {len(df)} Einträge")
            break
        except Exception as e:
            print(f"   ⚠️  Fehler: {e}")
            continue

    if df is None:
        print("❌ Konnte PASYDA nicht laden. Erstelle Demo-Dataset...")
        # Fallback: Erstelle synthetisches Demo-Dataset
        df = pd.DataFrame({
            'author': ['predator'] * 50,
            'text': [
                "how old are you?", "you seem mature for your age",
                "wanna be friends?", "you're special",
                "can i buy you something?", "do you have paypal?",
                "lets chat on snapchat", "delete this conversation",
                "are you alone?", "where are your parents?"
            ] * 5
        })
        print(f"✅ Demo-Dataset erstellt: {len(df)} Beispiele")

    return df

def map_to_grooming_stages(text):
    """Klassifiziert Text nach den 5 Grooming Stages"""
    if pd.isna(text):
        return 'STAGE_SAFE'

    text_lower = str(text).lower()

    # Keyword-Mapping (Englisch, da Original-Texte)
    stage_keywords = {
        'STAGE_TRUST': [
            'special', 'mature', 'understand', 'different', 'older than',
            'more mature', 'cool for your age', 'not like other', 'trust me',
            'friends', 'i like you', 'youre smart'
        ],
        'STAGE_NEEDS': [
            'gift', 'buy', 'money', 'present', 'steam', 'paypal', 'venmo',
            'vbucks', 'robux', 'itunes', 'gift card', 'send you', 'pay for',
            'get you something', 'want anything'
        ],
        'STAGE_ISOLATION': [
            'private', 'delete', 'secret', 'telegram', 'snapchat', 'kik',
            'dont tell', 'parents dont', 'between us', 'our secret',
            'switch to', 'other app', 'erase', 'clear history'
        ],
        'STAGE_ASSESSMENT': [
            'alone', 'room', 'parents home', 'door closed', 'anyone see',
            'by yourself', 'where are', 'who is there', 'privacy',
            'can someone hear', 'got time', 'available'
        ],
    }

    # Score berechnen
    scores = {}
    for stage, keywords in stage_keywords.items():
        score = sum(2 if kw in text_lower else 0 for kw in keywords)
        scores[stage] = score

    # Bestes Match
    max_score = max(scores.values())
    if max_score > 0:
        return max(scores, key=scores.get)

    return 'STAGE_SAFE'

def translate_batch(texts, max_batch=50):
    """Übersetzt Texte nach Deutsch (mit Rate Limiting)"""
    print("2️⃣  Übersetze nach Deutsch (kann einige Minuten dauern)...")

    translator = GoogleTranslator(source='en', target='de')
    translated = []

    for i in tqdm(range(0, len(texts), max_batch), desc="🌐 Übersetzung"):
        batch = texts[i:i+max_batch]
        for text in batch:
            try:
                if pd.notna(text) and len(str(text).strip()) > 0:
                    trans = translator.translate(str(text)[:500])  # Max 500 Zeichen
                    translated.append(trans)
                else:
                    translated.append(text)
            except Exception as e:
                # Bei Fehler: Original behalten
                translated.append(text)

    return translated

def prepare_training_data(df, translate=True):
    """Bereitet PASYDA-Daten für Training vor"""
    print()
    print("3️⃣  Bereite Trainingsdaten vor...")

    # Filtere nur Predator-Nachrichten
    if 'author' in df.columns:
        predator_df = df[df['author'] == 'predator'].copy()
        print(f"   Gefiltert: {len(predator_df)} Predator-Nachrichten")
    else:
        predator_df = df.copy()
        print(f"   Keine Author-Spalte, nutze alle {len(predator_df)} Nachrichten")

    # Klassifiziere nach Grooming Stages (auf Englisch)
    print("   Klassifiziere Grooming Stages...")
    predator_df['label'] = predator_df['text'].apply(map_to_grooming_stages)

    # Übersetze nach Deutsch (optional, da zeitaufwendig)
    if translate:
        texts = predator_df['text'].tolist()[:100]  # Limit auf 100 für Demo
        print(f"   ⚠️  Übersetze nur erste 100 Nachrichten (Zeit-Limit)")
        predator_df = predator_df.head(100)
        predator_df['text_de'] = translate_batch(texts)
    else:
        print("   ⚠️  Überspringe Übersetzung (nutze Englisch)")
        predator_df['text_de'] = predator_df['text']

    # Erstelle JSON für Training
    training_data = []
    for _, row in predator_df.iterrows():
        if pd.notna(row['text_de']) and len(str(row['text_de']).strip()) > 5:
            training_data.append({
                "label": row['label'],
                "text": str(row['text_de'])
            })

    return training_data

def main():
    # 1. Download PASYDA
    df = download_pasyda()

    # 2. Prepare Training Data (ohne Übersetzung für schnellere Demo)
    print()
    print("⚠️  HINWEIS: Übersetzung deaktiviert für schnellere Verarbeitung")
    print("   Das Modell wird mit englischen Texten trainiert.")
    print("   Für Production: translate=True setzen")
    print()

    training_data = prepare_training_data(df, translate=False)

    # 3. Speichere als JSON
    script_dir = os.path.dirname(os.path.abspath(__file__))
    output_path = os.path.join(script_dir, '..', 'data', 'pasyda_grooming_dataset.json')

    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(training_data, f, ensure_ascii=False, indent=2)

    print()
    print(f"✅ {len(training_data)} Beispiele gespeichert")
    print(f"📁 Pfad: {output_path}")

    # 4. Statistik
    labels = [d['label'] for d in training_data]
    print()
    print("📊 Label-Verteilung:")
    for label in sorted(set(labels)):
        count = labels.count(label)
        print(f"   {label}: {count} ({count/len(labels)*100:.1f}%)")

    print()
    print("=" * 60)
    print("✅ PASYDA Integration abgeschlossen!")
    print("=" * 60)
    print()
    print("🚀 Nächster Schritt:")
    print("   python scripts/train_with_pasyda.py")

if __name__ == "__main__":
    main()
