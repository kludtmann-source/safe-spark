#!/usr/bin/env python3
"""
SafeSpark Refactoring Script
Ersetzt alle "KidGuard" Vorkommen mit "SafeSpark"

Usage:
    python refactor_to_safespark.py --dry-run  # Test-Modus
    python refactor_to_safespark.py            # Echte Ausführung
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Tuple, Dict

# Konfiguration
PROJECT_ROOT = Path(__file__).parent
DRY_RUN = '--dry-run' in sys.argv

# Refactoring-Regeln (Reihenfolge wichtig!)
REPLACEMENTS = [
    # Package-Namen (wichtig: vor anderen Ersetzungen!)
    (r'com\.example\.safespark', 'com.example.safespark'),
    (r'com\.safespark', 'com.safespark'),

    # Klassen-Namen (CamelCase)
    (r'\bKidGuardEngine\b', 'SafeSparkEngine'),
    (r'\bKidGuardService\b', 'SafeSparkService'),
    (r'\bKidGuardDetector\b', 'SafeSparkDetector'),
    (r'\bKidGuardConstants\b', 'SafeSparkConstants'),
    (r'\bKidGuardApplication\b', 'SafeSparkApplication'),

    # Variablen (camelCase)
    (r'\bkidGuardEngine\b', 'safeSparkEngine'),
    (r'\bkidGuardService\b', 'safeSparkService'),
    (r'\bkidGuardResult\b', 'safeSparkResult'),
    (r'\bkidGuardInstance\b', 'safeSparkInstance'),

    # Funktionen
    (r'\binitKidGuard\b', 'initSafeSpark'),
    (r'\bstartKidGuard\b', 'startSafeSpark'),
    (r'\bstopKidGuard\b', 'stopSafeSpark'),

    # Generische Ersetzungen (nach spezifischen!)
    (r'\bKidGuard\b', 'SafeSpark'),
    (r'\bkidGuard\b', 'safeSpark'),

    # Snake_case
    (r'\bkid_guard\b', 'safe_spark'),
    (r'\bKID_GUARD\b', 'SAFE_SPARK'),

    # Kebab-case
    (r'\bkid-guard\b', 'safe-spark'),

    # Lowercase (nur ganze Wörter)
    (r'\bsafespark\b', 'safespark'),

    # Uppercase
    (r'\bKIDGUARD\b', 'SAFESPARK'),
]

# Dateitypen die bearbeitet werden
INCLUDE_EXTENSIONS = {
    '.kt',      # Kotlin
    '.java',    # Java
    '.xml',     # XML (Manifest, Resources)
    '.md',      # Markdown
    '.py',      # Python
    '.gradle',  # Gradle
    '.kts',     # Kotlin Script (Gradle)
    '.properties',
    '.json',
    '.txt',
    '.sh',
}

# Verzeichnisse die übersprungen werden
EXCLUDE_DIRS = {
    '.git',
    '.gradle',
    '.idea',
    'build',
    'venv',
    'ml/venv',
    'training/venv',
    'node_modules',
    '__pycache__',
}

# Dateien die übersprungen werden (Model-Namen bleiben!)
EXCLUDE_FILES = {
    'grooming_detector_scientific.tflite',
    'grooming_detector_pasyda.tflite',
    'grooming_detector.tflite',
    'kid_guard_v1.tflite',  # Wird später umbenannt
}

# Ausnahmen: Diese Strings NICHT ersetzen
EXCEPTIONS = [
    'grooming_detector',  # Model-Namen
    'pan12',              # Dataset
    'pasyda',             # Dataset
]

class RefactoringStats:
    def __init__(self):
        self.files_processed = 0
        self.files_modified = 0
        self.total_replacements = 0
        self.changes_by_file: Dict[str, int] = {}

    def add_change(self, filepath: str, count: int):
        self.files_processed += 1
        if count > 0:
            self.files_modified += 1
            self.total_replacements += count
            self.changes_by_file[filepath] = count

    def print_summary(self):
        print("\n" + "=" * 70)
        print("📊 REFACTORING SUMMARY")
        print("=" * 70)
        print(f"Files processed:  {self.files_processed}")
        print(f"Files modified:   {self.files_modified}")
        print(f"Total replacements: {self.total_replacements}")
        print()
        if self.changes_by_file:
            print("Top 10 Modified Files:")
            for filepath, count in sorted(
                self.changes_by_file.items(),
                key=lambda x: x[1],
                reverse=True
            )[:10]:
                print(f"  {count:3d}x  {filepath}")
        print("=" * 70)

def should_process_file(filepath: Path) -> bool:
    """Prüft ob Datei bearbeitet werden soll"""

    # Prüfe Extension
    if filepath.suffix not in INCLUDE_EXTENSIONS:
        return False

    # Prüfe ausgeschlossene Dateien
    if filepath.name in EXCLUDE_FILES:
        return False

    # Prüfe ausgeschlossene Verzeichnisse
    for exclude_dir in EXCLUDE_DIRS:
        if exclude_dir in filepath.parts:
            return False

    return True

def refactor_content(content: str, filepath: str) -> Tuple[str, int]:
    """Ersetzt alle KidGuard Vorkommen"""

    replacement_count = 0
    modified_content = content

    for pattern, replacement in REPLACEMENTS:
        # Zähle Matches
        matches = re.findall(pattern, modified_content)

        # Prüfe Ausnahmen
        valid_matches = []
        for match in matches:
            # Prüfe ob Match in Ausnahme-Kontext steht
            is_exception = False
            for exception in EXCEPTIONS:
                if exception in match.lower():
                    is_exception = True
                    break

            if not is_exception:
                valid_matches.append(match)

        if valid_matches:
            # Ersetze
            before_count = len(re.findall(pattern, modified_content))
            modified_content = re.sub(pattern, replacement, modified_content)
            after_count = len(re.findall(pattern, modified_content))

            actual_replacements = before_count - after_count
            if actual_replacements > 0:
                replacement_count += actual_replacements
                print(f"  ✓ {actual_replacements}x '{pattern}' → '{replacement}'")

    return modified_content, replacement_count

def refactor_file(filepath: Path, stats: RefactoringStats) -> bool:
    """Bearbeitet eine einzelne Datei"""

    try:
        # Lese Datei
        with open(filepath, 'r', encoding='utf-8') as f:
            original_content = f.read()

        # Refactoring
        modified_content, replacement_count = refactor_content(
            original_content,
            str(filepath)
        )

        # Statistik
        stats.add_change(str(filepath.relative_to(PROJECT_ROOT)), replacement_count)

        # Schreibe zurück (wenn nicht Dry-Run)
        if replacement_count > 0 and not DRY_RUN:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(modified_content)
            print(f"✅ Modified: {filepath.relative_to(PROJECT_ROOT)}")
        elif replacement_count > 0:
            print(f"🔍 Would modify: {filepath.relative_to(PROJECT_ROOT)}")

        return True

    except Exception as e:
        print(f"❌ Error processing {filepath}: {e}")
        return False

def rename_files_and_dirs():
    """Benennt Dateien und Verzeichnisse um"""

    print("\n" + "=" * 70)
    print("📁 RENAMING FILES & DIRECTORIES")
    print("=" * 70)

    renames = []

    # Finde alle Dateien/Ordner mit "safespark" im Namen
    for root, dirs, files in os.walk(PROJECT_ROOT):
        # Skip ausgeschlossene Verzeichnisse
        dirs[:] = [d for d in dirs if d not in EXCLUDE_DIRS]

        # Dateien
        for filename in files:
            if 'safespark' in filename.lower():
                old_path = Path(root) / filename
                new_filename = filename.replace('safespark', 'safespark')
                new_filename = new_filename.replace('KidGuard', 'SafeSpark')
                new_filename = new_filename.replace('kid_guard', 'safe_spark')
                new_filename = new_filename.replace('kid-guard', 'safe-spark')
                new_path = Path(root) / new_filename

                if old_path != new_path:
                    renames.append((old_path, new_path))

        # Verzeichnisse
        for dirname in dirs:
            if 'safespark' in dirname.lower():
                old_path = Path(root) / dirname
                new_dirname = dirname.replace('safespark', 'safespark')
                new_dirname = new_dirname.replace('KidGuard', 'SafeSpark')
                new_dirname = new_dirname.replace('kid_guard', 'safe_spark')
                new_path = Path(root) / new_dirname

                if old_path != new_path:
                    renames.append((old_path, new_path))

    # Führe Umbenennungen durch
    for old_path, new_path in renames:
        if DRY_RUN:
            print(f"🔍 Would rename: {old_path.relative_to(PROJECT_ROOT)} → {new_path.name}")
        else:
            try:
                old_path.rename(new_path)
                print(f"✅ Renamed: {old_path.relative_to(PROJECT_ROOT)} → {new_path.name}")
            except Exception as e:
                print(f"❌ Error renaming {old_path}: {e}")

def main():
    print("🚀 SafeSpark Refactoring Script")
    print("=" * 70)

    if DRY_RUN:
        print("🔍 DRY-RUN MODE (keine Änderungen werden gespeichert)")
    else:
        print("⚠️  LIVE MODE (Dateien werden geändert!)")

    print("=" * 70)
    print()

    # Statistik
    stats = RefactoringStats()

    # Durchsuche alle Dateien
    print("📝 Processing files...")
    print()

    for filepath in PROJECT_ROOT.rglob('*'):
        if filepath.is_file() and should_process_file(filepath):
            refactor_file(filepath, stats)

    # Benenne Dateien/Ordner um
    rename_files_and_dirs()

    # Zusammenfassung
    stats.print_summary()

    if DRY_RUN:
        print("\n💡 Führe 'python refactor_to_safespark.py' aus (ohne --dry-run) für echte Änderungen")
    else:
        print("\n✅ Refactoring abgeschlossen!")
        print("\nNächste Schritte:")
        print("  1. python verify_refactoring.py")
        print("  2. ./gradlew clean build")
        print("  3. git add -A")
        print("  4. git commit -m 'refactor: Rename KidGuard to SafeSpark'")

if __name__ == '__main__':
    main()
