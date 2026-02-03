#!/usr/bin/env python3
"""Check Training Data Labels"""

import json
from collections import Counter
from pathlib import Path

print("="*60)
print("🔍 CHECKING TRAINING DATA LABELS")
print("="*60)

# Load data
data_path = Path.home() / 'AndroidStudioProjects/KidGuard/training/data/pan12_full/pan12_train_full.json'

print(f"\n📂 Loading: {data_path}")

with open(data_path, 'r') as f:
    data = json.load(f)

print(f"✅ Loaded: {len(data):,} samples")

# Check labels
labels = [item['label'] for item in data]
label_counts = Counter(labels)

print(f"\n📊 Label Distribution:")
for label, count in label_counts.most_common():
    pct = count / len(labels) * 100
    print(f"   {label:20s}: {count:7,} ({pct:5.2f}%)")

# Check first few samples
print(f"\n📝 First 3 samples:")
for i, sample in enumerate(data[:3]):
    text_preview = sample['text'][:100] + "..." if len(sample['text']) > 100 else sample['text']
    print(f"\n   Sample {i+1}:")
    print(f"   Label: {sample['label']}")
    print(f"   Text: {text_preview}")

print("\n" + "="*60)
print("⚠️  PROBLEM: Alle Labels sind STAGE_SAFE!")
print("    Der Parser hat keine Grooming-Labels zugewiesen.")
print("    Wir müssen die PAN12 Predator-IDs nutzen!")
print("="*60)
