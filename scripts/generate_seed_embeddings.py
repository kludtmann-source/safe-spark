#!/usr/bin/env python3
"""
Generate Seed Embeddings for Semantic Grooming Detection

Encodes all seed patterns using sentence-transformers and saves them
as JSON for Android app consumption.

Requirements:
    pip install sentence-transformers torch numpy

Usage:
    python generate_seed_embeddings.py

Output:
    ../app/src/main/assets/seed_embeddings.json
"""

import json
import numpy as np
from pathlib import Path
from sentence_transformers import SentenceTransformer
from typing import Dict, List

# Seed patterns for each intent
SUPERVISION_CHECK_SEEDS = [
    # Deutsch
    "Bist du alleine?",
    "Bist du alleine zuhause?",
    "Ist jemand bei dir?",
    "Ist heute noch jemand bei dir?",
    "Sind deine Eltern da?",
    "Sind deine Eltern zuhause?",
    "Wann kommen deine Eltern nach Hause?",
    "Wann kommen deine Eltern wieder?",
    "Ist deine Mama da?",
    "Ist dein Papa da?",
    "Passt jemand auf dich auf?",
    "Hast du das Haus für dich?",
    "Bist du gerade ungestört?",
    "Können wir ungestört reden?",
    "Kann jemand mitlesen?",
    "Ist jemand in der Nähe?",
    "Bist du gerade allein?",
    "Bist du unbeaufsichtigt?",
    # Englisch
    "Are you alone?",
    "Are you home alone?",
    "Is anyone there with you?",
    "Are your parents home?",
    "When do your parents get back?",
    "Can anyone see your messages?",
    "Is someone watching you?",
    "Are you by yourself?",
]

SECRECY_REQUEST_SEEDS = [
    # Deutsch
    "Sag niemandem davon",
    "Das bleibt unter uns",
    "Erzähl das nicht deinen Eltern",
    "Das ist unser Geheimnis",
    "Versprich mir dass du nichts sagst",
    "Behalte das für dich",
    "Niemand darf das wissen",
    "Erzähl das bitte niemand weiter",
    "Das muss geheim bleiben",
    "Kein Wort zu anderen",
    "Deine Eltern dürfen das nicht erfahren",
    "Verrat mich nicht",
    # Englisch
    "Don't tell anyone",
    "This is our secret",
    "Keep this between us",
    "Promise you won't tell",
    "Don't tell your parents",
    "This stays between us",
    "Nobody can know about this",
    "You can't tell anyone",
    "Keep this to yourself",
]

PHOTO_REQUEST_SEEDS = [
    # Deutsch
    "Schick mir ein Bild von dir",
    "Ich will sehen wie du aussiehst",
    "Hast du Fotos von dir?",
    "Zeig dich mal",
    "Schick mal ein Selfie",
    "Kannst du mir ein Foto schicken?",
    "Ich würde dich gerne sehen",
    "Mach mal ein Bild",
    "Zeig mir wie du aussiehst",
    "Hast du aktuelle Bilder?",
    "Wie siehst du aus?",
    # Englisch
    "Send me a picture",
    "Show me what you look like",
    "Can I see you?",
    "Send a selfie",
    "Do you have pictures?",
    "I want to see a photo of you",
    "Can you send a pic?",
    "Show yourself",
]

MEETING_REQUEST_SEEDS = [
    # Deutsch
    "Wollen wir uns mal treffen?",
    "Können wir uns sehen?",
    "Lass uns mal treffen",
    "Wir sollten uns treffen",
    "Komm doch mal vorbei",
    "Ich würde dich gerne treffen",
    "Lass uns persönlich reden",
    "Wann können wir uns sehen?",
    "Treffen wir uns?",
    "Ich hole dich ab",
    # Englisch
    "Let's meet up",
    "Can we meet?",
    "I want to see you in person",
    "We should meet",
    "Come meet me",
    "When can we meet?",
    "Let's meet in person",
    "I'll pick you up",
]

# All intents with their seeds
INTENTS = {
    "SUPERVISION_CHECK": SUPERVISION_CHECK_SEEDS,
    "SECRECY_REQUEST": SECRECY_REQUEST_SEEDS,
    "PHOTO_REQUEST": PHOTO_REQUEST_SEEDS,
    "MEETING_REQUEST": MEETING_REQUEST_SEEDS,
}


def generate_embeddings(model_name: str = "paraphrase-multilingual-MiniLM-L6-v2") -> Dict:
    """
    Generate embeddings for all seed patterns.

    Args:
        model_name: Name of the sentence-transformers model

    Returns:
        Dictionary with model info and embeddings
    """
    print(f"🔄 Loading model: {model_name}")
    model = SentenceTransformer(model_name)

    # Get embedding dimension
    embedding_dim = model.get_sentence_embedding_dimension()
    print(f"✅ Model loaded (embedding_dim={embedding_dim})")

    result = {
        "model": model_name,
        "embedding_dim": embedding_dim,
        "version": "1.0.0",
        "generated_at": str(np.datetime64('now')),
        "intents": {}
    }

    # Generate embeddings for each intent
    for intent_name, seeds in INTENTS.items():
        print(f"\n🔄 Encoding {intent_name} ({len(seeds)} seeds)...")

        # Encode all seeds at once (faster)
        embeddings = model.encode(seeds, show_progress_bar=True, convert_to_numpy=True)

        # Convert to list for JSON serialization
        embeddings_list = embeddings.tolist()

        result["intents"][intent_name] = {
            "seeds": seeds,
            "embeddings": embeddings_list,
            "count": len(seeds)
        }

        print(f"✅ {intent_name}: {len(seeds)} embeddings generated")

    return result


def save_embeddings(data: Dict, output_path: Path):
    """Save embeddings to JSON file."""
    output_path.parent.mkdir(parents=True, exist_ok=True)

    print(f"\n💾 Saving to: {output_path}")
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    # Print file size
    size_mb = output_path.stat().st_size / (1024 * 1024)
    print(f"✅ Saved! File size: {size_mb:.2f} MB")


def verify_embeddings(data: Dict):
    """Verify embeddings are valid."""
    print("\n🔍 Verifying embeddings...")

    embedding_dim = data["embedding_dim"]
    total_embeddings = 0

    for intent_name, intent_data in data["intents"].items():
        seeds = intent_data["seeds"]
        embeddings = intent_data["embeddings"]

        assert len(seeds) == len(embeddings), f"{intent_name}: seed/embedding count mismatch"

        for i, emb in enumerate(embeddings):
            assert len(emb) == embedding_dim, f"{intent_name}[{i}]: wrong dimension"
            assert all(isinstance(x, (int, float)) for x in emb), f"{intent_name}[{i}]: invalid values"

        total_embeddings += len(embeddings)

    print(f"✅ Verification passed!")
    print(f"   Total intents: {len(data['intents'])}")
    print(f"   Total embeddings: {total_embeddings}")
    print(f"   Embedding dimension: {embedding_dim}")


def print_similarity_examples(model_name: str):
    """Print some example similarities for testing."""
    print("\n📊 Testing similarity examples...")
    model = SentenceTransformer(model_name)

    test_cases = [
        ("Bist du alleine?", "Ist heute noch jemand bei dir?"),
        ("Bist du alleine?", "Bist du müde?"),
        ("Sag niemandem davon", "Das bleibt unter uns"),
        ("Schick mir ein Bild", "Send me a picture"),
    ]

    for text1, text2 in test_cases:
        emb1 = model.encode(text1)
        emb2 = model.encode(text2)

        # Cosine similarity
        similarity = np.dot(emb1, emb2) / (np.linalg.norm(emb1) * np.linalg.norm(emb2))

        print(f"\n'{text1}'")
        print(f"  vs '{text2}'")
        print(f"  → Similarity: {similarity:.3f}")


def main():
    print("=" * 60)
    print("SafeSpark - Seed Embedding Generator")
    print("=" * 60)

    # Model to use
    model_name = "paraphrase-multilingual-MiniLM-L6-v2"

    # Output path
    script_dir = Path(__file__).parent
    output_path = script_dir.parent / "app" / "src" / "main" / "assets" / "seed_embeddings.json"

    # Generate embeddings
    data = generate_embeddings(model_name)

    # Verify
    verify_embeddings(data)

    # Save
    save_embeddings(data, output_path)

    # Test examples
    print_similarity_examples(model_name)

    print("\n" + "=" * 60)
    print("✅ Done! Embeddings ready for Android app.")
    print("=" * 60)


if __name__ == "__main__":
    main()
