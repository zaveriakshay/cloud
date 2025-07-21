package com.cloud.docs.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to handle user authentication-related page requests.
 */
@Controller
public class AuthController {

    /**
     * Directs users to the sign-in page.
     * This can use an existing login.html template.
     * @return The view name for the login page.
     */
    @GetMapping("/signin")
    public String signIn() {
        return "login"; // Assumes you have a login.html template
    }

    /**
     * A placeholder for the account creation flow.
     * For now, it redirects to the main landing page.
     * @return A redirect string.
     */
    @GetMapping("/signup")
    public String signUp() {
        // This can be changed to a dedicated sign-up page later
        return "redirect:/";
    }
}