package com.cloud.docs.controller;

import java.util.List;

/**
 * Represents a node in the documentation hub's article tree. It can be either a section (directory)
 * or an article (markdown file).
 *
 * @param id        A unique identifier for the node (e.g., "getting-started-overview").
 * @param title     The display-friendly title (e.g., "Overview").
 * @param path      The relative file path used for sorting.
 * @param isSection A boolean indicating if this node is a directory.
 * @param children  A list of child nodes.
 * @param content   The raw markdown content of the article (null for sections).
 */
public record DocsArticleNode(
        String id,
        String title,
        String path,
        boolean isSection,
        List<DocsArticleNode> children,
        String content
) implements Comparable<DocsArticleNode> {

    /**
     * Compares nodes based on their file path to ensure a consistent,
     * file-system-like order in the navigation tree.
     */
    @Override
    public int compareTo(DocsArticleNode other) {
        return this.path.compareTo(other.path);
    }
}