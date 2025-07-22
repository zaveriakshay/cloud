package com.cloud.docs.controller;

/**
 * A record representing a single item in the search index.
 *
 * @param title      The main title of the search result (e.g., an endpoint summary or article title).
 * @param breadcrumb A navigation path to show context (e.g., "API Name → Tag" or "Hub Name").
 * @param url        The direct URL to navigate to the item.
 * @param content    The full, plain-text content of the item to be searched against.
 */
public record SearchHit(String title, String breadcrumb, String url, String content) {
}