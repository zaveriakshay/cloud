package com.cloud.docs.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ContentManagementService {

    private static final String GUIDES_PATH = "docs/guides/";

    /**
     * Lists the available guide pages.
     * @return A list of page names (without the .md extension).
     */
    public List<String> listGuidePages() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:" + GUIDES_PATH + "*.md");
            return Arrays.stream(resources)
                    .map(Resource::getFilename)
                    .map(name -> name.replace(".md", ""))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Could not list guide pages.", e);
            return List.of();
        }
    }

    /**
     * Reads the raw markdown content of a specific guide page.
     * @param pageName The name of the page (e.g., "onboarding").
     * @return The raw markdown content as a string.
     */
    public String getPageContent(String pageName) throws IOException {
        Resource resource = new ClassPathResource(GUIDES_PATH + pageName + ".md");
        if (!resource.exists()) {
            throw new IOException("Guide page not found: " + pageName);
        }
        byte[] binaryData = FileCopyUtils.copyToByteArray(resource.getInputStream());
        return new String(binaryData, StandardCharsets.UTF_8);
    }

    /**
     * Saves (creates or overwrites) the content of a guide page.
     * NOTE: This writes directly to the filesystem and is suitable for a dev/single-user environment.
     * @param pageName The name of the page.
     * @param content The new markdown content.
     */
    public void savePageContent(String pageName, String content) throws IOException {
        // Sanitize pageName to prevent directory traversal
        if (!pageName.matches("^[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Invalid page name. Only alphanumeric characters, hyphens, and underscores are allowed.");
        }

        try {
            Resource resource = new ClassPathResource(GUIDES_PATH);
            File file = new File(resource.getFile().getAbsolutePath() + File.separator + pageName + ".md");
            log.info("Saving content to file: {}", file.getAbsolutePath());
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(content);
            }
        } catch (IOException e) {
            log.error("Failed to save content for page: {}", pageName, e);
            throw new IOException("Could not save the guide page. Check file permissions.", e);
        }
    }
}