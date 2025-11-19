package com.app.document.serach.app.service;


import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class OpenAiService {

    private final ChatModel chatModel;
    private final EmbeddingModel embeddingModel;

    public OpenAiService(ChatModel chatModel, EmbeddingModel embeddingModel) {
        this.chatModel = chatModel;
        this.embeddingModel = embeddingModel;
    }

    /**
     * Generate embeddings using Spring AI EmbeddingModel (reactive)
     */
    /*public Mono<List<Double>> embeddings(String input) {
        return Mono.fromCallable(() -> {
            float[] vector = embeddingModel.embed(input);  // returns float[]

            List<Double> list = new java.util.ArrayList<>(vector.length);
            for (float f : vector) {
                list.add((double) f);  // Convert float → Double
            }

            return list;
        });
    }*/

    public Mono<List<Double>> embeddings(String input) {
        List<String> chunks = chunk(input, 8000);


        return Flux.fromIterable(chunks)
                .map(chunk -> {
                    float[] vector = embeddingModel.embed(chunk);
                    List<Double> list = new ArrayList<>(vector.length);
                    for (float f : vector) list.add((double) f);
                    return list;
                })
                .collectList()
                .map(OpenAiService::averageVectors);  // combine into one vector
    }
    public static List<String> chunk(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }
        return chunks;
    }

    private static List<Double> averageVectors(List<List<Double>> vectors) {
        int size = vectors.get(0).size();
        double[] sum = new double[size];

        for (List<Double> vec : vectors)
            for (int i = 0; i < size; i++)
                sum[i] += vec.get(i);

        List<Double> result = new ArrayList<>(size);
        for (double v : sum)
            result.add(v / vectors.size());

        return result;
    }

    /**
     * Send a prompt to OpenAI Chat Model (reactive)
     */
    public Mono<String> ask(String prompt) {
        return Mono.fromCallable(() -> chatModel.call(new UserMessage(prompt)));
    }

    public Mono<List<Double>> embeddings1(String input) {
        return Mono.fromCallable(() -> {
                    float[] vector = embeddingModel.embed(input);  // <-- blocking call!
                    List<Double> list = new ArrayList<>(vector.length);
                    for (float f : vector) list.add((double) f);
                    return list;
                })
                .subscribeOn(Schedulers.boundedElastic());  // <-- required fix
    }
}


