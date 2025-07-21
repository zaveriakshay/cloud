package com.cloud.docs.controller;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.commonmark.node.Heading;
import org.commonmark.node.Node;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A service class responsible for loading, parsing, and managing the content
 * for a single documentation hub (a top-level folder in `resources/docs`).
 * This class is NOT a Spring Bean; it's instantiated by the DocsHubRegistry.
 */
@Slf4j
@Getter
public class DocsHubService {

    private final String hubName;
    private final String hubRootPath;
    private final Parser markdownParser;

    private DocsArticleNode articleTree;
    private Map<String, DocsArticleNode> articleIndex;
    private List<DocsArticleNode> flatArticleList;
    private Map<String, DocsArticleNode> navigationMap;
    // This map stores which articles belong to which section, enabling auto-expansion.
    private Map<String, List<String>> descendantIdMap;

    public DocsHubService(String hubName, Parser markdownParser) {
        this.hubName = hubName;
        this.hubRootPath = "docs/" + hubName;
        this.markdownParser = markdownParser;
        this.articleTree = new DocsArticleNode(hubName, generateTitle(hubName, true), "", true, new ArrayList<>(), null);
        this.articleIndex = new HashMap<>();
        this.flatArticleList = new ArrayList<>();
        this.navigationMap = new HashMap<>();
        this.descendantIdMap = new HashMap<>();
    }

    /**
     * Scans the classpath for markdown files within this hub's directory and builds the content tree.
     */
    public void initialize() {
        log.info("Initializing Docs Hub [{}]. Scanning for articles in classpath:{}...", hubName, hubRootPath);
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:" + hubRootPath + "/**/*.md");

            if (resources.length == 0) {
                log.warn("No markdown files found for hub [{}]. It will be empty.", hubName);
                return;
            }

            for (Resource resource : resources) {
                String fullPath = URLDecoder.decode(resource.getURL().getPath(), StandardCharsets.UTF_8);
                String relativePath = extractRelativePath(fullPath);
                String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                addNode(relativePath, content);
            }

            sortTree(this.articleTree);
            this.flatArticleList = createFlatArticleList(this.articleTree);
            buildNavigationMap();
            buildDescendantIdMap(this.articleTree);

            log.info("Successfully loaded {} articles and built navigation/descendant maps for hub [{}].", articleIndex.size(), hubName);

        } catch (IOException e) {
            log.error("Failed to scan for articles for hub [{}].", hubName, e);
        }
    }

    public Optional<DocsArticleNode> findArticleById(String id) {
        return Optional.ofNullable(articleIndex.get(id));
    }

    /**
     * FIX: Added a public getter for the flat list of articles.
     * This is what the SearchController needs to call.
     * Note: If you are using Lombok's @Getter on the class, this is technically optional,
     * but it makes the contract of the class more explicit.
     *
     * @return A flat list of all article nodes in this hub.
     */
    public List<DocsArticleNode> getFlatArticleList() {
        return this.flatArticleList;
    }

    /**
     * It allows the Thymeleaf template to get the pre-calculated list of descendant article IDs for a given section.
     *
     * @param sectionId The ID of the section node.
     * @return A list of all article IDs nested under that section.
     */
    public List<String> getDescendantArticleIds(String sectionId) {
        return descendantIdMap.getOrDefault(sectionId, Collections.emptyList());
    }

    private void buildNavigationMap() {
        for (int i = 0; i < flatArticleList.size(); i++) {
            DocsArticleNode current = flatArticleList.get(i);
            DocsArticleNode prev = (i > 0) ? flatArticleList.get(i - 1) : null;
            DocsArticleNode next = (i < flatArticleList.size() - 1) ? flatArticleList.get(i + 1) : null;
            this.navigationMap.put(current.id(), new DocsArticleNode("nav", "nav", "nav", false, List.of(
                    Optional.ofNullable(prev).orElse(new DocsArticleNode("", "", "", false, List.of(), "")),
                    Optional.ofNullable(next).orElse(new DocsArticleNode("", "", "", false, List.of(), ""))
            ), ""));
        }
    }

    /**
     * Recursively builds a map of section IDs to a list of their descendant article IDs.
     */
    private void buildDescendantIdMap(DocsArticleNode node) {
        if (node.isSection()) {
            List<String> ids = new ArrayList<>();
            collectDescendantIds(node, ids);
            descendantIdMap.put(node.id(), ids);
            // Recurse for child sections
            for (DocsArticleNode child : node.children()) {
                buildDescendantIdMap(child);
            }
        }
    }

    /**
     * Helper method to recursively collect all article IDs under a given node.
     */
    private void collectDescendantIds(DocsArticleNode node, List<String> ids) {
        for (DocsArticleNode child : node.children()) {
            if (!child.isSection()) {
                ids.add(child.id());
            } else {
                collectDescendantIds(child, ids); // Recurse into sub-sections
            }
        }
    }

    public List<Map<String, String>> generateTableOfContents(String markdownContent) {
        if (markdownContent == null || markdownContent.isBlank()) {
            return Collections.emptyList();
        }
        List<Map<String, String>> toc = new ArrayList<>();
        Node document = markdownParser.parse(markdownContent);
        document.accept(new org.commonmark.node.AbstractVisitor() {
            @Override
            public void visit(Heading heading) {
                if (heading.getLevel() == 2 || heading.getLevel() == 3) { // H2 and H3
                    if (heading.getFirstChild() instanceof Text textNode) {
                        String title = textNode.getLiteral();
                        String anchor = title.toLowerCase().replaceAll("[^a-z0-9\\s-]", "").replaceAll("\\s+", "-");
                        toc.add(Map.of("level", String.valueOf(heading.getLevel()), "title", title, "anchor", anchor));
                    }
                }
                super.visit(heading);
            }
        });
        return toc;
    }

    private void addNode(String path, String content) {
        String[] parts = path.split("/");
        DocsArticleNode currentNode = this.articleTree;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            boolean isLastPart = (i == parts.length - 1);

            if (isLastPart) { // It's the file
                String id = generateId(path);
                String title = generateTitle(part, false);
                DocsArticleNode articleNode = new DocsArticleNode(id, title, path, false, new ArrayList<>(), content);
                currentNode.children().add(articleNode);
                articleIndex.put(id, articleNode);
            } else { // It's a directory
                String currentPath = String.join("/", Arrays.copyOfRange(parts, 0, i + 1));
                Optional<DocsArticleNode> existingNode = currentNode.children().stream()
                        .filter(child -> child.path().equals(currentPath))
                        .findFirst();

                if (existingNode.isPresent()) {
                    currentNode = existingNode.get();
                } else {
                    String id = generateId(currentPath);
                    String title = generateTitle(part, true);
                    DocsArticleNode sectionNode = new DocsArticleNode(id, title, currentPath, true, new ArrayList<>(), null);
                    currentNode.children().add(sectionNode);
                    currentNode = sectionNode;
                }
            }
        }
    }

    private void sortTree(DocsArticleNode node) {
        if (node.isSection() && !node.children().isEmpty()) {
            Collections.sort(node.children());
            node.children().forEach(this::sortTree);
        }
    }

    private List<DocsArticleNode> createFlatArticleList(DocsArticleNode node) {
        List<DocsArticleNode> list = new ArrayList<>();
        if (!node.isSection()) {
            list.add(node);
        }
        if (node.children() != null) {
            for (DocsArticleNode child : node.children()) {
                list.addAll(createFlatArticleList(child));
            }
        }
        return list;
    }

    private String extractRelativePath(String fullPath) {
        int hubRootIndex = fullPath.indexOf(hubRootPath);
        if (hubRootIndex != -1) {
            return fullPath.substring(hubRootIndex + hubRootPath.length() + 1);
        }
        return fullPath;
    }

    private String generateTitle(String part, boolean isSection) {
        String name = isSection ? part : part.replace(".md", "");
        return Arrays.stream(name.split("-"))
                .filter(s -> !s.matches("^[0-9]+$")) // Remove numeric prefixes
                .map(StringUtils::capitalize)
                .collect(Collectors.joining(" "));
    }

    private String generateId(String path) {
        return path.replace(".md", "").replaceAll("/", "-");
    }


}