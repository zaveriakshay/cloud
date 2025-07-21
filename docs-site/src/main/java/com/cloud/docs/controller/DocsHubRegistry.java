package com.cloud.docs.controller;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.parser.Parser;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Spring-managed service that discovers and manages all available documentation hubs.
 * It scans the `resources/docs` directory for top-level folders and creates a
 * DocsHubService instance for each one.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocsHubRegistry {

    private final Parser markdownParser;
    private final Map<String, DocsHubService> hubs = new ConcurrentHashMap<>();

    @PostConstruct
    public void initialize() {
        log.info("Initializing Docs Hub Registry: Scanning for documentation hubs...");
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            // Scan for directories directly under `classpath:docs/`
            Resource[] resources = resolver.getResources("classpath:docs/*");
            for (Resource resource : resources) {
                if (resource.exists() && resource.getURL().getPath().endsWith("/")) {
                    String hubName = resource.getFilename();
                    if (hubName != null && !hubName.isEmpty()) {
                        log.info("Discovered documentation hub: {}", hubName);
                        DocsHubService hubService = new DocsHubService(hubName, markdownParser);
                        hubService.initialize(); // Load content for this hub
                        hubs.put(hubName, hubService);
                    }
                }
            }
        } catch (IOException e) {
            log.error("Failed to scan for documentation hubs.", e);
        }
        log.info("Docs Hub Registry initialized with {} hub(s).", hubs.size());
    }

    /**
     * Retrieves a specific documentation hub by its name (folder name).
     * @param hubName The name of the hub.
     * @return An Optional containing the DocsHubService if found.
     */
    public Optional<DocsHubService> getHub(String hubName) {
        return Optional.ofNullable(hubs.get(hubName));
    }

    /**
     * Returns a collection of all registered documentation hubs.
     * @return A collection of all DocsHubService instances.
     */
    public Collection<DocsHubService> getAllHubs() {
        return hubs.values();
    }
}