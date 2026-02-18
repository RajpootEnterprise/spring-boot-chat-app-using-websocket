package com.chatapp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Main entry point for the RealTime Chat Application.
 *
 * Extends SpringBootServletInitializer to support WAR deployment
 * to external Tomcat servers while also supporting embedded Tomcat.
 *
 * @author ChatApp Team
 * @version 1.0.0
 */
@Slf4j
@SpringBootApplication
@EnableJpaAuditing
public class ChatApplication extends SpringBootServletInitializer {

    public static void main(String[] args) {
        SpringApplication.run(ChatApplication.class, args);
        log.info("============================================================");
        log.info("  RealTime Chat Application started successfully!");
        log.info("  Access URL: http://localhost:8080");
        log.info("  Chat Room:  http://localhost:8080/chat");
        log.info("============================================================");
    }
}