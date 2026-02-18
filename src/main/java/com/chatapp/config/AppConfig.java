package com.chatapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * General application configuration.
 *
 * Configures:
 * - File upload directory creation
 * - Static resource handlers for uploaded images
 * - CORS settings for cross-origin requests
 * - Multipart file resolver
 *
 * @author ChatApp Team
 */
@Slf4j
@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/images}")
    private String uploadDir;

    /**
     * Configure static resource handlers.
     * Maps /uploads/** URL path to the actual file system upload directory.
     * This allows uploaded images to be served directly via HTTP.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ensure upload directory exists
        createUploadDirectoryIfNotExists();

        // Map /uploads/** to the file system
        String absoluteUploadPath = new File(uploadDir).getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absoluteUploadPath + "/");

        log.info("Resource handler configured: /uploads/** -> {}", absoluteUploadPath);
    }

    /**
     * Configure CORS for REST endpoints.
     * Allows cross-origin requests from any origin in development.
     * Restrict to specific domains in production.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * Multipart resolver bean for file uploads.
     */
    @Bean
    public MultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }

    /**
     * Creates the upload directory on application startup if it doesn't exist.
     * Logs appropriate messages for operational visibility.
     */
    private void createUploadDirectoryIfNotExists() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            } else {
                log.info("Upload directory already exists: {}", uploadPath.toAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Failed to create upload directory: {}", uploadDir, e);
            throw new RuntimeException("Cannot create upload directory: " + uploadDir, e);
        }
    }
}
