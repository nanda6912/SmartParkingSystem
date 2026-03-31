package com.smartparking.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Home controller to handle default page routing
 */
@Controller
public class HomeController {
    
    /**
     * Redirect root URL to exit page
     */
    @GetMapping("/")
    public String home() {
        return "redirect:/index.html";
    }
}
