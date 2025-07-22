package com.cloud.docs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequiredArgsConstructor
public class SearchController {

    private final SearchIndexService searchIndexService;

    /**
     * This endpoint now returns the pre-cached JSON string directly.
     * This is extremely fast as it avoids serialization on every request.
     */
    @GetMapping(value = "/search-index", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody // Ensures the String is written directly to the response body
    public String getSearchIndex() {
        // FIX: Removed the brittle string replacement.
        // The cached string is now guaranteed to be valid JSON.
        return searchIndexService.getSearchIndexJson();
    }
}