package com.app.document.serach.app.service;

import com.app.document.serach.app.model.entity.EmbeddingDocument;
import com.app.document.serach.app.repository.EmbeddingRepository;
import org.springframework.cache.Cache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Service
public class EmbeddingService {
    private final OpenAiService openAiService;
    private final EmbeddingRepository embeddingRepository;
    private final CaffeineCacheManager cacheManager;

    public EmbeddingService(OpenAiService openAiService, EmbeddingRepository embeddingRepository, CaffeineCacheManager cacheManager) {
        this.openAiService = openAiService;
        this.embeddingRepository = embeddingRepository;
        this.cacheManager = cacheManager;
    }

    // returns embedding and stores it in Mongo + cache
    public Mono<List<Double>> getOrCreateEmbedding(String fileId, String textSnippet, String userId) {
        Cache cache = cacheManager.getCache("embeddings");
        // check cache first
        List<Double> cached = cache != null ? cache.get(fileId, List.class) : null;
        if (cached != null) return Mono.just(cached);

        // check DB then OpenAI
        return embeddingRepository.findById(fileId)
                .map(EmbeddingDocument::getVector)
                .switchIfEmpty(openAiService.embeddings(textSnippet)
                        .flatMap(vec -> {
                            EmbeddingDocument doc = new EmbeddingDocument();
                            doc.setId(fileId);
                            doc.setUserId(userId);
                            doc.setVector(vec);
                            doc.setTextSnippet(textSnippet.length() > 1000 ? textSnippet.substring(0,1000) : textSnippet);
                            doc.setCreatedAt(Instant.now().toEpochMilli());
                            return embeddingRepository.save(doc).map(saved -> {
                                if (cache != null) cache.put(fileId, vec);
                                return vec;
                            });
                        })
                );
    }
}
