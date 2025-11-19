package com.app.document.serach.app.service;

import com.app.document.serach.app.model.entity.EmbeddingDocument;
import com.app.document.serach.app.model.entity.User;
import com.app.document.serach.app.repository.EmbeddingRepository;
import com.app.document.serach.app.repository.UserRepository;
import com.app.document.serach.app.util.VectorUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Comparator;
import java.util.List;

@Service
public class RagService {
    private final UserRepository userRepository;
    private final GridFsService gridFsService;
    private final EmbeddingService embeddingService;
    private final OpenAiService openAiService;
    private final EmbeddingRepository embeddingRepository;

    public RagService(UserRepository ur, GridFsService gfs, EmbeddingService es, OpenAiService oas, EmbeddingRepository er){
        this.userRepository = ur; this.gridFsService = gfs; this.embeddingService = es;
        this.openAiService = oas; this.embeddingRepository = er;
    }

    // get top 20 user records by createdAt desc
    public Flux<User> top20Users() {
        return userRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0,20));
    }

    // For a search query: compute embedding of query, compute cosine with stored embeddings and return matches
    public Mono<List<EmbeddingDocument>> semanticSearch(String query, int topK) {
        return openAiService.embeddings1(query)
                .flatMap(queryVec ->
                        embeddingRepository.findAll()
                                .collectList()
                                .map(list -> {
                                    // score in memory
                                    return list.stream()
                                            .map(d -> new java.util.AbstractMap.SimpleEntry<>(d, VectorUtils.cosineSimilarity(d.getVector(), queryVec)))
                                            .sorted((a,b)->Double.compare(b.getValue(), a.getValue()))
                                            .limit(topK)
                                            .map(java.util.Map.Entry::getKey)
                                            .toList();
                                })
                ).single();
    }

    // high-level method: get top20 users, for each fetch biodata text and create embeddings (cached)
    public Flux<User> enrichTop20WithEmbeddings(){
        return top20Users()
                .flatMap(user -> gridFsService.getFileContentAsString(user.getBiodataFileId())
                        .flatMap(content -> embeddingService.getOrCreateEmbedding(user.getBiodataFileId(), content, user.getId())
                                .thenReturn(user)
                        )
                        .onErrorResume(e -> Mono.just(user)) // don't fail entire flow if file read fails
                );
    }

    // extract using model (RAG style): send snippet + question to OpenAI to extract certain contents
    public Mono<String> extractFromBiodata(String biodataText, String question) {
        String prompt = "You are a helpful assistant. Given the biodata text below, answer the question concisely.\n\nBiodata:\n"
                + biodataText + "\n\nQuestion: " + question;
        return openAiService.ask(prompt);
    }
}
