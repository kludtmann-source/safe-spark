#!/usr/bin/env python3
"""
Quick Similarity Test - Verifiziert Sentence Encoder Scores

Testet ob die Similarity-Scores für Grooming-Patterns korrekt sind.
"""

from sentence_transformers import SentenceTransformer
import numpy as np

print("=" * 60)
print("SafeSpark - Similarity Score Verification")
print("=" * 60)

# Load model
print("\n🔄 Loading model...")
model = SentenceTransformer('paraphrase-multilingual-MiniLM-L12-v2', device='cpu')
print("✅ Model loaded")

# Test cases
test_cases = [
    # Should be HIGH similarity (positive cases)
    ("Bist du alleine?", "Ist heute noch jemand bei dir?", "SUPERVISION_CHECK", 0.60),
    ("Bist du alleine?", "Sind deine Eltern da?", "SUPERVISION_CHECK", 0.60),
    ("Are you alone?", "Bist du alleine?", "MULTILINGUAL", 0.70),
    ("Sag niemandem davon", "Das bleibt unter uns", "SECRECY_REQUEST", 0.50),
    ("Schick mir ein Bild", "Send me a picture", "PHOTO_REQUEST", 0.80),

    # Should be LOW similarity (negative cases)
    ("Bist du alleine?", "Bist du müde?", "FALSE_POSITIVE", None),
    ("Bist du alleine?", "Wie geht es dir?", "FALSE_POSITIVE", None),
    ("Sind deine Eltern da?", "Was machst du gerade?", "FALSE_POSITIVE", None),
]

print("\n" + "=" * 60)
print("Testing Similarity Scores")
print("=" * 60)

def cosine_similarity(a, b):
    return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))

passed = 0
failed = 0

for text1, text2, intent, min_score in test_cases:
    # Encode
    emb1 = model.encode(text1, convert_to_numpy=True)
    emb2 = model.encode(text2, convert_to_numpy=True)

    # Calculate similarity
    similarity = cosine_similarity(emb1, emb2)

    # Check expectation
    if min_score is not None:
        # Should be high similarity
        if similarity >= min_score:
            status = "✅ PASS"
            passed += 1
        else:
            status = f"❌ FAIL (expected >= {min_score:.2f})"
            failed += 1
    else:
        # Should be low similarity (< 0.60)
        if similarity < 0.60:
            status = "✅ PASS"
            passed += 1
        else:
            status = f"❌ FAIL (expected < 0.60)"
            failed += 1

    print(f"\n{status} [{intent}]")
    print(f"  '{text1}'")
    print(f"  vs '{text2}'")
    print(f"  → Similarity: {similarity:.3f} ({int(similarity*100)}%)")

print("\n" + "=" * 60)
print(f"Results: {passed} passed, {failed} failed")
print("=" * 60)

# Detailed analysis for SUPERVISION_CHECK
print("\n" + "=" * 60)
print("SUPERVISION_CHECK - Detailed Analysis")
print("=" * 60)

supervision_seeds = [
    "Bist du alleine?",
    "Bist du alleine zuhause?",
    "Ist jemand bei dir?",
    "Ist heute noch jemand bei dir?",
    "Sind deine Eltern da?",
    "Sind deine Eltern zuhause?",
    "Are you alone?",
    "Are your parents home?",
]

test_text = "Ist heute noch jemand bei dir?"
print(f"\nTest Text: '{test_text}'")
print("\nSimilarity with seeds:")

test_emb = model.encode(test_text, convert_to_numpy=True)
similarities = []

for seed in supervision_seeds:
    seed_emb = model.encode(seed, convert_to_numpy=True)
    sim = cosine_similarity(test_emb, seed_emb)
    similarities.append((seed, sim))

# Sort by similarity
similarities.sort(key=lambda x: x[1], reverse=True)

for seed, sim in similarities:
    emoji = "✅" if sim > 0.75 else "⚠️" if sim > 0.60 else "❌"
    print(f"  {emoji} {sim:.3f} ({int(sim*100):2d}%) - '{seed}'")

max_sim = max(s[1] for s in similarities)
print(f"\n🎯 Max Similarity: {max_sim:.3f} ({int(max_sim*100)}%)")
print(f"   Threshold: 0.750 (75%)")

if max_sim >= 0.75:
    print("   ✅ WOULD BE DETECTED by Semantic Detector!")
elif max_sim >= 0.60:
    print("   ⚠️ MAYBE detected (close to threshold)")
else:
    print("   ❌ Would NOT be detected")

print("\n" + "=" * 60)
print("✅ Verification Complete!")
print("=" * 60)

if failed == 0:
    print("\n✅ All similarity scores are correct!")
    print("   The semantic detection will work as expected.")
else:
    print(f"\n⚠️ {failed} test(s) failed - review thresholds!")
