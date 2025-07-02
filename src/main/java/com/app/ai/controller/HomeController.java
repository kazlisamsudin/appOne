package com.app.ai.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "", "/home"})
    public String home() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return "home";
    }
}
