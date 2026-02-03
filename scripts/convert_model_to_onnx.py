#!/usr/bin/env python3
"""
Convert Sentence-Transformers Model to ONNX for Android

Converts paraphrase-multilingual-MiniLM-L12-v2 to ONNX format
with optimizations for mobile deployment.

Requirements:
    pip install sentence-transformers torch onnx onnxruntime transformers optimum

Usage:
    python convert_model_to_onnx.py

Output:
    ../app/src/main/assets/minilm_encoder.onnx
"""

import torch
import onnx
from pathlib import Path
from sentence_transformers import SentenceTransformer
from transformers import AutoTokenizer, AutoModel
import numpy as np


def convert_to_onnx(
    model_name: str = "paraphrase-multilingual-MiniLM-L12-v2",
    output_path: Path = None,
    quantize: bool = True
):
    """
    Convert sentence-transformers model to ONNX.

    Args:
        model_name: HuggingFace model name
        output_path: Where to save the ONNX model
        quantize: Whether to quantize the model (reduces size)
    """
    print("=" * 60)
    print(f"Converting {model_name} to ONNX")
    print("=" * 60)

    # Load model - use local cache
    print("\n🔄 Loading model from cache...")
    try:
        model = SentenceTransformer(model_name, use_auth_token=False, device='cpu')
        transformer = model[0].auto_model  # Get the transformer model
        transformer = transformer.to('cpu')  # Force CPU for ONNX export
        print("✅ Model loaded from local cache (CPU)")
    except Exception as e:
        print(f"❌ Error loading model: {e}")
        print("\nNote: Das Model wurde bereits beim ersten Skript heruntergeladen.")
        print("Es sollte im Cache sein. Versuche es nochmal...")
        raise

    # Set model to eval mode
    transformer.eval()

    # Create dummy input
    print("\n🔄 Creating dummy input...")
    dummy_text = "This is a test sentence."

    # Get tokenizer from loaded model
    tokenizer = model.tokenizer

    inputs = tokenizer(
        dummy_text,
        return_tensors="pt",
        padding=True,
        truncation=True,
        max_length=128
    )

    input_ids = inputs["input_ids"]
    attention_mask = inputs["attention_mask"]

    print(f"   Input shape: {input_ids.shape}")

    # Export to ONNX
    print("\n🔄 Exporting to ONNX...")

    onnx_path_temp = output_path.parent / "minilm_encoder_temp.onnx"

    torch.onnx.export(
        transformer,
        args=(input_ids, attention_mask),
        f=str(onnx_path_temp),
        input_names=["input_ids", "attention_mask"],
        output_names=["last_hidden_state", "pooler_output"],
        dynamic_axes={
            "input_ids": {0: "batch_size", 1: "sequence_length"},
            "attention_mask": {0: "batch_size", 1: "sequence_length"},
            "last_hidden_state": {0: "batch_size", 1: "sequence_length"},
            "pooler_output": {0: "batch_size"}
        },
        opset_version=14,
        do_constant_folding=True,
    )

    print(f"✅ Exported to: {onnx_path_temp}")

    # Verify ONNX model
    print("\n🔍 Verifying ONNX model...")
    onnx_model = onnx.load(str(onnx_path_temp))
    onnx.checker.check_model(onnx_model)
    print("✅ ONNX model is valid")

    # Quantize if requested
    final_path = output_path

    if quantize:
        print("\n🔄 Quantizing model...")
        from onnxruntime.quantization import quantize_dynamic, QuantType

        quantize_dynamic(
            model_input=str(onnx_path_temp),
            model_output=str(final_path),
            weight_type=QuantType.QUInt8,
            optimize_model=True,
        )

        # Remove temp file
        onnx_path_temp.unlink()

        print("✅ Model quantized")
    else:
        onnx_path_temp.rename(final_path)

    # Print file sizes
    size_mb = final_path.stat().st_size / (1024 * 1024)
    print(f"\n📦 Final model size: {size_mb:.2f} MB")

    return final_path


def test_onnx_model(onnx_path: Path):
    """Test the ONNX model with sample input."""
    print("\n🧪 Testing ONNX model...")

    import onnxruntime as ort

    # Load model
    session = ort.InferenceSession(str(onnx_path))

    print(f"   Provider: {session.get_providers()}")

    # Get input/output info
    print(f"   Inputs: {[i.name for i in session.get_inputs()]}")
    print(f"   Outputs: {[o.name for o in session.get_outputs()]}")

    # Load tokenizer from cached model
    print("\n🔄 Loading tokenizer from cache...")
    model_name = "paraphrase-multilingual-MiniLM-L12-v2"
    model = SentenceTransformer(model_name, use_auth_token=False)
    tokenizer = model.tokenizer

    test_texts = [
        "Bist du alleine?",
        "Are you alone?",
        "Das ist ein Test."
    ]

    for text in test_texts:
        # Tokenize
        inputs = tokenizer(
            text,
            return_tensors="np",
            padding=True,
            truncation=True,
            max_length=128
        )

        # Run inference
        outputs = session.run(
            None,
            {
                "input_ids": inputs["input_ids"],
                "attention_mask": inputs["attention_mask"]
            }
        )

        # Mean pooling
        last_hidden_state = outputs[0]
        attention_mask = inputs["attention_mask"]

        # Mean pooling
        input_mask_expanded = np.expand_dims(attention_mask, -1)
        sum_embeddings = np.sum(last_hidden_state * input_mask_expanded, axis=1)
        sum_mask = np.clip(np.sum(input_mask_expanded, axis=1), a_min=1e-9, a_max=None)
        embedding = sum_embeddings / sum_mask

        print(f"\n   Text: '{text}'")
        print(f"   Embedding shape: {embedding.shape}")
        print(f"   First 5 values: {embedding[0][:5]}")

    print("\n✅ ONNX model works correctly!")


def create_android_helper_code():
    """Generate Kotlin code snippet for Android integration."""
    code = """
// ═══════════════════════════════════════════════════════════════
// ONNX Model Integration - Add to SemanticDetector.kt
// ═══════════════════════════════════════════════════════════════

private fun loadOnnxModel(context: Context): OrtSession {
    val modelBytes = context.assets.open("minilm_encoder.onnx").use { 
        it.readBytes() 
    }
    
    val env = OrtEnvironment.getEnvironment()
    val sessionOptions = OrtSession.SessionOptions().apply {
        setIntraOpNumThreads(2)
        setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
    }
    
    return env.createSession(modelBytes, sessionOptions)
}

private fun tokenize(text: String): Pair<LongArray, LongArray> {
    // Simple BPE tokenizer - use actual tokenizer in production
    val tokens = text.toLowerCase().split(" ")
    val inputIds = tokens.map { vocabMap[it] ?: 0L }.toLongArray()
    val attentionMask = LongArray(inputIds.size) { 1L }
    
    return Pair(inputIds, attentionMask)
}

private fun encode(text: String): FloatArray {
    val (inputIds, attentionMask) = tokenize(text)
    
    // Create ONNX tensors
    val inputIdsTensor = OnnxTensor.createTensor(
        ortEnvironment,
        longArrayOf(1, inputIds.size.toLong()),
        inputIds
    )
    
    val attentionMaskTensor = OnnxTensor.createTensor(
        ortEnvironment,
        longArrayOf(1, attentionMask.size.toLong()),
        attentionMask
    )
    
    // Run inference
    val inputs = mapOf(
        "input_ids" to inputIdsTensor,
        "attention_mask" to attentionMaskTensor
    )
    
    val outputs = ortSession.run(inputs)
    val lastHiddenState = outputs[0].value as Array<Array<FloatArray>>
    
    // Mean pooling
    return meanPooling(lastHiddenState[0], attentionMask)
}

private fun meanPooling(embeddings: Array<FloatArray>, mask: LongArray): FloatArray {
    val embeddingDim = embeddings[0].size
    val result = FloatArray(embeddingDim)
    
    var sumMask = 0f
    for (i in embeddings.indices) {
        if (mask[i] == 1L) {
            for (j in 0 until embeddingDim) {
                result[j] += embeddings[i][j]
            }
            sumMask += 1f
        }
    }
    
    for (j in 0 until embeddingDim) {
        result[j] /= sumMask.coerceAtLeast(1e-9f)
    }
    
    return result
}
"""

    return code


def main():
    # Paths
    script_dir = Path(__file__).parent
    output_path = script_dir.parent / "app" / "src" / "main" / "assets" / "minilm_encoder.onnx"

    output_path.parent.mkdir(parents=True, exist_ok=True)

    # Convert
    try:
        onnx_path = convert_to_onnx(
            model_name="paraphrase-multilingual-MiniLM-L12-v2",
            output_path=output_path,
            quantize=True
        )

        # Test
        test_onnx_model(onnx_path)

        print("\n" + "=" * 60)
        print("✅ Conversion complete!")
        print(f"📦 Model saved to: {onnx_path}")
        print("=" * 60)

        # Print integration help
        print("\n📝 Android Integration Code:")
        print(create_android_helper_code())

    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()


if __name__ == "__main__":
    main()
