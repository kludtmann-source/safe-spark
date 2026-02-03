#!/usr/bin/env python3
"""
ONNX Model Export - Production Ready

Exports paraphrase-multilingual-MiniLM-L12-v2 to ONNX format
for Android deployment.
"""

import os
import sys
from pathlib import Path

def export_onnx():
    print("=" * 70)
    print("SafeSpark - ONNX Model Export for Production")
    print("=" * 70)
    
    # Output path
    output_dir = Path(__file__).parent.parent / "app" / "src" / "main" / "assets"
    output_file = output_dir / "minilm_encoder.onnx"
    output_dir.mkdir(parents=True, exist_ok=True)
    
    print(f"\n📁 Output: {output_file}")
    
    try:
        # Try optimum first (best quality)
        print("\n🔄 Attempting export with optimum...")
        try:
            from optimum.exporters.onnx import main_export
            from pathlib import Path as OptPath
            
            temp_dir = "/tmp/safespark_onnx"
            os.makedirs(temp_dir, exist_ok=True)
            
            main_export(
                model_name_or_path="sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
                output=temp_dir,
                task="feature-extraction",
                opset=14,
                device="cpu"
            )
            
            # Copy to assets
            import shutil
            onnx_src = os.path.join(temp_dir, "model.onnx")
            if os.path.exists(onnx_src):
                shutil.copy(onnx_src, output_file)
                print(f"✅ Exported with optimum!")
                
                # Clean up
                shutil.rmtree(temp_dir)
                
                size_mb = output_file.stat().st_size / (1024 * 1024)
                print(f"📦 Size: {size_mb:.2f} MB")
                return True
                
        except ImportError:
            print("⚠️ optimum not available, trying manual export...")
        except Exception as e:
            print(f"⚠️ optimum failed: {e}")
            print("⚠️ Trying manual export...")
        
        # Manual export with sentence-transformers
        print("\n🔄 Manual export with sentence-transformers...")
        
        from sentence_transformers import SentenceTransformer
        import torch
        import onnx
        
        # Load model
        print("   Loading model from cache...")
        model = SentenceTransformer(
            "paraphrase-multilingual-MiniLM-L12-v2",
            device='cpu'
        )
        
        # Get the transformer
        transformer = model[0].auto_model
        transformer = transformer.to('cpu')
        transformer.eval()
        
        print("   ✅ Model loaded")
        
        # Create dummy input
        tokenizer = model.tokenizer
        dummy_text = "This is a test sentence for ONNX export."
        inputs = tokenizer(
            dummy_text,
            return_tensors="pt",
            padding="max_length",
            max_length=128,
            truncation=True
        )
        
        input_ids = inputs["input_ids"]
        attention_mask = inputs["attention_mask"]
        
        print(f"   Input shape: {input_ids.shape}")
        
        # Export to ONNX
        print("\n   🔄 Exporting to ONNX (this may take 1-2 minutes)...")
        
        temp_onnx = "/tmp/minilm_temp.onnx"
        
        with torch.no_grad():
            torch.onnx.export(
                transformer,
                (input_ids, attention_mask),
                temp_onnx,
                input_names=["input_ids", "attention_mask"],
                output_names=["last_hidden_state"],
                dynamic_axes={
                    "input_ids": {0: "batch_size", 1: "sequence_length"},
                    "attention_mask": {0: "batch_size", 1: "sequence_length"},
                    "last_hidden_state": {0: "batch_size", 1: "sequence_length"}
                },
                opset_version=14,
                do_constant_folding=True,
                verbose=False
            )
        
        print("   ✅ ONNX export successful")
        
        # Verify ONNX
        print("\n   🔍 Verifying ONNX model...")
        onnx_model = onnx.load(temp_onnx)
        onnx.checker.check_model(onnx_model)
        print("   ✅ ONNX model is valid")
        
        # Move to final location
        import shutil
        shutil.move(temp_onnx, str(output_file))
        
        size_mb = output_file.stat().st_size / (1024 * 1024)
        print(f"\n📦 Final size: {size_mb:.2f} MB")
        
        # Test with ONNX Runtime
        print("\n   🧪 Testing ONNX Runtime...")
        import onnxruntime as ort
        
        session = ort.InferenceSession(
            str(output_file),
            providers=['CPUExecutionProvider']
        )
        
        print(f"   Providers: {session.get_providers()}")
        
        # Run inference
        outputs = session.run(
            None,
            {
                "input_ids": input_ids.numpy(),
                "attention_mask": attention_mask.numpy()
            }
        )
        
        print(f"   Output shape: {outputs[0].shape}")
        print("   ✅ ONNX Runtime works!")
        
        return True
        
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()
        return False

if __name__ == "__main__":
    print("\n🚀 Starting ONNX export for SafeSpark production deployment...")
    
    success = export_onnx()
    
    if success:
        print("\n" + "=" * 70)
        print("✅ SUCCESS! ONNX model is ready for production!")
        print("=" * 70)
        print("\n📋 Next steps:")
        print("1. cd /Users/knutludtmann/AndroidStudioProjects/SafeSpark")
        print("2. ./gradlew clean assembleDebug")
        print("3. ./gradlew connectedDebugAndroidTest")
        print("\n🎯 All 45 tests should pass now!")
        sys.exit(0)
    else:
        print("\n" + "=" * 70)
        print("❌ FAILED! Could not export ONNX model")
        print("=" * 70)
        print("\n💡 Try:")
        print("pip3 install --upgrade optimum[exporters] onnxruntime")
        sys.exit(1)
