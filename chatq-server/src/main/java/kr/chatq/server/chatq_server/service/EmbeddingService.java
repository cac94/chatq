package kr.chatq.server.chatq_server.service;

import com.openai.client.OpenAIClient;
import com.openai.models.Embedding;
import com.openai.models.EmbeddingCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@SessionScope
public class EmbeddingService {

    private final OpenAIClient openAIClient;

    @Value("${spring.ai.openai.embedding.options.model:text-embedding-3-small}")
    private String embeddingModel;

    private final Map<String, List<Double>> storage = new ConcurrentHashMap<>();
    private Runnable initializer;
    private final AtomicBoolean initializing = new AtomicBoolean(false);

    public EmbeddingService(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }

    public List<Double> embed(String text) {

        EmbeddingCreateParams params = EmbeddingCreateParams.builder()
                .model(embeddingModel)
                .input(EmbeddingCreateParams.Input.ofString(text))
                .build();

        Embedding embedding = openAIClient.embeddings().create(params)
                .data()
                .get(0);

        return embedding.embedding();
    }

    public void save(String text, List<Double> vector) {
        storage.put(text, vector);
    }

    public List<Double> get(String text) {
        return storage.get(text);
    }

    public void clear() {
        storage.clear();
    }

    public void setInitializer(Runnable initializer) {
        this.initializer = initializer;
    }

    public List<Map.Entry<String, Double>> search(String queryText, int topN) {
        if (storage.isEmpty() && initializer != null) {
            if (initializing.compareAndSet(false, true)) {
                try {
                    initializer.run();
                } finally {
                    initializing.set(false);
                }
            }
        }
        List<Double> queryVector = embed(queryText);

        return storage.entrySet().stream()
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
