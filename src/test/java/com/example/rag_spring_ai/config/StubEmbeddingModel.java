package com.example.rag_spring_ai.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple stub EmbeddingModel for unit tests.
 * Returns deterministic zero-filled float arrays so no network call is made.
 * Reused across unit tests that need a real SimpleVectorStore but no AI infrastructure.
 */
public class StubEmbeddingModel implements EmbeddingModel {

    private final int dimensions;

    public StubEmbeddingModel() {
        this(768);
    }

    public StubEmbeddingModel(int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        for (int i = 0; i < request.getInstructions().size(); i++) {
            embeddings.add(new Embedding(nonZeroVector(), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public float[] embed(String text) {
        return nonZeroVector();
    }

    @Override
    public float[] embed(Document document) {
        return nonZeroVector();
    }

    /** Returns a unit vector (all values = 1/sqrt(dimensions)) to satisfy cosine-similarity requirements. */
    private float[] nonZeroVector() {
        float[] v = new float[dimensions];
        float val = (float) (1.0 / Math.sqrt(dimensions));
        java.util.Arrays.fill(v, val);
        return v;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }
}


