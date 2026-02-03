#!/usr/bin/env python3
"""
Generate Pre-computed Test Embeddings

Generiert Embeddings für alle Test-Texte EINMAL,
speichert sie als JSON für Android Tests.

Kein ONNX in Tests nötig → Keine Memory-Probleme!
"""

import json
import numpy as np
from pathlib import Path
from sentence_transformers import SentenceTransformer

def generate_test_embeddings():
    print("=" * 70)
    print("SafeSpark - Generate Pre-computed Test Embeddings")
    print("=" * 70)

    # Alle Sätze die in Tests vorkommen - MIT EXAKTEN ZEICHEN!
    TEST_SENTENCES = [
        # Grooming - SUPERVISION_CHECK
        "Bist du alleine?",
        "Bist du allein?",  # Für Similarity-Test (ohne 'e')
        "Bist du alleine zuhause?",
        "Ist heute noch jemand bei dir?",
        "Sind deine Eltern da?",
        "Sind deine Eltern zuhause?",
        "Wann kommen deine Eltern?",
        "Können wir ungestört reden?",
        "Ist jemand in der Nähe?",
        "Are you alone?",
        "Are you home alone?",
        "Are your parents home?",

        # Grooming - SECRECY_REQUEST
        "Sag niemandem davon",
        "Das bleibt unter uns",
        "Das ist unser Geheimnis",
        "Don't tell anyone",  # WICHTIG: Richtiger Apostroph '
        "This is our secret",

        # Grooming - PHOTO_REQUEST
        "Schick mir ein Bild von dir",
        "Schick mir ein Bild",  # Ohne "von dir"
        "Zeig dich mal",
        "Send me a picture",

        # Grooming - MEETING_REQUEST
        "Wollen wir uns mal treffen?",  # WICHTIG: "mal" ist im Test
        "Lass uns mal treffen",
        "Lass uns treffen",  # Ohne "mal"
        "Let's meet up",  # WICHTIG: Richtiger Apostroph '

        # SAFE (sollten NICHT matchen)
        "Bist du müde?",
        "Bist du hungrig?",
        "Bist du fertig?",
        "Bist du heute in der Schule?",
        "Hast du Hausaufgaben?",
        "Wie war dein Tag?",
        "Bock auf Fortnite?",
        "lol",
        "ok",
        "Mama sagt Essen ist fertig",

        # Edge Cases
        "biste allein?",  # Tippfehler
        "Ist mama da?",   # Könnte harmlos sein
    ]

    test_texts = TEST_SENTENCES

    # Load model (use L12 which is already cached)
    MODEL_NAME = "paraphrase-multilingual-MiniLM-L12-v2"
    print(f"\n🔄 Loading model: {MODEL_NAME}")
    model = SentenceTransformer(MODEL_NAME, device='cpu')
    print("✅ Model loaded")

    # Generate embeddings
    print(f"\n🔄 Generating embeddings for {len(test_texts)} test texts...")
    embeddings_dict = {}

    for i, text in enumerate(test_texts, 1):
        embedding = model.encode(text, convert_to_numpy=True)
        embeddings_dict[text] = embedding.tolist()  # Convert to list for JSON
        print(f"   {i}/{len(test_texts)}: '{text[:40]}...' → {len(embedding)} dims")

    # Save to assets
    output_dir = Path(__file__).parent.parent / "app" / "src" / "androidTest" / "assets"
    output_dir.mkdir(parents=True, exist_ok=True)
    output_file = output_dir / "test_embeddings.json"

    # Create JSON structure
    output_data = {
        "model": MODEL_NAME,
        "embedding_dim": len(next(iter(embeddings_dict.values()))),
        "embeddings": embeddings_dict
    }

    with open(output_file, 'w', encoding='utf-8') as f:
        json.dump(output_data, f, indent=2)

    size_mb = output_file.stat().st_size / (1024 * 1024)
    print(f"\n✅ Saved to: {output_file}")
    print(f"📦 Size: {size_mb:.2f} MB")
    print(f"📊 {len(test_texts)} test embeddings")

    return True

if __name__ == "__main__":
    success = generate_test_embeddings()

    if success:
        print("\n" + "=" * 70)
        print("✅ SUCCESS! Test embeddings ready!")
        print("=" * 70)
        print("\nTests können jetzt ohne ONNX laufen:")
        print("- Keine Memory-Probleme")
        print("- Schneller (pre-computed)")
        print("- Alle 45 Tests sollten bestehen!")
