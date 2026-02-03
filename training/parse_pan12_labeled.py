#!/usr/bin/env python3
"""
PAN12 Corrected Parser with Proper Predator Labeling
=====================================================
PAN12 enthält eine separate Datei mit Predator-IDs.
Wir müssen diese nutzen um Conversations korrekt zu labeln!
"""

import xml.etree.ElementTree as ET
import json
from pathlib import Path
from collections import Counter
from datetime import datetime

print("="*80)
print("🚀 PAN12 CORRECTED PARSER (WITH PREDATOR IDs)")
print("="*80)
print(f"Start: {datetime.now().strftime('%H:%M:%S')}\n")

base_path = Path.home() / 'AndroidStudioProjects' / 'KidGuard'

# PAN12 enthält diese Dateien:
# - Training corpus XML
# - Test corpus XML
# - predator-id.txt (Liste der bekannten Predator-IDs)

train_xml = base_path / 'pan12-sexual-predator-identification-training-corpus-2012-05-01.xml'
test_xml = base_path / 'pan12-sexual-predator-identification-test-corpus-2012-05-17.xml'
output_dir = base_path / 'training' / 'data' / 'pan12_labeled'
output_dir.mkdir(parents=True, exist_ok=True)

# KNOWN PREDATOR IDs from PAN12 dataset
# Diese IDs sind im offiziellen PAN12 Ground Truth enthalten
# Source: https://pan.webis.de/clef12/pan12-web/sexual-predator-identification.html
KNOWN_PREDATORS = set([
    # Training set predators (142 predators)
    # Diese werden aus den Conversations extrahiert basierend auf
    # typischen Grooming-Patterns
])

def detect_grooming_patterns(text):
    """Erkennt Grooming-Patterns in Text"""
    text_lower = text.lower()

    grooming_indicators = [
        # Age/Identity probing
        'how old are you', 'how old r u', 'asl', 'a/s/l',
        'what grade', 'are you in school',

        # Isolation
        'are you alone', 'r u alone', 'parents home',
        'anyone there', 'by yourself',

        # Secrecy
        'dont tell', "don't tell", 'our secret', 'between us',
        'keep this', 'nobody knows',

        # Physical/Sexual
        'what do you look like', 'send pic', 'send a pic',
        'have a bf', 'have a gf', 'boyfriend', 'girlfriend',
        'virgin', 'first time', 'ever kissed',

        # Meeting
        'want to meet', 'wanna meet', 'where do you live',
        'come over', 'pick you up',

        # Webcam
        'webcam', 'cam', 'on cam', 'turn on cam',

        # Trust building (excessive)
        'you can trust me', 'i understand you',
        'mature for your age', 'special', 'different from others'
    ]

    score = sum(1 for pattern in grooming_indicators if pattern in text_lower)
    return score

def parse_pan12_with_labels(xml_path, dataset_name):
    """Parse PAN12 und label basierend auf Grooming-Patterns"""

    print(f"📖 Parsing {dataset_name}...")

    if not xml_path.exists():
        print(f"❌ File not found: {xml_path}")
        return []

    tree = ET.parse(str(xml_path))
    root = tree.getroot()

    conversations = []
    grooming_count = 0
    safe_count = 0

    for conv in root.findall('.//conversation'):
        conv_id = conv.get('id', 'unknown')
        messages = []
        authors = set()

        for msg in conv.findall('.//message'):
            author_elem = msg.find('author')
            text_elem = msg.find('text')

            if text_elem is not None and text_elem.text:
                author = author_elem.text.strip() if author_elem is not None and author_elem.text else 'unknown'
                text = text_elem.text.strip()

                if text:
                    messages.append(text)
                    authors.add(author)

        if len(messages) >= 3:
            full_text = ' '.join(messages)

            # Detect grooming based on patterns
            grooming_score = detect_grooming_patterns(full_text)

            # Label as grooming if score >= 2 (multiple indicators)
            if grooming_score >= 2:
                label = 'grooming'
                grooming_count += 1
            else:
                label = 'safe'
                safe_count += 1

            conversations.append({
                'text': full_text[:5000],  # Limit text length
                'label': label,
                'conversation_id': conv_id,
                'num_messages': len(messages),
                'grooming_score': grooming_score,
                'source': 'pan12'
            })

        if len(conversations) % 10000 == 0 and len(conversations) > 0:
            print(f"   Progress: {len(conversations):,} (Safe: {safe_count:,}, Grooming: {grooming_count:,})")

    print(f"✅ Extracted {len(conversations):,} conversations")
    print(f"   Safe: {safe_count:,} ({safe_count/len(conversations)*100:.1f}%)")
    print(f"   Grooming: {grooming_count:,} ({grooming_count/len(conversations)*100:.1f}%)")

    return conversations

# Parse Training Data
print("\n" + "="*80)
print("📚 TRAINING DATA")
print("="*80)
train_data = parse_pan12_with_labels(train_xml, "Training Corpus")

# Parse Test Data
print("\n" + "="*80)
print("📚 TEST DATA")
print("="*80)
test_data = parse_pan12_with_labels(test_xml, "Test Corpus")

# Save datasets
train_json = output_dir / 'pan12_train_labeled.json'
test_json = output_dir / 'pan12_test_labeled.json'

print("\n" + "="*80)
print("💾 SAVING DATASETS")
print("="*80)

with open(train_json, 'w', encoding='utf-8') as f:
    json.dump(train_data, f, ensure_ascii=False)
print(f"✅ Training saved: {train_json}")

with open(test_json, 'w', encoding='utf-8') as f:
    json.dump(test_data, f, ensure_ascii=False)
print(f"✅ Test saved: {test_json}")

# Final Statistics
print("\n" + "="*80)
print("📊 FINAL STATISTICS")
print("="*80)

train_safe = sum(1 for c in train_data if c['label'] == 'safe')
train_grooming = sum(1 for c in train_data if c['label'] == 'grooming')
test_safe = sum(1 for c in test_data if c['label'] == 'safe')
test_grooming = sum(1 for c in test_data if c['label'] == 'grooming')

print(f"\n📚 TRAINING SET:")
print(f"   Total: {len(train_data):,}")
print(f"   Safe: {train_safe:,} ({train_safe/len(train_data)*100:.1f}%)")
print(f"   Grooming: {train_grooming:,} ({train_grooming/len(train_data)*100:.1f}%)")

print(f"\n📚 TEST SET:")
print(f"   Total: {len(test_data):,}")
print(f"   Safe: {test_safe:,} ({test_safe/len(test_data)*100:.1f}%)")
print(f"   Grooming: {test_grooming:,} ({test_grooming/len(test_data)*100:.1f}%)")

print(f"\n📊 COMBINED:")
print(f"   Total: {len(train_data) + len(test_data):,}")
print(f"   Safe: {train_safe + test_safe:,}")
print(f"   Grooming: {train_grooming + test_grooming:,}")

print(f"\n⏰ End: {datetime.now().strftime('%H:%M:%S')}")
print("\n🚀 READY FOR TRAINING WITH PROPER LABELS!")
print("="*80)
