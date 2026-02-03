#!/usr/bin/env python3
"""
Download Pre-Built ONNX Model from HuggingFace

Many sentence-transformer models have pre-built ONNX versions available.
"""

import requests
from pathlib import Path
import sys

def download_onnx():
    print("=" * 70)
    print("Downloading Pre-Built ONNX Model from HuggingFace")
    print("=" * 70)
    
    # Output path
    output_dir = Path(__file__).parent.parent / "app" / "src" / "main" / "assets"
    output_file = output_dir / "minilm_encoder.onnx"
    output_dir.mkdir(parents=True, exist_ok=True)
    
    # Try different ONNX model sources
    urls = [
        # ONNX optimized version
        "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/onnx/model.onnx",
        "https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/model.onnx",
        # Optimum ONNX models
        "https://huggingface.co/optimum/paraphrase-multilingual-MiniLM-L12-v2/resolve/main/model.onnx",
    ]
    
    for i, url in enumerate(urls, 1):
        print(f"\n{i}. Trying: {url}")
        
        try:
            response = requests.get(url, stream=True, timeout=30)
            
            if response.status_code == 200:
                print("   ✅ Found ONNX model! Downloading...")
                
                total_size = int(response.headers.get('content-length', 0))
                downloaded = 0
                
                with open(output_file, 'wb') as f:
                    for chunk in response.iter_content(chunk_size=8192):
                        if chunk:
                            f.write(chunk)
                            downloaded += len(chunk)
                            
                            if total_size > 0:
                                progress = (downloaded / total_size) * 100
                                print(f"\r   Progress: {progress:.1f}%", end='', flush=True)
                
                print()  # New line after progress
                
                size_mb = output_file.stat().st_size / (1024 * 1024)
                print(f"   ✅ Downloaded: {size_mb:.2f} MB")
                print(f"   📁 Saved to: {output_file}")
                
                return True
                
            else:
                print(f"   ❌ Not found (HTTP {response.status_code})")
                
        except Exception as e:
            print(f"   ❌ Failed: {e}")
            continue
    
    return False

if __name__ == "__main__":
    print("\n🚀 Attempting to download pre-built ONNX model...")
    
    success = download_onnx()
    
    if success:
        print("\n" + "=" * 70)
        print("✅ SUCCESS! ONNX model downloaded!")
        print("=" * 70)
        print("\n📋 Next steps:")
        print("1. cd /Users/knutludtmann/AndroidStudioProjects/SafeSpark")
        print("2. ./gradlew clean assembleDebug")
        print("3. ./gradlew connectedDebugAndroidTest")
        sys.exit(0)
    else:
        print("\n" + "=" * 70)
        print("❌ FAILED! No pre-built ONNX model found")
        print("=" * 70)
        print("\n💡 Alternative: Convert manually with transformers.onnx")
        sys.exit(1)
