package com.kaoshi.question.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class QuestionSetResourceImporter implements ApplicationRunner {
    private final ObjectMapper objectMapper;
    private final QuestionSetJdbcWriter writer;

    public QuestionSetResourceImporter(ObjectMapper objectMapper, QuestionSetJdbcWriter writer) {
        this.objectMapper = objectMapper;
        this.writer = writer;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        QuestionSetIndex index = read("question-sets/index.json", QuestionSetIndex.class);
        for (String file : index.files()) {
            QuestionSetResource resource = read(file, QuestionSetResource.class);
            writer.importSet(file, resource);
        }
    }

    private <T> T read(String path, Class<T> type) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (var input = resource.getInputStream()) {
            return objectMapper.readValue(input, type);
        }
    }

    private record QuestionSetIndex(List<String> files) {
    }
}
