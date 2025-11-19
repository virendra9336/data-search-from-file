package com.app.document.serach.app.repository;

import com.app.document.serach.app.model.entity.EmbeddingDocument;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface EmbeddingRepository extends ReactiveMongoRepository<EmbeddingDocument, String> {
    Flux<EmbeddingDocument> findAll();
}

