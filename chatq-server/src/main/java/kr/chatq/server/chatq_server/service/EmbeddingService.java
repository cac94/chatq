package kr.chatq.server.chatq_server.service;

import com.openai.client.OpenAIClient;
import com.openai.models.Embedding;
import com.openai.models.EmbeddingCreateParams;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class EmbeddingService {

    private final OpenAIClient openAIClient;

    @Value("${spring.ai.openai.embedding.options.model:text-embedding-3-small}")
    private String embeddingModel;

    @Value("${spring.ai.embedding.type:openai}")
    private String embeddingType;

    @Value("${spring.ai.ollama.embedding.options.model:}")
    private String ollamaEmbeddingModelName;

    @Autowired(required = false)
    private OllamaEmbeddingModel ollamaEmbeddingModel;

    private static final Map<String, Map<String, List<Double>>> storage = new ConcurrentHashMap<>();
    private Runnable initializer;
    private static final AtomicBoolean initializing = new AtomicBoolean(false);

    public EmbeddingService(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }

    public List<Double> embed(String text) {
        if ("ollama".equalsIgnoreCase(embeddingType)) {
            if (ollamaEmbeddingModel == null) {
                throw new IllegalStateException("OllamaEmbeddingModel is not configured.");
            }

            OllamaOptions options = OllamaOptions.builder()
                    .model(ollamaEmbeddingModelName)
                    .build();
            EmbeddingRequest request = new EmbeddingRequest(Collections.singletonList(text), options);
            float[] floatVector = ollamaEmbeddingModel.call(request).getResult().getOutput();

            List<Double> doubleVector = new ArrayList<>(floatVector.length);
            for (float f : floatVector) {
                doubleVector.add((double) f);
            }
            return doubleVector;
        }

        // Default to OpenAI
        EmbeddingCreateParams params = EmbeddingCreateParams.builder()
                .model(embeddingModel)
                .input(EmbeddingCreateParams.Input.ofString(text))
                .build();

        Embedding embedding = openAIClient.embeddings().create(params)
                .data()
                .get(0);

        return embedding.embedding();
    }

    public void save(String dbName, String text, List<Double> vector) {
        storage.computeIfAbsent(dbName, k -> new ConcurrentHashMap<>()).put(text, vector);
    }

    public List<Double> get(String dbName, String text) {
        Map<String, List<Double>> dbStorage = storage.get(dbName);
        return dbStorage != null ? dbStorage.get(text) : null;
    }

    public void clear(String dbName) {
        Map<String, List<Double>> dbStorage = storage.get(dbName);
        if (dbStorage != null) {
            dbStorage.clear();
        }
    }

    public void setInitializer(Runnable initializer) {
        this.initializer = initializer;
    }

    public List<Map.Entry<String, Double>> search(String dbName, String queryText, int topN) {
        if ((!storage.containsKey(dbName) || storage.get(dbName).isEmpty()) && initializer != null) {
            if (initializing.compareAndSet(false, true)) {
                try {
                    initializer.run();
                } finally {
                    initializing.set(false);
                }
            }
        }

        Map<String, List<Double>> dbStorage = storage.getOrDefault(dbName, new HashMap<>());
        List<Double> queryVector = embed(queryText);

        return dbStorage.entrySet().stream()
                .map(entry -> new AbstractMap.SimpleEntry<>(
                        entry.getKey(),
                        cosineSimilarity(queryVector, entry.getValue())))
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(topN)
                .collect(Collectors.toList());
    }

    private double cosineSimilarity(List<Double> v1, List<Double> v2) {
        if (v1.size() != v2.size())
            return 0.0;

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < v1.size(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            normA += Math.pow(v1.get(i), 2);
            normB += Math.pow(v2.get(i), 2);
        }

        if (normA == 0 || normB == 0)
            return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
