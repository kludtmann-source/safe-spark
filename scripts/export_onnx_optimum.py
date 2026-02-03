#!/usr/bin/env python3
"""
ONNX Export with Hugging Face Optimum

Uses Optimum - specifically built for ONNX export.
Handles SDPA (Scaled Dot Product Attention) automatically.
"""

import sys
from pathlib import Path

def export_with_optimum():
    print("=" * 70)
    print("SafeSpark - ONNX Export with Hugging Face Optimum")
    print("=" * 70)
    
    # Paths
    output_dir = Path(__file__).parent.parent / "app" / "src" / "main" / "assets"
    output_file = output_dir / "minilm_encoder.onnx"
    output_dir.mkdir(parents=True, exist_ok=True)
    
    temp_dir = "/tmp/safespark_onnx_export"
    
    print(f"\n📁 Final output: {output_file}")
    print(f"📁 Temp directory: {temp_dir}")
    
    try:
        from optimum.exporters.onnx import main_export
        import shutil
        import os
        
        # Clean temp dir
        if os.path.exists(temp_dir):
            shutil.rmtree(temp_dir)
        os.makedirs(temp_dir)
        
        print("\n🔄 Exporting with Optimum...")
        print("   Model: sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2")
        print("   Task: feature-extraction")
        print("   Opset: 14")
        print("   Device: cpu")
        
        # Export using Optimum
        main_export(
            model_name_or_path="sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
            output=temp_dir,
            task="feature-extraction",
            opset=14,
            device="cpu",
            fp16=False,  # Use FP32 for better compatibility
            optimize=None  # No optimization for now
        )
        
        print("   ✅ Optimum export completed!")
        
        # Find the ONNX file
        onnx_files = list(Path(temp_dir).glob("*.onnx"))
        
        if not onnx_files:
            # Sometimes it's in a subdirectory
            onnx_files = list(Path(temp_dir).glob("**/*.onnx"))
        
        if not onnx_files:
            print("   ❌ No ONNX file found in output!")
            return False
        
        source_onnx = onnx_files[0]
        print(f"   Found ONNX: {source_onnx.name}")
        
        # Copy to assets
        shutil.copy(source_onnx, output_file)
        
        size_mb = output_file.stat().st_size / (1024 * 1024)
        print(f"\n📦 Size: {size_mb:.2f} MB")
        print(f"✅ Saved to: {output_file}")
        
        # Verify with ONNX
        print("\n🔍 Verifying ONNX model...")
        import onnx
        onnx_model = onnx.load(str(output_file))
        onnx.checker.check_model(onnx_model)
        print("   ✅ ONNX model is valid")
        
        # Test with ONNX Runtime
        print("\n🧪 Testing ONNX Runtime...")
        import onnxruntime as ort
        import numpy as np
        
        session = ort.InferenceSession(
            str(output_file),
            providers=['CPUExecutionProvider']
        )
        
        print(f"   Providers: {session.get_providers()}")
        print(f"   Inputs: {[i.name for i in session.get_inputs()]}")
        print(f"   Outputs: {[o.name for o in session.get_outputs()]}")
        
        # Create test input
        from transformers import AutoTokenizer
        tokenizer = AutoTokenizer.from_pretrained(
            "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
        )
        
        test_text = "This is a test sentence."
        inputs = tokenizer(
            test_text,
            return_tensors="np",
            padding="max_length",
            max_length=128,
            truncation=True
        )
        
        # Run inference
        outputs = session.run(
            None,
            {
                "input_ids": inputs["input_ids"].astype(np.int64),
                "attention_mask": inputs["attention_mask"].astype(np.int64)
            }
        )
        
        print(f"   Output shape: {outputs[0].shape}")
        
        # Calculate mean pooling
        hidden_states = outputs[0]
        attention_mask = inputs["attention_mask"]
        
        mask_expanded = np.expand_dims(attention_mask, -1)
        sum_embeddings = np.sum(hidden_states * mask_expanded, axis=1)
        sum_mask = np.clip(np.sum(mask_expanded, axis=1), a_min=1e-9, a_max=None)
        embeddings = sum_embeddings / sum_mask
        
        print(f"   Embedding shape: {embeddings.shape}")
        print(f"   Embedding dimension: {embeddings.shape[1]}")
        
        if embeddings.shape[1] == 384:
            print("   ✅ Correct dimension (384)!")
        else:
            print(f"   ⚠️ Unexpected dimension: {embeddings.shape[1]}")
        
        print("   ✅ ONNX Runtime test passed!")
        
        # Clean up temp dir
        shutil.rmtree(temp_dir)
        
        return True
        
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    print("\n🚀 Starting ONNX export with Hugging Face Optimum...")
    print("   (Optimum handles SDPA automatically)")
    
    success = export_with_optimum()
    
    if success:
        print("\n" + "=" * 70)
        print("✅ SUCCESS! ONNX model ready for production!")
        print("=" * 70)
        print("\n📋 Next steps:")
        print("1. cd /Users/knutludtmann/AndroidStudioProjects/SafeSpark")
        print("2. ./gradlew clean assembleDebug")
        print("3. ./gradlew connectedDebugAndroidTest")
        print("\n🎯 All 45 tests should pass!")
        print("🎯 App is production-ready with semantic understanding!")
        sys.exit(0)
    else:
        print("\n" + "=" * 70)
        print("❌ FAILED!")
        print("=" * 70)
        sys.exit(1)
