package ru.itq.documents.service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.workers")
@Data
public class WorkersConfig {
    private int batchSize = 100;
    private long submitInterval = 5000;
    private long approveInterval = 5000;
    private boolean enabled = true;
}

