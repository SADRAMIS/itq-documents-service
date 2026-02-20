package ru.itq.documents.service.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import ru.itq.documents.service.dto.CreateDocumentRequest;
import ru.itq.documents.service.dto.DocumentDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Утилита для массового создания документов через API сервиса.
 * Читает параметр N из файла config.properties и создает N документов.
 */
@Slf4j
public class DocumentGeneratorUtil {

    private static final String CONFIG_FILE = "config.properties";
    private static final String DEFAULT_API_URL = "http://localhost:8080/api/documents";
    private static final String DEFAULT_INITIATOR = "document-generator";

    public static void main(String[] args) {
        try {
            Properties config = loadConfig();
            int n = Integer.parseInt(config.getProperty("N", "100"));
            String apiUrl = config.getProperty("api.url", DEFAULT_API_URL);
            String initiator = config.getProperty("initiator", DEFAULT_INITIATOR);

            log.info("Starting document generation: N={}, API URL={}", n, apiUrl);

            RestTemplate restTemplate = new RestTemplate();
            List<DocumentDto> created = new ArrayList<>();
            long startTime = System.currentTimeMillis();

            for (int i = 0; i < n; i++) {
                try {
                    CreateDocumentRequest request = new CreateDocumentRequest();
                    request.setAuthor("Author-" + (i % 10));
                    request.setTitle("Document " + (i + 1));
                    request.setContent("Content for document " + (i + 1));

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("X-Initiator", initiator);
                    HttpEntity<CreateDocumentRequest> entity = new HttpEntity<>(request, headers);

                    ResponseEntity<DocumentDto> response = restTemplate.postForEntity(
                            apiUrl, entity, DocumentDto.class);

                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        created.add(response.getBody());
                        if ((i + 1) % 100 == 0) {
                            log.info("Progress: {}/{} documents created", i + 1, n);
                        }
                    } else {
                        log.warn("Failed to create document {}: {}", i + 1, response.getStatusCode());
                    }
                } catch (Exception e) {
                    log.error("Error creating document {}: {}", i + 1, e.getMessage());
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Document generation completed: {}/{} documents created in {}ms (avg: {}ms per document)",
                    created.size(), n, duration, duration / (double) n);

        } catch (Exception e) {
            log.error("Fatal error in document generation", e);
            System.exit(1);
        }
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        try {
            java.io.InputStream input = DocumentGeneratorUtil.class.getClassLoader()
                    .getResourceAsStream(CONFIG_FILE);
            if (input != null) {
                props.load(input);
            } else {
                log.warn("Config file {} not found, using defaults", CONFIG_FILE);
            }
        } catch (Exception e) {
            log.warn("Error loading config file, using defaults: {}", e.getMessage());
        }
        return props;
    }
}

