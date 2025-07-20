package com.cloud.docs.controller;

/**
 * A simple data carrier for API response details.
 *
 * @param description The description of the response.
 * @param exampleJson A pretty-printed JSON string representing an example response body.
 */
public record ResponseInfo(String description, String exampleJson) {
}