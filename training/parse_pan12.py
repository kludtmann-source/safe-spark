#!/usr/bin/env python3
"""
PAN12 XML Parser für KidGuard
Extrahiert Text + Labels aus PAN12 XML und weist heuristisch Grooming-Stages zu
"""

import xml.etree.ElementTree as ET
import pandas as pd
import json
from pathlib import Path
from datetime import datetime
import re
from typing import List, Dict, Tuple

class PAN12Parser:
    """
    Parser für PAN12 Sexual Predator Identification Dataset

    Extrahiert:
    1. Conversation-Texte
    2. Author Labels (Predator/Victim)
    3. Heuristische Stage-Zuweisung basierend auf Content

    Grooming Stages (Six Stages of Grooming):
    - STAGE_SAFE: Normale Konversation
    - STAGE_TRUST: Vertrauensaufbau ("Du bist besonders", "Ich versteh dich")
    - STAGE_ISOLATION: Isolation ("Sag niemandem", "Unser Geheimnis")
    - STAGE_NEEDS: Materielle Anreize ("Ich schenk dir", "Brauchst du Geld?")
    - STAGE_ASSESSMENT: Umgebungs-Check ("Bist du allein?", "Wo sind deine Eltern?")
    """

    def __init__(self):
        self.conversations = []
        self.predator_ids = set()

        # Heuristische Stage-Detection Keywords
        self.stage_patterns = {
            'STAGE_TRUST': [
                r'\b(special|besonders|unique|einzigartig)\b',
                r'\b(understand|versteh)\b',
                r'\b(mature|reif)\b',
                r'\b(different|anders)\b',
                r'\b(close|nah)\b',
                r'\b(friend|freund)\b',
            ],
            'STAGE_ISOLATION': [
                r'\b(secret|geheimnis)\b',
                r'\b(don\'t tell|sag nicht|sag niemandem)\b',
                r'\b(between us|zwischen uns)\b',
                r'\b(private|privat)\b',
                r'\b(snapchat|telegram|signal)\b',
                r'\b(delete|löschen)\b',
            ],
            'STAGE_NEEDS': [
                r'\b(gift|geschenk)\b',
                r'\b(money|geld)\b',
                r'\b(buy|kaufen)\b',
                r'\b(want|willst|brauchst)\b',
                r'\b(robux|v-?bucks|skins?)\b',
                r'\b(credit|guthaben)\b',
            ],
            'STAGE_ASSESSMENT': [
                r'\b(alone|allein|alleine)\b',
                r'\b(parents?|eltern)\b',
                r'\b(room|zimmer)\b',
                r'\b(door|tür)\b',
                r'\b(camera|kamera|webcam)\b',
                r'\b(where are you|wo bist du)\b',
            ],
            'STAGE_SEXUAL': [  # Zusätzlich: Explizite sexuelle Inhalte
                r'\b(pic|picture|photo|bild|foto|selfie)\b',
                r'\b(naked|nackt|nude)\b',
                r'\b(body|körper)\b',
                r'\b(sex|sexual)\b',
                r'\b(show me|zeig mir)\b',
                r'\b(send me|schick mir)\b',
            ]
        }

    def load_predator_ids(self, predators_file: str):
        """Lade Predator IDs aus Text-Datei"""
        print(f"📥 Lade Predator IDs: {predators_file}")

        with open(predators_file, 'r') as f:
            self.predator_ids = set(line.strip() for line in f if line.strip())

        print(f"✅ {len(self.predator_ids)} Predator IDs geladen")

    def parse_xml(self, xml_file: str, max_conversations: int = None):
        """Parse PAN12 XML und extrahiere Conversations"""
        print(f"\n🔍 Parse XML: {xml_file}")

        tree = ET.parse(xml_file)
        root = tree.getroot()

        conversations = root.findall('.//conversation')
        total_conversations = len(conversations)

        if max_conversations:
            conversations = conversations[:max_conversations]
            print(f"📊 Limitiere auf {max_conversations} von {total_conversations} Conversations")
        else:
            print(f"📊 {total_conversations} Conversations gefunden")

        parsed_count = 0

        for conv in conversations:
            conv_id = conv.get('id')
            messages = []
            has_predator = False

            for message in conv.findall('message'):
                msg_data = {
                    'line': message.get('line'),
                    'author': message.find('author').text,
                    'text': message.find('text').text,
                    'time': message.find('time').text if message.find('time') is not None else None
                }

                # Check ob Author ein Predator ist
                if msg_data['author'] in self.predator_ids:
                    has_predator = True
                    msg_data['is_predator'] = True
                else:
                    msg_data['is_predator'] = False

                # Heuristische Stage-Zuweisung
                msg_data['stage'] = self._detect_stage(msg_data['text'])

                messages.append(msg_data)

            self.conversations.append({
                'conversation_id': conv_id,
                'has_predator': has_predator,
                'messages': messages,
                'message_count': len(messages)
            })

            parsed_count += 1

            if parsed_count % 100 == 0:
                print(f"  Processed {parsed_count}/{len(conversations)} conversations...")

        print(f"✅ {parsed_count} Conversations geparsed")

        # Statistics
        predator_convs = sum(1 for c in self.conversations if c['has_predator'])
        print(f"📊 {predator_convs} Conversations mit Predator")
        print(f"📊 {len(self.conversations) - predator_convs} Safe Conversations")

    def _detect_stage(self, text: str) -> str:
        """
        Heuristische Stage-Detection basierend auf Keywords

        Returns: STAGE_SAFE, STAGE_TRUST, STAGE_ISOLATION, STAGE_NEEDS,
                 STAGE_ASSESSMENT, oder STAGE_SEXUAL
        """
        if not text:
            return 'STAGE_SAFE'

        text_lower = text.lower()

        # Score für jede Stage
        stage_scores = {}

        for stage, patterns in self.stage_patterns.items():
            score = 0
            for pattern in patterns:
                if re.search(pattern, text_lower, re.IGNORECASE):
                    score += 1
            stage_scores[stage] = score

        # Finde Stage mit höchstem Score
        max_score = max(stage_scores.values())

        if max_score > 0:
            # Gebe Stage mit höchstem Score zurück
            for stage, score in stage_scores.items():
                if score == max_score:
                    return stage

        return 'STAGE_SAFE'

    def extract_training_data(self, output_format='json') -> List[Dict]:
        """
        Extrahiere Training-Daten aus geparsed Conversations

        Returns: Liste von Trainings-Beispielen mit:
        - text: Nachricht
        - label: 0=SAFE, 1=GROOMING
        - stage: Detaillierte Stage
        - context: Optional - vorherige Nachrichten
        """
        print("\n📦 Extrahiere Training-Daten...")

        training_data = []

        for conv in self.conversations:
            for i, msg in enumerate(conv['messages']):
                # Überspringe leere Nachrichten
                if not msg['text'] or len(msg['text'].strip()) < 3:
                    continue

                # Kontext: Letzte 3 Nachrichten (optional)
                context = []
                for j in range(max(0, i-3), i):
                    context.append(conv['messages'][j]['text'])

                # Label: 1 wenn Predator UND nicht STAGE_SAFE
                is_grooming = (
                    msg['is_predator'] and
                    msg['stage'] != 'STAGE_SAFE'
                )

                training_data.append({
                    'text': msg['text'],
                    'label': 1 if is_grooming else 0,
                    'stage': msg['stage'],
                    'is_predator_author': msg['is_predator'],
                    'conversation_id': conv['conversation_id'],
                    'context': context if context else None,
                    'timestamp': msg.get('time')
                })

        print(f"✅ {len(training_data)} Training-Beispiele extrahiert")

        # Statistics
        grooming_count = sum(1 for d in training_data if d['label'] == 1)
        safe_count = len(training_data) - grooming_count

        print(f"📊 {grooming_count} Grooming-Beispiele")
        print(f"📊 {safe_count} Safe-Beispiele")
        print(f"📊 Balance: {grooming_count / len(training_data) * 100:.1f}% Grooming")

        # Stage-Distribution
        from collections import Counter
        stage_dist = Counter(d['stage'] for d in training_data)
        print(f"\n📊 Stage-Distribution:")
        for stage, count in stage_dist.most_common():
            print(f"   {stage}: {count} ({count/len(training_data)*100:.1f}%)")

        return training_data

    def save_to_json(self, training_data: List[Dict], output_file: str):
        """Speichere Training-Daten als JSON"""
        print(f"\n💾 Speichere JSON: {output_file}")

        output_path = Path(output_file)
        output_path.parent.mkdir(parents=True, exist_ok=True)

        with open(output_file, 'w', encoding='utf-8') as f:
            json.dump(training_data, f, indent=2, ensure_ascii=False)

        print(f"✅ JSON gespeichert: {output_file}")

    def save_to_csv(self, training_data: List[Dict], output_file: str):
        """Speichere Training-Daten als CSV"""
        print(f"\n💾 Speichere CSV: {output_file}")

        output_path = Path(output_file)
        output_path.parent.mkdir(parents=True, exist_ok=True)

        # Konvertiere zu DataFrame (ohne 'context' für CSV)
        df_data = []
        for item in training_data:
            df_data.append({
                'text': item['text'],
                'label': item['label'],
                'stage': item['stage'],
                'is_predator_author': item['is_predator_author'],
                'conversation_id': item['conversation_id']
            })

        df = pd.DataFrame(df_data)
        df.to_csv(output_file, index=False, encoding='utf-8')

        print(f"✅ CSV gespeichert: {output_file}")
        print(f"📊 {len(df)} Zeilen")

    def create_balanced_dataset(self, training_data: List[Dict],
                                  max_samples: int = 1000) -> List[Dict]:
        """
        Erstelle balanced Dataset (50/50 Grooming/Safe)
        """
        print(f"\n⚖️  Erstelle balanced Dataset (max {max_samples} Samples)...")

        grooming = [d for d in training_data if d['label'] == 1]
        safe = [d for d in training_data if d['label'] == 0]

        print(f"📊 Verfügbar: {len(grooming)} Grooming, {len(safe)} Safe")

        # Sample gleiche Anzahl
        n_samples = min(len(grooming), len(safe), max_samples // 2)

        import random
        random.seed(42)

        grooming_sample = random.sample(grooming, n_samples)
        safe_sample = random.sample(safe, n_samples)

        balanced = grooming_sample + safe_sample
        random.shuffle(balanced)

        print(f"✅ Balanced Dataset: {n_samples} Grooming + {n_samples} Safe = {len(balanced)} total")

        return balanced


def main():
    """Main Execution"""
    print("=" * 70)
    print("🦅 PAN12 XML PARSER FÜR KIDGUARD")
    print("=" * 70)

    # Paths
    base_path = Path("training/Osprey/data/toy.train")
    xml_file = base_path / "pan12-sexual-predator-identification-training-corpus-2012-05-01.xml"
    predators_file = base_path / "pan12-sexual-predator-identification-training-corpus-predators-2012-05-01.txt"

    # Output paths
    output_dir = Path("training/data/pan12_extracted")
    output_dir.mkdir(parents=True, exist_ok=True)

    # Parser initialisieren
    parser = PAN12Parser()

    # 1. Lade Predator IDs
    parser.load_predator_ids(str(predators_file))

    # 2. Parse XML (ALLE Conversations)
    parser.parse_xml(str(xml_file), max_conversations=None)  # Full Dataset!

    # 3. Extrahiere Training-Daten
    training_data = parser.extract_training_data()

    # 4. Erstelle balanced Dataset
    balanced_data = parser.create_balanced_dataset(training_data, max_samples=2000)

    # 5. Speichere in beiden Formaten
    parser.save_to_json(balanced_data, str(output_dir / "pan12_balanced.json"))
    parser.save_to_csv(balanced_data, str(output_dir / "pan12_balanced.csv"))

    # 6. Erstelle auch Full Dataset (unbalanced)
    parser.save_to_json(training_data, str(output_dir / "pan12_full.json"))
    parser.save_to_csv(training_data, str(output_dir / "pan12_full.csv"))

    print("\n" + "=" * 70)
    print("✅ PAN12 PARSING ABGESCHLOSSEN")
    print("=" * 70)
    print(f"\n📂 Output:")
    print(f"   - {output_dir / 'pan12_balanced.json'} (balanced 50/50)")
    print(f"   - {output_dir / 'pan12_balanced.csv'} (balanced 50/50)")
    print(f"   - {output_dir / 'pan12_full.json'} (unbalanced, all data)")
    print(f"   - {output_dir / 'pan12_full.csv'} (unbalanced, all data)")


if __name__ == "__main__":
    main()
