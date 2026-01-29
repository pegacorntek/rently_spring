package com.pegacorn.rently.controller;

import com.pegacorn.rently.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller for serving uploaded files with authentication.
 * All requests to this controller require authentication via JWT.
 */
@Slf4j
@RestController
@RequestMapping("/files")
public class FileController {

    @Value("${upload.path}")
    private String uploadPath;

    /**
     * Serve a file from the uploads directory.
     * Requires authentication (handled by Spring Security).
     *
     * @param folder   The subfolder (e.g., "tickets", "payments")
     * @param filename The filename
     * @return The file as a resource
     */
    @GetMapping("/{folder}/{filename:.+}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String folder,
            @PathVariable String filename) {

        try {
            // Sanitize folder and filename to prevent path traversal
            if (folder.contains("..") || filename.contains("..")) {
                throw ApiException.badRequest("Invalid file path");
            }

            Path filePath = Paths.get(uploadPath).resolve(folder).resolve(filename).normalize();

            // Ensure the resolved path is still within upload directory
            if (!filePath.startsWith(Paths.get(uploadPath).normalize())) {
                throw ApiException.badRequest("Invalid file path");
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                throw ApiException.notFound("File not found");
            }

            // Determine content type
            String contentType;
            try {
                contentType = Files.probeContentType(filePath);
            } catch (IOException e) {
                contentType = "application/octet-stream";
            }

            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (MalformedURLException e) {
            log.error("Error serving file: {}/{}", folder, filename, e);
            throw ApiException.badRequest("Error serving file");
        }
    }
}
