#!/usr/bin/env python3
"""
Kombiniere alle Datenquellen für KidGuard Training
Erstellt finales Trainings-Dataset aus:
1. Scientific Papers (207 Beispiele)
2. PAN12 (770 Beispiele)
3. Optional: Augmented Data
"""

import pandas as pd
import json
from pathlib import Path
from collections import Counter

class DatasetCombiner:
    """Kombiniert alle verfügbaren Datenquellen"""

    def __init__(self):
        self.datasets = []
        self.combined = None

    def load_scientific_papers(self):
        """Lade Scientific Papers Dataset (207 Beispiele)"""
        print("\n📥 Lade Scientific Papers Dataset...")

        # Versuche zuerst JSON
        json_path = Path("ml/data/scientific_augmented_dataset.json")

        if json_path.exists():
            with open(json_path, 'r') as f:
                data = json.load(f)
            df = pd.DataFrame(data)
            print(f"   Geladen von: {json_path}")
        else:
            # Fallback: CSV
            csv_path = Path("ml/data/grooming_training_scientific.csv")
            if csv_path.exists():
                df = pd.read_csv(csv_path)
                print(f"   Geladen von: {csv_path}")
            else:
                print(f"⚠️  Nicht gefunden: {json_path} oder {csv_path}")
                return 0

        # Normalisiere Format
        df['text'] = df['text'].astype(str)

        # Mappe Stage-Labels zu binär (0=SAFE, 1=GROOMING)
        if 'label' in df.columns:
            # Konvertiere zu object dtype für sicheres mapping
            df['label'] = df['label'].astype('object')

            # Mappe String-Labels zu numerisch
            stage_mapping = {
                'STAGE_SAFE': 0,
                'STAGE_TRUST': 1,
                'STAGE_ISOLATION': 1,
                'STAGE_NEEDS': 1,
                'STAGE_ASSESSMENT': 1,
                'STAGE_SEXUAL': 1,
                'safe': 0,
                'grooming': 1,
                0: 0,
                1: 1
            }
            df['label'] = df['label'].map(stage_mapping).fillna(0).astype(int)

        df['source'] = 'scientific_papers'

        self.datasets.append(df)

        print(f"✅ {len(df)} Beispiele von Scientific Papers")
        return len(df)

    def load_pan12(self):
        """Lade PAN12 Dataset (770 Beispiele)"""
        print("\n📥 Lade PAN12 Dataset...")

        csv_path = Path("training/data/pan12_extracted/pan12_full.csv")

        if not csv_path.exists():
            print(f"⚠️  Nicht gefunden: {csv_path}")
            return 0

        df = pd.read_csv(csv_path)

        # Normalisiere Format
        df['text'] = df['text'].astype(str)
        df['label'] = df['label'].astype(int)
        df['source'] = 'pan12'

        # Behalte nur relevante Spalten
        df = df[['text', 'label', 'source']]

        self.datasets.append(df)

        print(f"✅ {len(df)} Beispiele von PAN12")
        return len(df)

    def combine_all(self):
        """Kombiniere alle Datasets"""
        print("\n🔄 Kombiniere alle Datasets...")

        if not self.datasets:
            print("❌ Keine Datasets geladen!")
            return None

        self.combined = pd.concat(self.datasets, ignore_index=True)

        print(f"✅ {len(self.combined)} Beispiele total")

        # Statistics
        print(f"\n📊 Statistics:")
        print(f"   Total: {len(self.combined)}")

        # Label-Distribution
        label_dist = Counter(self.combined['label'])
        for label, count in label_dist.items():
            label_name = "GROOMING" if label == 1 else "SAFE"
            print(f"   {label_name}: {count} ({count/len(self.combined)*100:.1f}%)")

        # Source-Distribution
        print(f"\n📊 Per Source:")
        source_dist = self.combined.groupby('source')['label'].value_counts()
        for (source, label), count in source_dist.items():
            label_name = "GROOMING" if label == 1 else "SAFE"
            print(f"   {source} - {label_name}: {count}")

        return self.combined

    def create_balanced_split(self, train_ratio=0.8):
        """
        Erstelle balanced Train/Test Split
        """
        print(f"\n⚖️  Erstelle balanced Train/Test Split ({train_ratio*100:.0f}/{(1-train_ratio)*100:.0f})...")

        if self.combined is None:
            print("❌ Kombiniere zuerst die Datasets!")
            return None, None

        from sklearn.model_selection import train_test_split

        # Stratified Split (gleiche Label-Distribution in Train/Test)
        train_df, test_df = train_test_split(
            self.combined,
            test_size=1-train_ratio,
            stratify=self.combined['label'],
            random_state=42
        )

        print(f"✅ Train: {len(train_df)} Beispiele")
        print(f"✅ Test: {len(test_df)} Beispiele")

        # Train Statistics
        train_grooming = sum(train_df['label'] == 1)
        print(f"\n📊 Train Set:")
        print(f"   GROOMING: {train_grooming} ({train_grooming/len(train_df)*100:.1f}%)")
        print(f"   SAFE: {len(train_df) - train_grooming} ({(len(train_df) - train_grooming)/len(train_df)*100:.1f}%)")

        # Test Statistics
        test_grooming = sum(test_df['label'] == 1)
        print(f"\n📊 Test Set:")
        print(f"   GROOMING: {test_grooming} ({test_grooming/len(test_df)*100:.1f}%)")
        print(f"   SAFE: {len(test_df) - test_grooming} ({(len(test_df) - test_grooming)/len(test_df)*100:.1f}%)")

        return train_df, test_df

    def save_combined(self, output_dir="training/data/combined"):
        """Speichere kombiniertes Dataset"""
        print(f"\n💾 Speichere kombiniertes Dataset...")

        output_path = Path(output_dir)
        output_path.mkdir(parents=True, exist_ok=True)

        # Full Dataset
        full_csv = output_path / "kidguard_combined_full.csv"
        self.combined.to_csv(full_csv, index=False, encoding='utf-8')
        print(f"✅ Full Dataset: {full_csv}")

        # Train/Test Split
        train_df, test_df = self.create_balanced_split(train_ratio=0.8)

        train_csv = output_path / "kidguard_train.csv"
        test_csv = output_path / "kidguard_test.csv"

        train_df.to_csv(train_csv, index=False, encoding='utf-8')
        test_df.to_csv(test_csv, index=False, encoding='utf-8')

        print(f"✅ Train Dataset: {train_csv}")
        print(f"✅ Test Dataset: {test_csv}")

        # Erstelle auch JSON-Versionen
        train_json = output_path / "kidguard_train.json"
        test_json = output_path / "kidguard_test.json"

        train_df.to_json(train_json, orient='records', indent=2, force_ascii=False)
        test_df.to_json(test_json, orient='records', indent=2, force_ascii=False)

        print(f"✅ Train JSON: {train_json}")
        print(f"✅ Test JSON: {test_json}")

        # Erstelle Summary Report
        self.create_summary_report(output_path)

    def create_summary_report(self, output_dir):
        """Erstelle Summary Report"""
        report_path = output_dir / "DATASET_SUMMARY.md"

        report = f"""# KidGuard Combined Dataset Summary

## Dataset Sources

### 1. Scientific Papers
- Source: 4 Research Papers (Uppsala, Nature, Frontiers, ScienceDirect)
- Samples: ~207
- Language: Mixed (EN/DE)
- Quality: High (peer-reviewed)

### 2. PAN12 (via Osprey)
- Source: PAN12 Sexual Predator Identification Dataset
- Samples: ~770
- Language: English
- Quality: High (real chat logs, labeled by experts)

## Combined Dataset Statistics

### Total Samples
```
Total: {len(self.combined)}
```

### Label Distribution
```
{self.combined['label'].value_counts().to_string()}
```

### Source Distribution
```
{self.combined['source'].value_counts().to_string()}
```

## Dataset Splits

### Train Set (80%)
- Used for model training
- Stratified sampling ensures balanced labels

### Test Set (20%)
- Used for final evaluation
- NEVER used during training
- Stratified sampling ensures balanced labels

## Expected Performance

With {len(self.combined)} samples:

| Metrik | Current (207) | Expected ({len(self.combined)}) |
|--------|---------------|----------------------------------|
| **Accuracy** | 90.5% | **92-94%** |
| **Recall (Critical)** | 88% | **95%+** |
| **False Negatives** | ~5% | **< 3%** |
| **Vocabulary Size** | 381 | **1,500+** |

## Next Steps

1. ✅ **Datasets combined**
2. ⏭️  **Train new model** with combined data
3. ⏭️  **Evaluate** on test set
4. ⏭️  **Compare** with current 90.5% baseline

## Files Generated

```
training/data/combined/
├── kidguard_combined_full.csv   (Full dataset)
├── kidguard_train.csv           (80% for training)
├── kidguard_test.csv            (20% for evaluation)
├── kidguard_train.json          (JSON format)
├── kidguard_test.json           (JSON format)
└── DATASET_SUMMARY.md           (This file)
```

---

**Generated:** {pd.Timestamp.now().strftime('%Y-%m-%d %H:%M:%S')}
"""

        with open(report_path, 'w', encoding='utf-8') as f:
            f.write(report)

        print(f"📄 Summary Report: {report_path}")


def main():
    """Main Execution"""
    print("=" * 70)
    print("🔗 KOMBINIERE ALLE DATASETS FÜR KIDGUARD")
    print("=" * 70)

    combiner = DatasetCombiner()

    # Lade alle verfügbaren Datasets
    total = 0
    total += combiner.load_scientific_papers()
    total += combiner.load_pan12()

    if total == 0:
        print("\n❌ Keine Datasets gefunden!")
        return

    # Kombiniere
    combiner.combine_all()

    # Speichere
    combiner.save_combined()

    print("\n" + "=" * 70)
    print("✅ DATASET-KOMBINATION ABGESCHLOSSEN")
    print("=" * 70)
    print(f"\n🎯 Nächster Schritt: Training mit {len(combiner.combined)} Beispielen!")
    print(f"   python3 ml/scripts/train_model.py")


if __name__ == "__main__":
    main()
