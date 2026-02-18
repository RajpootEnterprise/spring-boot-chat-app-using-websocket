package com.chatapp.service;

import com.chatapp.exception.ChatExceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Service for handling image upload and storage operations.
 *
 * Responsibilities:
 * - File type validation (only images allowed)
 * - File size validation
 * - Secure filename generation (UUID-based, no path traversal)
 * - Saving files to the upload directory
 * - Generating accessible URLs for uploaded images
 *
 * Security considerations:
 * - Uses UUID filenames to prevent path traversal attacks
 * - Validates MIME type via content-type header AND file extension
 * - Files are served via /uploads/** static resource mapping
 *
 * @author ChatApp Team
 */
@Slf4j
@Service
public class ImageService {

    @Value("${app.upload.dir:uploads/images}")
    private String uploadDir;

    @Value("${app.max-image-size-mb:5}")
    private int maxImageSizeMb;

    /** Allowed image MIME types */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp"
    );

    /** Allowed file extensions */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp"
    );

    /**
     * Saves an uploaded image file to disk and returns its accessible URL.
     *
     * Steps:
     * 1. Validate file is not empty
     * 2. Validate MIME type
     * 3. Validate file size
     * 4. Generate unique filename
     * 5. Write to disk
     * 6. Return public-facing URL
     *
     * @param file The MultipartFile from HTTP request
     * @return Public URL path like /uploads/images/{uuid}.{ext}
     */
    public String saveImage(MultipartFile file) {
        log.debug("Processing image upload: name={}, size={} bytes, type={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());

        // 1. Check file is not empty
        if (file.isEmpty()) {
            throw new ImageUploadException("Uploaded file is empty");
        }

        // 2. Validate content type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new UnsupportedFileTypeException(contentType);
        }

        // 3. Validate file size (convert bytes to MB)
        long maxSizeBytes = (long) maxImageSizeMb * 1024 * 1024;
        if (file.getSize() > maxSizeBytes) {
            throw new FileSizeLimitExceededException(maxImageSizeMb);
        }

        // 4. Extract and validate extension from original filename
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new UnsupportedFileTypeException("." + extension);
        }

        // 5. Generate secure UUID-based filename
        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;

        // 6. Write to upload directory
        try {
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath); // Ensure directory exists

            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            log.info("Image saved: {} ({} bytes)", filePath, file.getSize());
        } catch (IOException e) {
            log.error("Failed to save image: {}", uniqueFilename, e);
            throw new ImageUploadException("Failed to save file to disk: " + e.getMessage(), e);
        }

        // 7. Return the public-facing URL
        String publicUrl = "/uploads/images/" + uniqueFilename;
        log.debug("Image accessible at: {}", publicUrl);
        return publicUrl;
    }

    /**
     * Deletes an uploaded image file by its URL path.
     * Used when a message containing an image is deleted.
     *
     * @param imageUrl The URL path returned by saveImage()
     * @return true if deleted, false if file not found
     */
    public boolean deleteImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return false;

        // Extract filename from URL (/uploads/images/uuid.ext → uuid.ext)
        String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        Path filePath = Paths.get(uploadDir, filename);

        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Image deleted: {}", filePath);
            } else {
                log.warn("Image file not found for deletion: {}", filePath);
            }
            return deleted;
        } catch (IOException e) {
            log.error("Failed to delete image: {}", filePath, e);
            return false;
        }
    }

    /**
     * Extracts the file extension from a filename.
     *
     * @param filename Original filename (e.g., "photo.jpg")
     * @return Extension without dot (e.g., "jpg"), defaults to "jpg" if none
     */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg"; // Default extension
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
