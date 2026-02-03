#!/usr/bin/env python3
"""
PAN12 Full Corpus Parser
=========================
Extrahiert ALLE Conversations aus den PAN12 XML-Dateien
für umfassendes Training mit 66,927+ Conversations.
"""

import xml.etree.ElementTree as ET
import json
import sys
from pathlib import Path
from collections import Counter
from datetime import datetime

print("="*80)
print("🚀 PAN12 FULL CORPUS PARSER")
print("="*80)
print(f"Start: {datetime.now().strftime('%H:%M:%S')}\n")

# Paths
base_path = Path.home() / 'AndroidStudioProjects' / 'KidGuard'
train_xml = base_path / 'pan12-sexual-predator-identification-training-corpus-2012-05-01.xml'
test_xml = base_path / 'pan12-sexual-predator-identification-test-corpus-2012-05-17.xml'
output_dir = base_path / 'training' / 'data' / 'pan12_full'
output_dir.mkdir(parents=True, exist_ok=True)

def parse_pan12_xml(xml_path, dataset_name):
    """Parse PAN12 XML und extrahiere alle Conversations"""

    print(f"📖 Parsing {dataset_name}...")
    print(f"   File: {xml_path.name}")

    if not xml_path.exists():
        print(f"❌ File not found: {xml_path}")
        return []

    # Parse XML
    tree = ET.parse(str(xml_path))
    root = tree.getroot()

    conversations = []
    total_messages = 0

    # Extract conversations
    for conv in root.findall('.//conversation'):
        conv_id = conv.get('id', 'unknown')
        messages = []

        for msg in conv.findall('.//message'):
            author_elem = msg.find('author')
            text_elem = msg.find('text')
            time_elem = msg.find('time')

            if text_elem is not None and text_elem.text:
                author = author_elem.text.strip() if author_elem is not None and author_elem.text else 'unknown'
                text = text_elem.text.strip()
                time = time_elem.text.strip() if time_elem is not None and time_elem.text else ''

                if text:  # Non-empty
                    messages.append({
                        'author': author,
                        'text': text,
                        'time': time
                    })
                    total_messages += 1

        if len(messages) >= 2:  # Min 2 messages
            # Concatenate conversation
            full_text = ' '.join([m['text'] for m in messages])

            conversations.append({
                'text': full_text,
                'label': 'STAGE_SAFE',  # Default, wird später klassifiziert
                'conversation_id': conv_id,
                'num_messages': len(messages),
                'source': 'pan12'
            })

        if len(conversations) % 5000 == 0 and len(conversations) > 0:
            print(f"   Progress: {len(conversations):,} conversations, {total_messages:,} messages...")

    print(f"✅ Extracted {len(conversations):,} conversations with {total_messages:,} messages")
    return conversations

# Parse Training Data
print("\n" + "="*80)
print("📚 TRAINING DATA")
print("="*80)
train_data = parse_pan12_xml(train_xml, "Training Corpus")

# Parse Test Data
print("\n" + "="*80)
print("📚 TEST DATA")
print("="*80)
test_data = parse_pan12_xml(test_xml, "Test Corpus")

# Save datasets
train_json = output_dir / 'pan12_train_full.json'
test_json = output_dir / 'pan12_test_full.json'

print("\n" + "="*80)
print("💾 SAVING DATASETS")
print("="*80)

with open(train_json, 'w', encoding='utf-8') as f:
    json.dump(train_data, f, indent=2, ensure_ascii=False)
print(f"✅ Training saved: {train_json}")
print(f"   Size: {train_json.stat().st_size / 1024 / 1024:.1f} MB")

with open(test_json, 'w', encoding='utf-8') as f:
    json.dump(test_data, f, indent=2, ensure_ascii=False)
print(f"✅ Test saved: {test_json}")
print(f"   Size: {test_json.stat().st_size / 1024 / 1024:.1f} MB")

# Statistics
print("\n" + "="*80)
print("📊 FINAL STATISTICS")
print("="*80)
print(f"\n📚 TRAINING SET:")
print(f"   Conversations: {len(train_data):,}")
print(f"   Avg messages/conv: {sum(c['num_messages'] for c in train_data) / len(train_data):.1f}")

print(f"\n📚 TEST SET:")
print(f"   Conversations: {len(test_data):,}")
print(f"   Avg messages/conv: {sum(c['num_messages'] for c in test_data) / len(test_data):.1f}")

print(f"\n📊 TOTAL:")
print(f"   Conversations: {len(train_data) + len(test_data):,}")

print(f"\n⏰ End: {datetime.now().strftime('%H:%M:%S')}")
print("\n🚀 READY FOR FULL TRAINING!")
print("="*80)
