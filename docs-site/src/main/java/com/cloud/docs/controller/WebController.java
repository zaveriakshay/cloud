package com.cloud.docs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
class WebController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/keycloak-mock")
    public String keycloakMock() {
        return "keycloak-mock";
    }

    /**
     * FIX: This endpoint now accepts the 'operation' parameter from the "Try it out" link
     * and passes it to the view model.
     */
    @GetMapping("/test-scalar")
    public String scalarTest(@RequestParam(name = "operation", required = false) String operationId, Model model) {
        model.addAttribute("operationId", operationId);
        return "scalar-test";
    }
}