"""
KidGuard ML Training - Phase 1: Synthetische Daten Generierung
================================================================

Generiert 2000 synthetische Chat-Beispiele für:
- Safe: Hausaufgaben, Gaming, Sport, Freunde, Familie
- Toxic: Beleidigungen, Scam, Druck, Aggression

Ziel: Basis-Modell das normales Rauschen von aggressivem Tonfall unterscheidet
"""

import json
import random
from typing import List, Tuple

# ============================================================================
# PHASE 1: SYNTHETISCHE DATEN - "Safe" vs "Toxic"
# ============================================================================

SAFE_TEMPLATES = {
    "hausaufgaben": [
        "Hast du die Mathe-Hausaufgaben schon gemacht?",
        "Können wir zusammen für die Arbeit lernen?",
        "Welche Seite müssen wir lesen?",
        "Ich versteh die Aufgabe nicht, kannst du mir helfen?",
        "Wann ist die Bio-Klausur?",
        "Hast du die Lösung für Aufgabe 3?",
        "Treffen wir uns in der Bibliothek?",
    ],
    "gaming": [
        "Zockst du heute Abend?",
        "Welches Level bist du?",
        "Hast du das neue Update gesehen?",
        "Können wir zusammen spielen?",
        "Mein Team ist so schlecht heute",
        "Ich bin Level 50 jetzt!",
        "Hast du Discord?",
    ],
    "sport": [
        "Kommst du zum Training?",
        "Wer hat das Spiel gewonnen?",
        "Gehst du ins Fitnessstudio?",
        "Hast du die WM gesehen?",
        "Ich hab einen neuen Rekord!",
        "Spielst du Fußball?",
    ],
    "alltag": [
        "Hey wie geht's?",
        "Was machst du heute?",
        "Gute Nacht",
        "Bis morgen",
        "Ich bin müde",
        "Was gibt's zu essen?",
        "Wie war dein Tag?",
    ],
    "freunde": [
        "Treffen wir uns später?",
        "Kommst du zur Party?",
        "Wollen wir ins Kino?",
        "Hast du Lust rauszugehen?",
        "Ich bin krank heute",
        "Können wir telefonieren?",
    ]
}

TOXIC_TEMPLATES = {
    "beleidigung": [
        "Du bist so dumm",
        "Du Loser",
        "Du bist hässlich",
        "Niemand mag dich",
        "Du bist so peinlich",
        "Du Versager",
        "Halt die Fresse",
    ],
    "scam": [
        "Ich brauch dein Passwort",
        "Schick mir 50 Euro",
        "Gib mir deine Handynummer",
        "Ich hab nen Trick für gratis Geld",
        "Klick auf diesen Link für Rewards",
        "Schick mir deine Login-Daten",
    ],
    "druck": [
        "Wenn du nicht machst verlier ich Respekt",
        "Alle anderen machen das auch",
        "Du bist kein echter Freund wenn nicht",
        "Stell dich nicht so an",
        "Du bist so ein Angsthase",
        "Traust du dich nicht?",
    ],
    "aggression": [
        "Ich mach dich fertig",
        "Pass auf was du sagst",
        "Ich warte nach der Schule auf dich",
        "Du bist tot",
        "Ich hol meine Jungs",
    ]
}

# ============================================================================
# PHASE 2: LINGUISTISCHE HEURISTIK - "Six Stages of Grooming"
# ============================================================================

GROOMING_PATTERNS = {
    "vertrauen_aufbau": [
        "Du kannst mir alles erzählen",
        "Ich verstehe dich so gut",
        "Niemand versteht dich wie ich",
        "Du bist so reif für dein Alter",
        "Wir sind uns so ähnlich",
    ],
    "geheimnisse": [
        "Das bleibt unser Geheimnis okay?",
        "Sag niemandem davon",
        "Deine Eltern müssen das nicht wissen",
        "Das ist nur zwischen uns",
        "Verrat mich nicht",
    ],
    "geschenke": [
        "Ich schick dir was Geld",
        "Willst du ein Geschenk?",
        "Ich kauf dir was Schönes",
        "Ich geb dir Guthaben",
        "Willst du nen neuen Skin?",
    ],
    "isolation": [
        "Deine Freunde sind fake",
        "Nur ich bin echt zu dir",
        "Die verstehen dich nicht",
        "Du brauchst nur mich",
    ],
    "sexualisierung": [
        "Bist du schon mal verliebt gewesen?",
        "Hast du schon nen Freund/Freundin?",
        "Schick mir ein Bild von dir",
        "Zeig mir wie du aussiehst",
        "Du bist bestimmt hübsch",
    ],
    "treffen": [
        "Lass uns mal treffen",
        "Ich hol dich ab",
        "Komm zu mir",
        "Wo wohnst du?",
        "Bist du allein zuhause?",
    ]
}

# ============================================================================
# DATEN GENERIERUNG
# ============================================================================

def generate_safe_examples(count: int) -> List[Tuple[str, int, str]]:
    """Generiert sichere Chat-Beispiele"""
    examples = []
    categories = list(SAFE_TEMPLATES.keys())

    for _ in range(count):
        category = random.choice(categories)
        template = random.choice(SAFE_TEMPLATES[category])

        # Variationen hinzufügen
        variations = [
            template,
            template + " 😊",
            template + " lol",
            template + "?",
            template.lower(),
        ]

        text = random.choice(variations)
        examples.append((text, 0, "safe_" + category))

    return examples

def generate_toxic_examples(count: int) -> List[Tuple[str, int, str]]:
    """Generiert toxische Chat-Beispiele"""
    examples = []
    categories = list(TOXIC_TEMPLATES.keys())

    for _ in range(count):
        category = random.choice(categories)
        template = random.choice(TOXIC_TEMPLATES[category])

        # Variationen
        variations = [
            template,
            template + "!",
            template + "!!",
            template.upper(),
        ]

        text = random.choice(variations)
        examples.append((text, 1, "toxic_" + category))

    return examples

def generate_grooming_examples(count: int) -> List[Tuple[str, int, str]]:
    """Generiert Grooming-Pattern Beispiele (Phase 2)"""
    examples = []
    patterns = list(GROOMING_PATTERNS.keys())

    for _ in range(count):
        pattern = random.choice(patterns)
        template = random.choice(GROOMING_PATTERNS[pattern])

        # Grooming ist subtiler - oft harmlos klingend
        text = template
        examples.append((text, 2, "grooming_" + pattern))  # Label 2 = Grooming

    return examples

def generate_context_sequences(count: int) -> List[dict]:
    """
    Generiert Gesprächsverläufe (Sliding Window)
    Simuliert 3-5 Nachrichten Kontext
    """
    sequences = []

    for _ in range(count):
        # Safe Sequence
        if random.random() < 0.4:
            length = random.randint(3, 5)
            messages = []
            for _ in range(length):
                cat = random.choice(list(SAFE_TEMPLATES.keys()))
                msg = random.choice(SAFE_TEMPLATES[cat])
                messages.append(msg)

            sequences.append({
                "messages": messages,
                "label": 0,
                "category": "safe_conversation"
            })

        # Toxic Sequence
        elif random.random() < 0.7:
            length = random.randint(2, 4)
            messages = []
            # Start harmlos
            messages.append(random.choice(SAFE_TEMPLATES["alltag"]))
            # Wird toxisch
            for _ in range(length - 1):
                cat = random.choice(list(TOXIC_TEMPLATES.keys()))
                msg = random.choice(TOXIC_TEMPLATES[cat])
                messages.append(msg)

            sequences.append({
                "messages": messages,
                "label": 1,
                "category": "toxic_escalation"
            })

        # Grooming Sequence (subtil!)
        else:
            messages = []
            # Start: Vertrauen aufbauen
            messages.append(random.choice(GROOMING_PATTERNS["vertrauen_aufbau"]))
            # Geschenk anbieten
            messages.append(random.choice(GROOMING_PATTERNS["geschenke"]))
            # Geheimnis fordern
            messages.append(random.choice(GROOMING_PATTERNS["geheimnisse"]))
            # Treffen vorschlagen
            if random.random() < 0.5:
                messages.append(random.choice(GROOMING_PATTERNS["treffen"]))

            sequences.append({
                "messages": messages,
                "label": 2,
                "category": "grooming_sequence"
            })

    return sequences

# ============================================================================
# EXPORT
# ============================================================================

def generate_training_data(total_examples: int = 2000):
    """
    Generiert komplettes Training-Dataset

    Verteilung:
    - 40% Safe (800)
    - 30% Toxic (600)
    - 20% Grooming Single (400)
    - 10% Context Sequences (200)
    """

    print("🚀 Generiere Training-Daten...")
    print(f"📊 Ziel: {total_examples} Beispiele\n")

    # Single-Message Beispiele
    safe = generate_safe_examples(int(total_examples * 0.4))
    toxic = generate_toxic_examples(int(total_examples * 0.3))
    grooming = generate_grooming_examples(int(total_examples * 0.2))

    # Context Sequences
    sequences = generate_context_sequences(int(total_examples * 0.1))

    # Kombiniere zu Single-Message Dataset
    single_messages = []
    for text, label, category in safe + toxic + grooming:
        single_messages.append({
            "text": text,
            "label": label,
            "category": category
        })

    # Shuffle
    random.shuffle(single_messages)
    random.shuffle(sequences)

    # Stats
    safe_count = sum(1 for m in single_messages if m["label"] == 0)
    toxic_count = sum(1 for m in single_messages if m["label"] == 1)
    grooming_count = sum(1 for m in single_messages if m["label"] == 2)

    print(f"✅ Generierung abgeschlossen!\n")
    print(f"📊 Single Messages: {len(single_messages)}")
    print(f"   - Safe: {safe_count} ({safe_count/len(single_messages)*100:.1f}%)")
    print(f"   - Toxic: {toxic_count} ({toxic_count/len(single_messages)*100:.1f}%)")
    print(f"   - Grooming: {grooming_count} ({grooming_count/len(single_messages)*100:.1f}%)")
    print(f"\n📊 Context Sequences: {len(sequences)}")

    return single_messages, sequences

def save_to_json(data, sequences, output_dir="data"):
    """Speichert Daten als JSON"""
    import os
    os.makedirs(output_dir, exist_ok=True)

    # Single Messages
    with open(f"{output_dir}/training_data.json", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

    # Sequences
    with open(f"{output_dir}/training_sequences.json", "w", encoding="utf-8") as f:
        json.dump(sequences, f, ensure_ascii=False, indent=2)

    print(f"\n💾 Gespeichert in:")
    print(f"   - {output_dir}/training_data.json")
    print(f"   - {output_dir}/training_sequences.json")

# ============================================================================
# MAIN
# ============================================================================

if __name__ == "__main__":
    # Generiere 2000 Beispiele
    single_messages, sequences = generate_training_data(2000)

    # Speichern
    save_to_json(single_messages, sequences, "data")

    print("\n✅ Phase 1 Datengenerierung abgeschlossen!")
    print("📦 Nächster Schritt: train_model.py ausführen")
