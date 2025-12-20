package org.agra.agra_backend.controller;

import org.agra.agra_backend.service.CloudinaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cloudinary")
public class CloudinaryController {
    private static final String KEY_STATUS = "status";
    private static final String KEY_SUCCESS = "success";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_CONNECTED = "connected";
    private static final String KEY_ERROR = "error";
    private static final String KEY_PUBLIC_ID = "public_id";

    private final CloudinaryService cloudinaryService;

    public CloudinaryController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> testConnection() {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean isConnected = cloudinaryService.testConnection();

            if (isConnected) {
                response.put(KEY_STATUS, KEY_SUCCESS);
                response.put(KEY_MESSAGE, "Cloudinary connection is working!");
                response.put(KEY_CONNECTED, true);
                return ResponseEntity.ok(response);
            } else {
                response.put(KEY_STATUS, KEY_ERROR);
                response.put(KEY_MESSAGE, "Cloudinary connection failed");
                response.put(KEY_CONNECTED, false);
                return ResponseEntity.status(500).body(response);
            }

        } catch (Exception e) {
            response.put(KEY_STATUS, KEY_ERROR);
            response.put(KEY_MESSAGE, "Error testing connection: " + e.getMessage());
            response.put(KEY_CONNECTED, false);
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();

        try {
            // Validate file
            if (file.isEmpty()) {
                response.put(KEY_STATUS, KEY_ERROR);
                response.put(KEY_MESSAGE, "Please select a file to upload");
                return ResponseEntity.badRequest().body(response);
            }

            // Check if it's an image
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put(KEY_STATUS, KEY_ERROR);
                response.put(KEY_MESSAGE, "Please upload a valid image file");
                return ResponseEntity.badRequest().body(response);
            }

            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinaryService.uploadImage(file);

            response.put(KEY_STATUS, KEY_SUCCESS);
            response.put(KEY_MESSAGE, "Image uploaded successfully");
            response.put("url", uploadResult.get("secure_url"));
            response.put(KEY_PUBLIC_ID, uploadResult.get(KEY_PUBLIC_ID));
            response.put("format", uploadResult.get("format"));
            response.put("width", uploadResult.get("width"));
            response.put("height", uploadResult.get("height"));
            response.put("bytes", uploadResult.get("bytes"));
            response.put("created_at", uploadResult.get("created_at"));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put(KEY_STATUS, KEY_ERROR);
            response.put(KEY_MESSAGE, "Error uploading image: " + e.getMessage());
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
                response.put(KEY_STATUS, KEY_ERROR);
                response.put(KEY_MESSAGE, "Please select a file to upload");
                return ResponseEntity.badRequest().body(response);
            }

            Map<String, Object> uploadResult = cloudinaryService.uploadImage(file, uploadPreset);

            response.put(KEY_STATUS, KEY_SUCCESS);
            response.put(KEY_MESSAGE, "Image uploaded successfully with preset: " + uploadPreset);
            response.put("url", uploadResult.get("secure_url"));
            response.put(KEY_PUBLIC_ID, uploadResult.get(KEY_PUBLIC_ID));
            response.put("preset_used", uploadPreset);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put(KEY_STATUS, KEY_ERROR);
            response.put(KEY_MESSAGE, "Error uploading image: " + e.getMessage());
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
