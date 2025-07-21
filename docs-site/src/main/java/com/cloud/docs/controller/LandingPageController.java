package com.cloud.docs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller dedicated to serving the main landing page of the documentation site.
 * It gathers all available documentation hubs and API specifications to be displayed.
 */
@Controller
@RequiredArgsConstructor
public class LandingPageController {

    private final ApiRegistryService apiRegistry;
    private final DocsHubRegistry docsHubRegistry;
    private final SpaceService spaceService; // Needed for the shared header

    @GetMapping("/")
    public String landingPage(Model model) {
        // Add all discovered documentation hubs to the model for the "Guides" cards
        model.addAttribute("allHubs", docsHubRegistry.getAllHubs());

        // Add all discovered API specifications for the "API Reference" cards
        model.addAttribute("allApis", apiRegistry.getSpecifications());

        // Add other services needed to render the shared header
        model.addAttribute("docsHubRegistry", docsHubRegistry);
        model.addAttribute("spaceService", spaceService);

        // Set 'activeTab' to a unique value so no tab is highlighted on the landing page
        model.addAttribute("activeTab", "home");

        return "landing"; // This will resolve to landing.html
    }
}