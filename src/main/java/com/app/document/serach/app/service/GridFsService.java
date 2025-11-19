package com.app.document.serach.app.service;
import org.bson.types.ObjectId;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsTemplate;
import org.springframework.data.mongodb.gridfs.ReactiveGridFsResource;
import reactor.core.publisher.Mono;
import org.springframework.http.codec.multipart.FilePart;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Service
public class GridFsService {
    private final ReactiveGridFsTemplate gridFsTemplate;

    public GridFsService(ReactiveGridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }

    // store file and return file id as String
    public Mono<String> store(FilePart file) {
        return gridFsTemplate.store(file.content(), file.filename())
                .map(Object::toString);
    }

    // fetch file content as string (assumes text-based file)
    public Mono<String> getFileContentAsString(String fileId) {
        return gridFsTemplate
                .findOne(Query.query(Criteria.where("_id").is(new ObjectId(fileId))))
                .flatMap(gridFsTemplate::getResource)
                .flatMap(res -> DataBufferUtils.join(res.getDownloadStream()))
                .map(buf -> {
                    byte[] bytes = new byte[buf.readableByteCount()];
                    buf.read(bytes);
                    DataBufferUtils.release(buf);
                    return new String(bytes, StandardCharsets.UTF_8);
                });
    }
}
