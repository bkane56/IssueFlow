package com.issueflow.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class SqliteDataDirectoryConfig {

    @PostConstruct
    public void createDataDirectory() throws IOException {
        Files.createDirectories(Path.of("data"));
    }
}
