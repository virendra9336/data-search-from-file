package com.app.document.serach.app.repository;


import com.app.document.serach.app.model.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

public interface UserRepository extends ReactiveMongoRepository<User, String> {
    Flux<User> findAllByOrderByCreatedAtDesc(Pageable pageable);
}