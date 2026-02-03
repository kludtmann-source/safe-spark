#!/usr/bin/env python3
"""
ONNX Export with Quantization for Android

Optimiert und quantisiert das Model für mobile Geräte.
"""

import sys
from pathlib import Path

def export_optimized_onnx():
    print("=" * 70)
    print("SafeSpark - Optimized ONNX Export for Android")
    print("=" * 70)

    output_dir = Path(__file__).parent.parent / "app" / "src" / "main" / "assets"
    output_file = output_dir / "minilm_encoder.onnx"
    output_dir.mkdir(parents=True, exist_ok=True)

    temp_dir = "/tmp/safespark_onnx_optimized"

    try:
        from optimum.onnxruntime import ORTModelForFeatureExtraction
        from optimum.onnxruntime.configuration import OptimizationConfig, AutoQuantizationConfig
        import shutil
        import os

        # Clean temp
        if os.path.exists(temp_dir):
            shutil.rmtree(temp_dir)
        os.makedirs(temp_dir)

        print("\n🔄 Step 1: Export to ONNX with Optimum...")
        model = ORTModelForFeatureExtraction.from_pretrained(
            "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
            export=True,
            provider="CPUExecutionProvider"
        )

        print("   ✅ Model exported")

        # Save to temp
        model.save_pretrained(temp_dir)

        # Find ONNX file
        onnx_files = list(Path(temp_dir).glob("*.onnx"))
        if not onnx_files:
            print("   ❌ No ONNX file found!")
            return False

        temp_onnx = onnx_files[0]
        size_mb = temp_onnx.stat().st_size / (1024 * 1024)
        print(f"   Original size: {size_mb:.2f} MB")

        # Quantize
        print("\n🔄 Step 2: Quantizing for mobile...")
        from onnxruntime.quantization import quantize_dynamic, QuantType

        quantized_path = temp_dir + "/model_quantized.onnx"

        quantize_dynamic(
            str(temp_onnx),
            quantized_path,
            weight_type=QuantType.QUInt8,
            optimize_model=True
        )

        quant_size_mb = Path(quantized_path).stat().st_size / (1024 * 1024)
        print(f"   Quantized size: {quant_size_mb:.2f} MB")
        print(f"   Reduction: {((size_mb - quant_size_mb) / size_mb * 100):.1f}%")

        # Copy to assets
        shutil.copy(quantized_path, output_file)

        final_size_mb = output_file.stat().st_size / (1024 * 1024)
        print(f"\n📦 Final size: {final_size_mb:.2f} MB")
        print(f"✅ Saved to: {output_file}")

        # Test
        print("\n🧪 Testing quantized model...")
        import onnxruntime as ort
        import numpy as np

        session = ort.InferenceSession(
            str(output_file),
            providers=['CPUExecutionProvider']
        )

        from transformers import AutoTokenizer
        tokenizer = AutoTokenizer.from_pretrained(
            "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
        )

        test_text = "Test"
        inputs = tokenizer(test_text, return_tensors="np", padding="max_length", max_length=128, truncation=True)

        outputs = session.run(None, {
            "input_ids": inputs["input_ids"].astype(np.int64),
            "attention_mask": inputs["attention_mask"].astype(np.int64)
        })

        print(f"   Output shape: {outputs[0].shape}")
        print("   ✅ Quantized model works!")

        # Clean up
        shutil.rmtree(temp_dir)

        return True

    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    success = export_optimized_onnx()

    if success:
        print("\n" + "=" * 70)
        print("✅ SUCCESS! Optimized ONNX ready for Android!")
        print("=" * 70)
        sys.exit(0)
    else:
        print("\n❌ FAILED!")
        sys.exit(1)
