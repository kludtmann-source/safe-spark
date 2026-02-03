#!/usr/bin/env python3
"""
Kombiniert alle Datasets für maximales Training:
1. PAN12 XML Dialoge (neu geparst)
2. Bestehendes deutsches Dataset
3. Augmentierte Daten
"""

import json
import random
from collections import Counter
import os

print("🔄 Kombiniere alle Datasets")
print("=" * 60)

# Lade alle verfügbaren Datasets
datasets = []

# 1. PAN12 XML Dialoge
pan12_xml = 'data/pan12_dialogs_extracted.json'
if os.path.exists(pan12_xml):
    with open(pan12_xml, 'r', encoding='utf-8') as f:
        pan12_data = json.load(f)
    print(f"✅ PAN12 XML: {len(pan12_data)} samples")
    datasets.append(('pan12_xml', pan12_data))
else:
    print(f"⚠️ PAN12 XML nicht gefunden: {pan12_xml}")

# 2. Deutsches Dataset
german_train = 'data/combined/kidguard_german_train.json'
if os.path.exists(german_train):
    with open(german_train, 'r', encoding='utf-8') as f:
        german_data = json.load(f)
    print(f"✅ Deutsches Dataset: {len(german_data)} samples")

    # Konvertiere Format wenn nötig (binary label → stage label)
    formatted_german = []
    for item in german_data:
        if 'label' not in item or isinstance(item['label'], int):
            # Binary label (0/1) → stage label
            if item.get('label', 0) == 0:
                item['label'] = 'STAGE_SAFE'
            else:
                # Wenn wir den Text analysieren können, besseres Label
                text_lower = item['text'].lower()
                if any(kw in text_lower for kw in ['robux', 'vbucks', 'geld', 'geschenk']):
                    item['label'] = 'STAGE_NEEDS'
                elif any(kw in text_lower for kw in ['allein', 'alone', 'zuhause']):
                    item['label'] = 'STAGE_ASSESSMENT'
                elif any(kw in text_lower for kw in ['discord', 'snapchat', 'privat']):
                    item['label'] = 'STAGE_ISOLATION'
                else:
                    item['label'] = 'STAGE_TRUST'
        formatted_german.append(item)

    datasets.append(('german', formatted_german))
else:
    print(f"⚠️ Deutsches Dataset nicht gefunden: {german_train}")

# 3. Original kombiniertes Dataset (falls vorhanden)
original_train = 'data/combined/kidguard_train.json'
if os.path.exists(original_train):
    with open(original_train, 'r', encoding='utf-8') as f:
        original_data = json.load(f)
    print(f"✅ Original Dataset: {len(original_data)} samples")
    datasets.append(('original', original_data))

# Kombiniere alle
print("\n🔀 Kombiniere Datasets...")
all_samples = []

for name, data in datasets:
    all_samples.extend(data)

print(f"✅ Gesamt vor Deduplizierung: {len(all_samples)} samples")

# Dedupliziere basierend auf Text
seen_texts = set()
unique_samples = []

for sample in all_samples:
    text = sample['text'].strip().lower()
    if text not in seen_texts and len(text) >= 10:
        seen_texts.add(text)
        unique_samples.append(sample)

print(f"✅ Nach Deduplizierung: {len(unique_samples)} samples")

# Label Distribution
labels = [s.get('label', 'STAGE_SAFE') for s in unique_samples]
label_counts = Counter(labels)

print("\n📊 Label Distribution:")
for label, count in sorted(label_counts.items()):
    print(f"   {label}: {count} ({count/len(unique_samples)*100:.1f}%)")

# Shuffle
random.seed(42)
random.shuffle(unique_samples)

# Split 80/20
split_idx = int(len(unique_samples) * 0.8)
train_samples = unique_samples[:split_idx]
test_samples = unique_samples[split_idx:]

print(f"\n📋 Split:")
print(f"   Training: {len(train_samples)} samples")
print(f"   Test: {len(test_samples)} samples")

# Speichern
os.makedirs('data/combined', exist_ok=True)

# Training Set
output_train = 'data/combined/kidguard_ultimate_train.json'
with open(output_train, 'w', encoding='utf-8') as f:
    json.dump(train_samples, f, indent=2, ensure_ascii=False)

print(f"\n💾 Training gespeichert: {output_train}")
print(f"   Größe: {os.path.getsize(output_train) / 1024:.1f} KB")

# Test Set
output_test = 'data/combined/kidguard_ultimate_test.json'
with open(output_test, 'w', encoding='utf-8') as f:
    json.dump(test_samples, f, indent=2, ensure_ascii=False)

print(f"💾 Test gespeichert: {output_test}")
print(f"   Größe: {os.path.getsize(output_test) / 1024:.1f} KB")

# Auch Binary Version erstellen (für bessere Kompatibilität)
print("\n🔄 Erstelle Binary Version...")

binary_train = []
binary_test = []

for sample in train_samples:
    binary_sample = sample.copy()
    # Safe = 0, alle anderen = 1
    binary_sample['label'] = 0 if sample.get('label') == 'STAGE_SAFE' else 1
    binary_train.append(binary_sample)

for sample in test_samples:
    binary_sample = sample.copy()
    binary_sample['label'] = 0 if sample.get('label') == 'STAGE_SAFE' else 1
    binary_test.append(binary_sample)

# Binary Training Distribution
binary_labels = [s['label'] for s in binary_train]
binary_counts = Counter(binary_labels)
print(f"\n📊 Binary Labels (Training):")
print(f"   Safe (0): {binary_counts[0]} ({binary_counts[0]/len(binary_train)*100:.1f}%)")
print(f"   Grooming (1): {binary_counts[1]} ({binary_counts[1]/len(binary_train)*100:.1f}%)")

# Speichern Binary
output_binary_train = 'data/combined/kidguard_ultimate_binary_train.json'
with open(output_binary_train, 'w', encoding='utf-8') as f:
    json.dump(binary_train, f, indent=2, ensure_ascii=False)

output_binary_test = 'data/combined/kidguard_ultimate_binary_test.json'
with open(output_binary_test, 'w', encoding='utf-8') as f:
    json.dump(binary_test, f, indent=2, ensure_ascii=False)

print(f"✅ Binary Training: {output_binary_train}")
print(f"✅ Binary Test: {output_binary_test}")

print("\n" + "=" * 60)
print("🎉 DATASET KOMBINATION ABGESCHLOSSEN!")
print("=" * 60)
print(f"\n📊 Finale Statistik:")
print(f"   Total Samples: {len(unique_samples)}")
print(f"   Training: {len(train_samples)}")
print(f"   Test: {len(test_samples)}")
print(f"   Datasets kombiniert: {len(datasets)}")
print(f"\n🚀 Nutze für Training:")
print(f"   {output_binary_train}")
print(f"   {output_binary_test}")
