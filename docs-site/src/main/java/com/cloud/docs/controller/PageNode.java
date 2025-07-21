package com.cloud.docs.controller;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a node in the documentation tree. It can be a page or a section (folder).
 * @param id A unique identifier for the node.
 * @param title The display title of the page or section.
 * @param content The markdown content (only for pages).
 * @param isSection True if this node is a container for other pages, false if it's a page with content.
 * @param children A list of child nodes.
 * @param author The author of the last update.
 * @param lastUpdatedAt Timestamp of the last update.
 */
public record PageNode(
    String id,
    String title,
    String content,
    boolean isSection,
    List<PageNode> children,
    String author,
    Instant lastUpdatedAt
) {
    // Constructor for creating a new, empty section
    public static PageNode createSection(String id, String title) {
        return new PageNode(id, title, null, true, new CopyOnWriteArrayList<>(), "system", Instant.now());
    }

    // Constructor for creating a new, empty page
    public static PageNode createPage(String id, String title) {
        return new PageNode(id, title, "# " + title + "\n\nStart writing here...", false, new CopyOnWriteArrayList<>(), "system", Instant.now());
    }
}