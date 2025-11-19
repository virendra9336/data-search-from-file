package com.app.document.serach.app.controller;


import com.app.document.serach.app.model.entity.User;
import com.app.document.serach.app.repository.UserRepository;
import com.app.document.serach.app.service.GridFsService;
import com.app.document.serach.app.service.RagService;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;
    private final GridFsService gridFsService;
    private final RagService ragService;

    public UserController(UserRepository ur, GridFsService gfs, RagService rs){
        this.userRepository = ur; this.gridFsService = gfs; this.ragService = rs;
    }

    // create user with biodata file upload
    @PostMapping(value = "/create", consumes = {"multipart/form-data"})
    public Mono<User> createUser(@RequestPart("user") Mono<User> userMono,
                                 @RequestPart("biodata") Mono<FilePart> fileMono) {
        return userMono.zipWith(fileMono)
                .flatMap(tuple -> {
                    User u = tuple.getT1();
                    FilePart file = tuple.getT2();
                    return gridFsService.store(file)
                            .flatMap(fileId -> {
                                u.setBiodataFileId(fileId);
                                return userRepository.save(u);
                            });
                });
    }

    // get top 20 users
    @GetMapping("/top20")
    public Flux<User> getTop20() {
        return ragService.top20Users();
    }

    // enrich top20 (create embeddings for biodata, cached)
    @GetMapping("/top20/enrich")
    public Flux<User> getTop20Enriched() {
        return ragService.enrichTop20WithEmbeddings();
    }

    // semantic search across biodata embeddings and then run extract on the matched biodata
    @PostMapping("/search")
    public Mono<Object> search(@RequestParam String query, @RequestParam(defaultValue = "3") int topK){
        return ragService.semanticSearch(query, topK)
                .flatMapMany(Flux::fromIterable)
                .flatMap(doc -> {
                    // load the biodata for each match and run a short extraction
                    return gridFsService.getFileContentAsString(doc.getId())
                            .flatMap(text -> ragService.extractFromBiodata(text, query)
                                    .map(result -> java.util.Map.of("fileId", doc.getId(), "answer", result)));
                })
                .collectList()
                .map(list -> java.util.Map.of("query", query, "results", list));
    }
}
