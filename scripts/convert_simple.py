#!/usr/bin/env python3
"""
Simplified ONNX Export using Optimum

Verwendet optimum library für vereinfachte ONNX-Konvertierung
"""

from pathlib import Path
from optimum.onnxruntime import ORTModelForFeatureExtraction
from transformers import AutoTokenizer
import onnx

def main():
    print("=" * 60)
    print("Simplified ONNX Export with Optimum")
    print("=" * 60)

    model_name = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
    output_dir = Path(__file__).parent.parent / "app" / "src" / "main" / "assets"
    output_dir.mkdir(parents=True, exist_ok=True)

    print(f"\n🔄 Converting {model_name}...")
    print(f"📁 Output: {output_dir}")

    try:
        # Export to ONNX using optimum
        print("\n1️⃣ Exporting to ONNX...")
        model = ORTModelForFeatureExtraction.from_pretrained(
            model_name,
            export=True,
            use_auth_token=False
        )

        print("✅ Model exported!")

        # Save to output directory
        print("\n2️⃣ Saving model...")
        temp_path = Path("./onnx_temp")
        model.save_pretrained(temp_path)

        # Copy ONNX file
        onnx_file = temp_path / "model.onnx"
        output_file = output_dir / "minilm_encoder.onnx"

        import shutil
        shutil.copy(onnx_file, output_file)

        # Clean up temp
        shutil.rmtree(temp_path)

        print(f"✅ Saved to: {output_file}")

        # Check file size
        size_mb = output_file.stat().st_size / (1024 * 1024)
        print(f"📦 File size: {size_mb:.2f} MB")

        # Verify ONNX
        print("\n3️⃣ Verifying ONNX...")
        onnx_model = onnx.load(str(output_file))
        onnx.checker.check_model(onnx_model)
        print("✅ ONNX model is valid!")

        print("\n" + "=" * 60)
        print("✅ DONE! ONNX model ready for Android.")
        print("=" * 60)

    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
