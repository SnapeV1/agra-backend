package org.agra.agra_backend.controller;

import org.agra.agra_backend.service.CloudinaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cloudinary")
public class CloudinaryController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean isConnected = cloudinaryService.testConnection();

            if (isConnected) {
                response.put("status", "success");
                response.put("message", "Cloudinary connection is working!");
                response.put("connected", true);
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Cloudinary connection failed");
                response.put("connected", false);
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error testing connection: " + e.getMessage());
            response.put("connected", false);
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Please select a file to upload");
                return ResponseEntity.badRequest().body(response);
            }

            // Check if it's an image
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("status", "error");
                response.put("message", "Please upload a valid image file");
                return ResponseEntity.badRequest().body(response);
            }

            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinaryService.uploadImage(file);

            response.put("status", "success");
            response.put("message", "Image uploaded successfully");
            response.put("url", uploadResult.get("secure_url"));
            response.put("public_id", uploadResult.get("public_id"));
            response.put("format", uploadResult.get("format"));
            response.put("width", uploadResult.get("width"));
            response.put("height", uploadResult.get("height"));
            response.put("bytes", uploadResult.get("bytes"));
            response.put("created_at", uploadResult.get("created_at"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error uploading image: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/upload-with-preset")
    public ResponseEntity<Map<String, Object>> uploadImageWithPreset(
            @RequestParam("file") MultipartFile file,
            @RequestParam("preset") String uploadPreset) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate file
            if (file.isEmpty()) {
                response.put("status", "error");
                response.put("message", "Please select a file to upload");
                return ResponseEntity.badRequest().body(response);
            }

            // Upload to Cloudinary with custom preset
            Map<String, Object> uploadResult = cloudinaryService.uploadImage(file, uploadPreset);

            response.put("status", "success");
            response.put("message", "Image uploaded successfully with preset: " + uploadPreset);
            response.put("url", uploadResult.get("secure_url"));
            response.put("public_id", uploadResult.get("public_id"));
            response.put("preset_used", uploadPreset);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Error uploading image: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("service", "CloudinaryController");
        response.put("status", "UP");
        return ResponseEntity.ok(response);
    }
}