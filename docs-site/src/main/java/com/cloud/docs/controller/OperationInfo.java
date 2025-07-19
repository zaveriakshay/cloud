package com.cloud.docs.controller;

/**
 * A simple data carrier for API operation details used by the UI.
 *
 * @param id          The operationId from the OpenAPI spec (e.g., "create-payment").
 * @param summary     The short summary of the operation.
 * @param description The detailed description.
 * @param tag         The primary tag for grouping (e.g., "Payments").
 */
public record OperationInfo(String id, String summary, String description, String tag) {
}