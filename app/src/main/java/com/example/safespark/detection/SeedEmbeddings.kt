package com.example.safespark.detection

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Loader for pre-computed seed embeddings
 *
 * Loads seed_embeddings.json from assets and provides
 * access to embeddings for each intent category.
 *
 * Format:
 * {
 *   "model": "paraphrase-multilingual-MiniLM-L12-v2",
 *   "embedding_dim": 384,
 *   "intents": {
 *     "SUPERVISION_CHECK": {
 *       "seeds": ["Bist du alleine?", ...],
 *       "embeddings": [[0.23, -0.45, ...], ...]
 *     }
 *   }
 * }
 */
class SeedEmbeddings private constructor(
    val modelName: String,
    val embeddingDim: Int,
    private val intentEmbeddings: Map<String, IntentEmbeddings>
) {

    data class IntentEmbeddings(
        val intentName: String,
        val seeds: List<String>,
        val embeddings: List<FloatArray>
    ) {
        init {
            require(seeds.size == embeddings.size) {
                "Seeds and embeddings must have same size"
            }
        }

        val count: Int get() = seeds.size
    }

    /**
     * Get embeddings for a specific intent
     */
    fun getIntent(intentName: String): IntentEmbeddings? {
        return intentEmbeddings[intentName]
    }

    /**
     * Get all intent names
     */
    fun getIntentNames(): Set<String> {
        return intentEmbeddings.keys
    }

    /**
     * Get total number of seed patterns across all intents
     */
    fun getTotalSeedCount(): Int {
        return intentEmbeddings.values.sumOf { it.count }
    }

    companion object {
        private const val TAG = "SeedEmbeddings"
        private const val ASSET_FILE = "seed_embeddings.json"

        @Volatile
        private var INSTANCE: SeedEmbeddings? = null

        /**
         * Load seed embeddings from assets (singleton)
         */
        fun getInstance(context: Context): SeedEmbeddings {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: load(context).also { INSTANCE = it }
            }
        }

        /**
         * Load seed embeddings from JSON file in assets
         */
        private fun load(context: Context): SeedEmbeddings {
            Log.d(TAG, "🔄 Loading seed embeddings from assets...")

            try {
                // Read JSON file
                val json = context.assets.open(ASSET_FILE).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.readText()
                    }
                }

                // Parse JSON
                val rootObject = JSONObject(json)

                val modelName = rootObject.getString("model")
                val embeddingDim = rootObject.getInt("embedding_dim")

                Log.d(TAG, "   Model: $modelName")
                Log.d(TAG, "   Embedding dimension: $embeddingDim")

                // Parse each intent
                val intentsObject = rootObject.getJSONObject("intents")
                val intentEmbeddings = mutableMapOf<String, IntentEmbeddings>()

                intentsObject.keys().forEach { intentName ->
                    val intentObject = intentsObject.getJSONObject(intentName)

                    // Parse seeds
                    val seedsArray = intentObject.getJSONArray("seeds")
                    val seeds = List(seedsArray.length()) { i ->
                        seedsArray.getString(i)
                    }

                    // Parse embeddings
                    val embeddingsArray = intentObject.getJSONArray("embeddings")
                    val embeddings = List(embeddingsArray.length()) { i ->
                        val embArray = embeddingsArray.getJSONArray(i)
                        FloatArray(embArray.length()) { j ->
                            embArray.getDouble(j).toFloat()
                        }
                    }

                    // Validate
                    require(embeddings.all { it.size == embeddingDim }) {
                        "$intentName: embedding dimension mismatch"
                    }

                    intentEmbeddings[intentName] = IntentEmbeddings(
                        intentName = intentName,
                        seeds = seeds,
                        embeddings = embeddings
                    )

                    Log.d(TAG, "   ✅ $intentName: ${seeds.size} seeds loaded")
                }

                val totalSeeds = intentEmbeddings.values.sumOf { it.count }
                Log.d(TAG, "✅ Loaded ${intentEmbeddings.size} intents, $totalSeeds total seeds")

                return SeedEmbeddings(
                    modelName = modelName,
                    embeddingDim = embeddingDim,
                    intentEmbeddings = intentEmbeddings
                )

            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load seed embeddings", e)
                throw RuntimeException("Failed to load seed embeddings from $ASSET_FILE", e)
            }
        }

        /**
         * Reset instance (for testing)
         */
        fun resetInstance() {
            synchronized(this) {
                INSTANCE = null
            }
        }
    }
}

/**
 * Utility functions for working with embeddings
 */
object EmbeddingUtils {

    /**
     * Calculate cosine similarity between two embeddings
     *
     * Formula: cos(θ) = (A·B) / (||A|| * ||B||)
     *
     * @return Similarity score between 0.0 and 1.0
     */
    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        require(a.size == b.size) { "Embeddings must have same dimension" }

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        normA = kotlin.math.sqrt(normA)
        normB = kotlin.math.sqrt(normB)

        // Avoid division by zero
        if (normA < 1e-9f || normB < 1e-9f) {
            return 0f
        }

        val similarity = dotProduct / (normA * normB)

        // Clamp to [0, 1] (cosine can be -1 to 1, but we only care about positive similarity)
        return similarity.coerceIn(0f, 1f)
    }

    /**
     * Find the maximum similarity between an embedding and a list of seed embeddings
     *
     * @return Pair of (maxSimilarity, indexOfBestMatch)
     */
    fun maxSimilarity(
        embedding: FloatArray,
        seedEmbeddings: List<FloatArray>
    ): Pair<Float, Int> {
        var maxSim = 0f
        var maxIndex = 0

        seedEmbeddings.forEachIndexed { index, seedEmb ->
            val sim = cosineSimilarity(embedding, seedEmb)
            if (sim > maxSim) {
                maxSim = sim
                maxIndex = index
            }
        }

        return Pair(maxSim, maxIndex)
    }

    /**
     * Normalize an embedding to unit length (L2 normalization)
     */
    fun normalize(embedding: FloatArray): FloatArray {
        var norm = 0f
        for (value in embedding) {
            norm += value * value
        }
        norm = kotlin.math.sqrt(norm)

        if (norm < 1e-9f) {
            return embedding.copyOf()
        }

        return FloatArray(embedding.size) { i ->
            embedding[i] / norm
        }
    }
}
