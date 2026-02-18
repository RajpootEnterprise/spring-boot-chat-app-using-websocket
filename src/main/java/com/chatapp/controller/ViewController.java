package com.chatapp.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * MVC Controller for serving JSP views.
 *
 * Handles page navigation between:
 * - Login page (/)
 * - Chat room (/chat)
 *
 * The actual chat logic runs via WebSocket in ChatWebSocketController.
 * This controller only handles HTTP page requests.
 *
 * @author ChatApp Team
 */
@Slf4j
@Controller
public class ViewController {

    /**
     * Serves the login/landing page.
     * Users enter their username here before joining the chat.
     */
    @GetMapping({"/", "/login"})
    public String loginPage(Model model) {
        model.addAttribute("pageTitle", "Join Chat - RealTime Chat App");
        return "login"; // Resolves to /WEB-INF/views/login.jsp
    }

    /**
     * Serves the main chat room page.
     * Redirects to login if username parameter is missing.
     *
     * @param username The username passed as query param from login form
     * @param model    Spring MVC model for passing data to JSP
     */
    @GetMapping("/chat")
    public String chatPage(@RequestParam(value = "username", required = false) String username,
                           Model model) {
        if (username == null || username.isBlank()) {
            log.warn("Chat access attempted without username, redirecting to login");
            return "redirect:/";
        }

        String cleanUsername = username.trim().toLowerCase();
        log.info("User entering chat room: {}", cleanUsername);

        // Pass data to JSP
        model.addAttribute("username", cleanUsername);
        model.addAttribute("pageTitle", "Chat Room - " + cleanUsername);
        model.addAttribute("wsEndpoint", "/ws-chat");

        return "chat"; // Resolves to /WEB-INF/views/chat.jsp
    }
}
