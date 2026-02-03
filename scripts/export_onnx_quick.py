#!/usr/bin/env python3
"""
Quick ONNX Export using cached model

Uses the already downloaded sentence-transformers model from cache
and exports it to ONNX format for Android.
"""

import torch
import onnx
from pathlib import Path
from sentence_transformers import SentenceTransformer
import shutil

def main():
    print("=" * 60)
    print("Quick ONNX Export")
    print("=" * 60)

    model_name = "paraphrase-multilingual-MiniLM-L12-v2"
    output_path = Path(__file__).parent.parent / "app" / "src" / "main" / "assets" / "minilm_encoder.onnx"
    output_path.parent.mkdir(parents=True, exist_ok=True)

    try:
        # Load from cache
        print("\n1️⃣ Loading model from cache (CPU)...")
        model = SentenceTransformer(model_name, device='cpu')
        transformer = model[0].auto_model
        transformer = transformer.to('cpu')
        transformer.eval()

        print("✅ Model loaded")

        # Get tokenizer
        tokenizer = model.tokenizer

        # Create dummy input
        print("\n2️⃣ Creating dummy input...")
        dummy_text = "Test sentence for export"
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

        # Export to ONNX with simplified approach
        print("\n3️⃣ Exporting to ONNX...")
        temp_path = Path("./temp_model.onnx")

        # Use torch.jit.trace instead of torch.onnx.export
        print("   Using TorchScript trace...")

        # Wrap model to handle tuple output
        class ModelWrapper(torch.nn.Module):
            def __init__(self, model):
                super().__init__()
                self.model = model

            def forward(self, input_ids, attention_mask):
                outputs = self.model(input_ids=input_ids, attention_mask=attention_mask)
                return outputs.last_hidden_state

        wrapped = ModelWrapper(transformer)
        traced = torch.jit.trace(wrapped, (input_ids, attention_mask))

        # Convert traced model to ONNX
        torch.onnx.export(
            traced,
            (input_ids, attention_mask),
            temp_path,
            input_names=["input_ids", "attention_mask"],
            output_names=["last_hidden_state"],
            dynamic_axes={
                "input_ids": {0: "batch_size", 1: "sequence"},
                "attention_mask": {0: "batch_size", 1: "sequence"},
                "last_hidden_state": {0: "batch_size", 1: "sequence"}
            },
            opset_version=12,  # Lower opset for better compatibility
            do_constant_folding=True
        )

        print("✅ Exported to temporary file")

        # Verify ONNX
        print("\n4️⃣ Verifying ONNX model...")
        onnx_model = onnx.load(str(temp_path))
        onnx.checker.check_model(onnx_model)
        print("✅ ONNX model is valid")

        # Move to final location
        print("\n5️⃣ Moving to assets...")
        shutil.move(str(temp_path), str(output_path))

        # Check size
        size_mb = output_path.stat().st_size / (1024 * 1024)
        print(f"✅ Saved to: {output_path}")
        print(f"📦 File size: {size_mb:.2f} MB")

        # Quick test with ONNX Runtime
        print("\n6️⃣ Testing ONNX Runtime...")
        import onnxruntime as ort

        session = ort.InferenceSession(str(output_path))
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
        print("✅ ONNX Runtime works!")

        print("\n" + "=" * 60)
        print("✅ SUCCESS! ONNX model ready for Android!")
        print("=" * 60)
        print(f"\nNext steps:")
        print("1. cd /Users/knutludtmann/AndroidStudioProjects/SafeSpark")
        print("2. ./gradlew clean assembleDebug")
        print("3. ./gradlew installDebug")

    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()

        print("\n💡 Trying alternative approach...")

        # Alternative: Just use the PyTorch model directly
        try:
            print("Saving as TorchScript instead...")
            scripted = torch.jit.script(transformer)
            scripted.save(str(output_path.with_suffix('.pt')))
            print(f"✅ Saved TorchScript model to {output_path.with_suffix('.pt')}")
        except Exception as e2:
            print(f"❌ Alternative also failed: {e2}")

if __name__ == "__main__":
    main()
