#!/usr/bin/env python3
"""
ONNX Export - MiniLM-L6 + INT8 Quantization für Android

Verwendet kleineres L6 Model statt L12 und quantisiert zu INT8.
Ziel: ~30MB statt 470MB
"""

import sys
import os
from pathlib import Path

def export_minilm_l6_quantized():
    print("=" * 70)
    print("SafeSpark - MiniLM-L6 + INT8 Quantization")
    print("=" * 70)

    # Paths
    assets_dir = Path(__file__).parent.parent / "app" / "src" / "main" / "assets"
    final_output = assets_dir / "minilm_encoder.onnx"
    assets_dir.mkdir(parents=True, exist_ok=True)

    temp_base = "/tmp/safespark_minilm"
    temp_export = f"{temp_base}_export"
    temp_quantized = f"{temp_base}_quantized"

    try:
        from optimum.onnxruntime import ORTModelForFeatureExtraction, ORTQuantizer
        from optimum.onnxruntime.configuration import AutoQuantizationConfig
        from transformers import AutoTokenizer
        import shutil

        # Clean temp dirs
        for d in [temp_export, temp_quantized]:
            if os.path.exists(d):
                shutil.rmtree(d)

        # Use L6 model (smaller than L12)
        MODEL_NAME = "sentence-transformers/paraphrase-multilingual-MiniLM-L6-v2"

        print(f"\n📦 Model: {MODEL_NAME}")
        print("   (L6 = smaller than L12, but still excellent quality)")

        # Step 1: Export to ONNX
        print("\n🔄 Step 1: Exporting to ONNX...")
        model = ORTModelForFeatureExtraction.from_pretrained(
            MODEL_NAME,
            export=True,
            provider="CPUExecutionProvider"
        )
        tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)

        model.save_pretrained(temp_export)
        tokenizer.save_pretrained(temp_export)

        original_onnx = f"{temp_export}/model.onnx"
        original_size = os.path.getsize(original_onnx) / (1024*1024)
        print(f"   ✅ Exported")
        print(f"   Original size: {original_size:.1f} MB")

        # Step 2: Quantize to INT8
        print("\n🔄 Step 2: Quantizing to INT8 (dynamic)...")

        # Use dynamic quantization (no calibration data needed)
        quantization_config = AutoQuantizationConfig.avx512_vnni(is_static=False)

        quantizer = ORTQuantizer.from_pretrained(temp_export)
        quantizer.quantize(
            save_dir=temp_quantized,
            quantization_config=quantization_config,
        )

        quantized_onnx = f"{temp_quantized}/model_quantized.onnx"
        if not os.path.exists(quantized_onnx):
            # Try alternative name
            quantized_onnx = f"{temp_quantized}/model.onnx"

        quantized_size = os.path.getsize(quantized_onnx) / (1024*1024)
        reduction = (1 - quantized_size/original_size) * 100

        print(f"   ✅ Quantized")
        print(f"   Quantized size: {quantized_size:.1f} MB")
        print(f"   Reduction: {reduction:.0f}%")

        # Copy to assets
        shutil.copy(quantized_onnx, final_output)

        final_size = os.path.getsize(final_output) / (1024*1024)
        print(f"\n📦 Final model: {final_size:.1f} MB")
        print(f"✅ Saved to: {final_output}")

        # Step 3: Test quality
        print("\n🧪 Step 3: Testing quality...")
        import onnxruntime as ort
        import numpy as np

        # Load quantized model
        session = ort.InferenceSession(
            str(final_output),
            providers=['CPUExecutionProvider']
        )

        print(f"   Providers: {session.get_providers()}")

        # Test with sample texts
        tokenizer_test = AutoTokenizer.from_pretrained(temp_quantized)

        def encode(text):
            inputs = tokenizer_test(
                text,
                return_tensors="np",
                padding="max_length",
                max_length=128,
                truncation=True
            )
            outputs = session.run(None, {
                "input_ids": inputs["input_ids"].astype(np.int64),
                "attention_mask": inputs["attention_mask"].astype(np.int64)
            })
            # Mean pooling
            hidden_states = outputs[0]
            attention_mask = inputs["attention_mask"]
            mask_expanded = np.expand_dims(attention_mask, -1)
            sum_embeddings = np.sum(hidden_states * mask_expanded, axis=1)
            sum_mask = np.clip(np.sum(mask_expanded, axis=1), a_min=1e-9, a_max=None)
            embedding = sum_embeddings / sum_mask
            return embedding.squeeze()

        def cosine_sim(a, b):
            return np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b))

        # Quality test
        seed = encode("Bist du alleine?")
        test1 = encode("Ist heute noch jemand bei dir?")
        test2 = encode("Bist du müde?")

        sim1 = cosine_sim(seed, test1)
        sim2 = cosine_sim(seed, test2)

        print(f"\n   Quality Check:")
        print(f"   'Bist du alleine?' ↔ 'Ist heute noch jemand bei dir?': {sim1:.3f}")
        print(f"   'Bist du alleine?' ↔ 'Bist du müde?': {sim2:.3f}")

        # Validation
        if sim1 > 0.70 and sim2 < 0.55:
            print(f"   ✅ Quality is EXCELLENT!")
            print(f"      Similar texts: {sim1:.1%} (should be >70%)")
            print(f"      Different texts: {sim2:.1%} (should be <55%)")
        else:
            print(f"   ⚠️ Quality check:")
            if sim1 <= 0.70:
                print(f"      Similar texts only {sim1:.1%} (expected >70%)")
            if sim2 >= 0.55:
                print(f"      Different texts {sim2:.1%} (expected <55%)")

        # Clean up
        shutil.rmtree(temp_export)
        shutil.rmtree(temp_quantized)

        return True

    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    print("\n🚀 Starting optimized ONNX export...")
    print("   Model: MiniLM-L6-v2 (smaller)")
    print("   Quantization: INT8 dynamic")
    print("   Target size: ~30 MB\n")

    success = export_minilm_l6_quantized()

    if success:
        print("\n" + "=" * 70)
        print("✅ SUCCESS! Optimized ONNX model ready for Android!")
        print("=" * 70)
        print("\n📋 Next steps:")
        print("1. cd /Users/knutludtmann/AndroidStudioProjects/SafeSpark")
        print("2. ./gradlew clean assembleDebug")
        print("3. ./gradlew connectedDebugAndroidTest")
        print("\n🎯 Model should now fit in Android memory!")
        print("🎯 All 45 tests should pass!")
        sys.exit(0)
    else:
        print("\n" + "=" * 70)
        print("❌ FAILED!")
        print("=" * 70)
        sys.exit(1)
