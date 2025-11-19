package com.app.document.serach.app.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "embeddings")
public class EmbeddingDocument {
    @Id
    private String id;          // use biodataFileId as id
    private String userId;      // optional link to user
    private List<Double> vector;
    private String textSnippet; // small text snippet indexed
    private long createdAt;
    // getters/setters
}
