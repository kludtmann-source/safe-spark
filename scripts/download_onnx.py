#!/usr/bin/env python3
"""
Download pre-built ONNX model from HuggingFace if available
"""

import requests
from pathlib import Path

def download_onnx():
    print("=" * 60)
    print("Trying to download pre-built ONNX model...")
    print("=" * 60)
    
    # Check if there's a pre-built ONNX model on HuggingFace
    model_id = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
    
    # Try ONNX model repo
    onnx_urls = [
        f"https://huggingface.co/{model_id}/resolve/main/onnx/model.onnx",
        f"https://huggingface.co/{model_id}/resolve/main/model.onnx",
    ]
    
    output_path = Path(__file__).parent.parent / "app" / "src" / "main" / "assets" / "minilm_encoder.onnx"
    output_path.parent.mkdir(parents=True, exist_ok=True)
    
    for url in onnx_urls:
        print(f"\n🔍 Trying: {url}")
        try:
            response = requests.get(url, stream=True, timeout=10)
            if response.status_code == 200:
                print("✅ Found ONNX model! Downloading...")
                
                with open(output_path, 'wb') as f:
                    for chunk in response.iter_content(chunk_size=8192):
                        f.write(chunk)
                
                size_mb = output_path.stat().st_size / (1024 * 1024)
                print(f"✅ Downloaded: {size_mb:.2f} MB")
                print(f"📁 Saved to: {output_path}")
                return True
                
        except Exception as e:
            print(f"❌ Failed: {e}")
            continue
    
    print("\n⚠️ No pre-built ONNX model found")
    print("\nAlternatives:")
    print("1. Use the app WITHOUT ONNX (works fine with BiLSTM)")
    print("2. Export manually with optimum-cli")
    print("3. Wait for ONNX export script to finish")
    return False

if __name__ == "__main__":
    download_onnx()
