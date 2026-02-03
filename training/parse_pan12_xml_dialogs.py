#!/usr/bin/env python3
"""
PAN12 XML Dialog Parser für KidGuard Training
Extrahiert komplette Chat-Dialoge und labelt sie intelligent
"""

import xml.etree.ElementTree as ET
import json
import re
from collections import defaultdict
from typing import List, Dict, Tuple
import os

print("🔍 PAN12 XML Dialog Parser")
print("=" * 60)

# Pfade zu den XML-Dateien
XML_FILES = [
    '../pan12-sexual-predator-identification-training-corpus-2012-05-01.xml',
    '../pan12-sexual-predator-identification-test-corpus-2012-05-17.xml'
]

# Grooming Keywords für intelligentes Labeling
GROOMING_PATTERNS = {
    'STAGE_TRUST': [
        'mature', 'understand', 'special', 'different', 'secret',
        'trust', 'friend', 'care', 'alone', 'parents dont',
        'reif', 'verstehen', 'besonders', 'anders', 'geheimnis',
        'vertrauen', 'freund', 'kümmern', 'allein', 'eltern verstehen nicht'
    ],
    'STAGE_NEEDS': [
        'money', 'gift', 'buy', 'robux', 'vbucks', 'battle pass',
        'help you', 'give you', 'present', 'surprise',
        'geld', 'geschenk', 'kaufen', 'robux', 'vbucks',
        'helfen', 'geben', 'präsent', 'überraschung'
    ],
    'STAGE_ISOLATION': [
        'discord', 'snapchat', 'whatsapp', 'private', 'delete',
        'secret', 'dont tell', 'between us', 'kik', 'telegram',
        'discord', 'snapchat', 'privat', 'löschen',
        'geheim', 'sag nichts', 'unter uns', 'telegram'
    ],
    'STAGE_ASSESSMENT': [
        'alone', 'home alone', 'where are', 'what you wearing',
        'picture', 'photo', 'cam', 'video', 'show me',
        'allein', 'allein zuhause', 'wo bist', 'was trägst',
        'bild', 'foto', 'cam', 'video', 'zeig mir'
    ],
    'STAGE_SEXUAL': [
        'sex', 'naked', 'body', 'touch', 'kiss', 'meet',
        'horny', 'hot', 'sexy', 'love', 'boyfriend', 'girlfriend',
        'sex', 'nackt', 'körper', 'anfassen', 'küssen', 'treffen',
        'geil', 'heiß', 'sexy', 'liebe', 'freund', 'freundin'
    ]
}

def classify_message(text: str, is_predator: bool) -> str:
    """Klassifiziert Nachricht basierend auf Content"""
    if not is_predator:
        return 'STAGE_SAFE'

    text_lower = text.lower()

    # Zähle Matches pro Stage
    stage_scores = defaultdict(int)
    for stage, patterns in GROOMING_PATTERNS.items():
        for pattern in patterns:
            if pattern in text_lower:
                stage_scores[stage] += 1

    if not stage_scores:
        return 'STAGE_TRUST'  # Default für Predator ohne spezifische Keywords

    # Returniere Stage mit meisten Matches
    return max(stage_scores.items(), key=lambda x: x[1])[0]

def parse_xml_file(filepath: str) -> Tuple[List[Dict], Dict]:
    """Parst PAN12 XML und extrahiert Dialoge"""
    print(f"\n📂 Parse: {os.path.basename(filepath)}")

    try:
        tree = ET.parse(filepath)
        root = tree.getroot()
    except Exception as e:
        print(f"❌ Fehler beim Parsen: {e}")
        return [], {}

    # Extrahiere Conversations
    conversations = root.find('conversations')
    if conversations is None:
        print("⚠️ Keine Conversations gefunden")
        return [], {}

    # Sammle Messages pro Conversation
    conv_messages = defaultdict(list)

    for conv in conversations.findall('conversation'):
        conv_id = conv.get('id')

        for message in conv.findall('message'):
            line = message.find('text')
            author = message.find('author')
            time = message.find('time')

            if line is not None and line.text:
                msg_data = {
                    'text': line.text.strip(),
                    'author': author.text if author is not None else 'unknown',
                    'time': time.text if time is not None else '',
                    'conv_id': conv_id
                }
                conv_messages[conv_id].append(msg_data)

    print(f"✅ {len(conv_messages)} Conversations gefunden")

    # Identifiziere Predators aus <predator-ids>
    predator_ids = set()
    predator_element = root.find('.//predator-ids')
    if predator_element is not None:
        for predator in predator_element.findall('predator'):
            predator_ids.add(predator.text)

    print(f"⚠️ {len(predator_ids)} Predators identifiziert")

    # Extrahiere Training Samples
    samples = []
    stats = {'safe': 0, 'grooming': 0, 'stages': defaultdict(int)}

    for conv_id, messages in conv_messages.items():
        # Check ob Predator in Conversation
        has_predator = any(msg['author'] in predator_ids for msg in messages)

        for msg in messages:
            text = msg['text']

            # Filtere zu kurze Messages
            if len(text) < 10:
                continue

            # Filtere URLs
            if 'http' in text.lower() or 'www.' in text.lower():
                continue

            # Check ob Message von Predator
            is_predator_msg = msg['author'] in predator_ids

            # Klassifiziere
            label = classify_message(text, is_predator_msg)

            sample = {
                'text': text,
                'label': label,
                'source': 'pan12_xml',
                'conv_id': conv_id,
                'author': msg['author'],
                'is_predator': is_predator_msg
            }

            samples.append(sample)

            if label == 'STAGE_SAFE':
                stats['safe'] += 1
            else:
                stats['grooming'] += 1
                stats['stages'][label] += 1

    print(f"\n📊 Extrahierte Samples: {len(samples)}")
    print(f"   Safe: {stats['safe']}")
    print(f"   Grooming: {stats['grooming']}")
    for stage, count in stats['stages'].items():
        print(f"     {stage}: {count}")

    return samples, stats

def clean_text(text: str) -> str:
    """Reinigt Text von Sonderzeichen"""
    # Ersetze multiple Spaces
    text = re.sub(r'\s+', ' ', text)
    # Entferne leading/trailing spaces
    text = text.strip()
    # Entferne sehr spezielle Zeichen
    text = re.sub(r'[^\w\s\.,!?\-äöüÄÖÜß]', '', text)
    return text

def main():
    all_samples = []
    total_stats = {'safe': 0, 'grooming': 0, 'stages': defaultdict(int)}

    # Parse beide XML-Dateien
    for xml_file in XML_FILES:
        if not os.path.exists(xml_file):
            print(f"⚠️ Datei nicht gefunden: {xml_file}")
            continue

        samples, stats = parse_xml_file(xml_file)
        all_samples.extend(samples)

        total_stats['safe'] += stats['safe']
        total_stats['grooming'] += stats['grooming']
        for stage, count in stats['stages'].items():
            total_stats['stages'][stage] += count

    if not all_samples:
        print("\n❌ Keine Samples extrahiert!")
        return

    print("\n" + "=" * 60)
    print("📊 GESAMT STATISTIK")
    print("=" * 60)
    print(f"Total Samples: {len(all_samples)}")
    print(f"Safe: {total_stats['safe']} ({total_stats['safe']/len(all_samples)*100:.1f}%)")
    print(f"Grooming: {total_stats['grooming']} ({total_stats['grooming']/len(all_samples)*100:.1f}%)")
    print("\nGrooming Stages:")
    for stage, count in sorted(total_stats['stages'].items()):
        print(f"  {stage}: {count}")

    # Clean samples
    print("\n🧹 Reinige Texte...")
    cleaned_samples = []
    for sample in all_samples:
        sample['text'] = clean_text(sample['text'])
        if len(sample['text']) >= 10:  # Nur Texte mit min. 10 Zeichen
            cleaned_samples.append(sample)

    print(f"✅ {len(cleaned_samples)} Samples nach Cleaning")

    # Speichern
    output_file = 'data/pan12_dialogs_extracted.json'
    os.makedirs('data', exist_ok=True)

    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(cleaned_samples, f, indent=2, ensure_ascii=False)

    print(f"\n💾 Gespeichert: {output_file}")
    print(f"   Größe: {os.path.getsize(output_file) / 1024:.1f} KB")

    # Erstelle auch Multi-Class Version (5 Stages)
    print("\n🔄 Erstelle Multi-Class Dataset...")

    # Konvertiere zu Multi-Class Labels (0-4)
    label_map = {
        'STAGE_SAFE': 0,
        'STAGE_TRUST': 1,
        'STAGE_NEEDS': 2,
        'STAGE_ISOLATION': 3,
        'STAGE_ASSESSMENT': 4,
        'STAGE_SEXUAL': 4  # Merge mit ASSESSMENT für Balance
    }

    multiclass_samples = []
    for sample in cleaned_samples:
        mc_sample = sample.copy()
        mc_sample['label_numeric'] = label_map.get(sample['label'], 0)
        multiclass_samples.append(mc_sample)

    output_mc = 'data/pan12_dialogs_multiclass.json'
    with open(output_mc, 'w', encoding='utf-8') as f:
        json.dump(multiclass_samples, f, indent=2, ensure_ascii=False)

    print(f"✅ Multi-Class gespeichert: {output_mc}")

    # Label Distribution für Multi-Class
    from collections import Counter
    label_dist = Counter([s['label_numeric'] for s in multiclass_samples])
    print("\n📊 Multi-Class Label Distribution:")
    for label, count in sorted(label_dist.items()):
        stage_name = [k for k, v in label_map.items() if v == label][0]
        print(f"   {label} ({stage_name}): {count} ({count/len(multiclass_samples)*100:.1f}%)")

    print("\n" + "=" * 60)
    print("🎉 PAN12 XML PARSING ABGESCHLOSSEN!")
    print("=" * 60)
    print("\n🚀 Nächste Schritte:")
    print("   1. Kombiniere mit bestehendem Dataset:")
    print("      python3 combine_all_datasets.py")
    print("   2. Trainiere Advanced Model:")
    print("      python3 train_advanced_model.py")
    print("   3. Teste auf Pixel 10!")

if __name__ == '__main__':
    main()
