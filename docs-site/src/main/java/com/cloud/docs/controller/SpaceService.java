package com.cloud.docs.controller;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SpaceService {

    // In-memory store for our documentation tree.
    private final PageNode rootNode;
    private final Map<String, PageNode> nodeIndex;

    @Getter
    private boolean isPublished = false;
    private PageNode publishedRootNode = null;

    public SpaceService() {
        this.nodeIndex = new ConcurrentHashMap<>();
        this.rootNode = PageNode.createSection("root", "My Workspace");

        // Create some initial data
        PageNode gettingStarted = PageNode.createPage("getting-started", "Getting Started");
        PageNode apiKeys = PageNode.createPage("api-keys", "API Keys");
        PageNode tutorials = PageNode.createSection("tutorials", "Tutorials");
        PageNode firstTutorial = PageNode.createPage("first-tutorial", "Your First Tutorial");

        tutorials.children().add(firstTutorial);
        this.rootNode.children().add(gettingStarted);
        this.rootNode.children().add(apiKeys);
        this.rootNode.children().add(tutorials);

        indexNode(this.rootNode);
    }

    private void indexNode(PageNode node) {
        nodeIndex.put(node.id(), node);
        node.children().forEach(this::indexNode);
    }

    public PageNode getPageTree() {
        return this.rootNode;
    }

    public Optional<PageNode> getPublishedPageTree() {
        return Optional.ofNullable(publishedRootNode);
    }

    public Optional<PageNode> findNodeById(String id) {
        return Optional.ofNullable(nodeIndex.get(id));
    }

    public Optional<PageNode> findParentOf(String childId) {
        return nodeIndex.values().stream()
                .filter(node -> node.children().stream().anyMatch(child -> child.id().equals(childId)))
                .findFirst();
    }

    /**
     * Creates a flattened list of all pages (not sections) in the tree.
     * This is used to build the "Next/Previous" navigation.
     * @param startNode The node to start traversal from (usually the root).
     * @return An ordered list of PageNode objects.
     */
    public List<PageNode> getFlatPageList(PageNode startNode) {
        List<PageNode> flatList = new ArrayList<>();
        collectPages(startNode, flatList);
        return flatList;
    }

    private void collectPages(PageNode node, List<PageNode> list) {
        if (!node.isSection()) {
            list.add(node);
        }
        if (node.children() != null) {
            for (PageNode child : node.children()) {
                collectPages(child, list);
            }
        }
    }

    public PageNode renameWorkspace(String newTitle) {
        PageNode updatedRoot = new PageNode(
                this.rootNode.id(),
                newTitle,
                this.rootNode.content(),
                this.rootNode.isSection(),
                this.rootNode.children(),
                "user",
                Instant.now()
        );
        this.nodeIndex.put("root", updatedRoot);
        this.rootNode.children().clear();
        this.rootNode.children().addAll(updatedRoot.children());
        try {
            var titleField = PageNode.class.getDeclaredField("title");
            titleField.setAccessible(true);
            titleField.set(this.rootNode, newTitle);
        } catch (Exception e) {
            log.error("Failed to update root node title via reflection", e);
        }

        log.info("Workspace renamed to '{}'", newTitle);
        return updatedRoot;
    }

    public void publishWorkspace() {
        this.publishedRootNode = deepCopy(this.rootNode);
        this.isPublished = true;
        log.info("Workspace '{}' has been published.", this.publishedRootNode.title());
    }

    private PageNode deepCopy(PageNode original) {
        if (original == null) return null;
        List<PageNode> copiedChildren = original.children().stream()
                .map(this::deepCopy)
                .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
        return new PageNode(
                original.id(),
                original.title(),
                original.content(),
                original.isSection(),
                copiedChildren,
                original.author(),
                original.lastUpdatedAt()
        );
    }

    public PageNode createNode(String parentId, String title, boolean isSection) {
        PageNode parent = findNodeById(parentId).orElseThrow(() -> new IllegalArgumentException("Parent not found"));
        if (!parent.isSection()) {
            throw new IllegalArgumentException("Cannot add a child to a page, only to a section.");
        }
        String newId = title.toLowerCase().replaceAll("\\s+", "-") + "-" + UUID.randomUUID().toString().substring(0, 4);
        PageNode newNode = isSection ? PageNode.createSection(newId, title) : PageNode.createPage(newId, title);

        parent.children().add(newNode);
        nodeIndex.put(newNode.id(), newNode);
        log.info("Created new node '{}' with id '{}' under parent '{}'", title, newId, parentId);
        return newNode;
    }

    public PageNode updatePage(String pageId, String newTitle, String newContent) {
        PageNode oldNode = findNodeById(pageId).orElseThrow(() -> new IllegalArgumentException("Page not found"));
        PageNode updatedNode = new PageNode(
                oldNode.id(),
                newTitle,
                newContent,
                oldNode.isSection(),
                oldNode.children(),
                "user",
                Instant.now()
        );
        nodeIndex.put(pageId, updatedNode);

        findParentOf(pageId).ifPresent(parent -> {
            parent.children().replaceAll(node -> node.id().equals(pageId) ? updatedNode : node);
        });

        log.info("Updated page '{}'", pageId);
        return updatedNode;
    }

    public void reorderChildren(String parentId, List<String> orderedChildIds) {
        PageNode parent = findNodeById(parentId).orElseThrow(() -> new IllegalArgumentException("Parent not found"));
        Map<String, PageNode> childrenMap = parent.children().stream()
                .collect(Collectors.toMap(PageNode::id, Function.identity()));

        List<PageNode> reorderedChildren = orderedChildIds.stream()
                .map(childrenMap::get)
                .collect(Collectors.toCollection(CopyOnWriteArrayList::new));

        parent.children().clear();
        parent.children().addAll(reorderedChildren);
        log.info("Reordered children for parent '{}'", parentId);
    }
}