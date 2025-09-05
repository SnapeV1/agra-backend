package org.agra.agra_backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    @Value("${cloudinary.cloud-name}")
    private String cloudName;

    @Value("${cloudinary.api-key}")
    private String apiKey;

    @Value("${cloudinary.api-secret}")
    private String apiSecret;

    private Cloudinary cloudinary;

    @PostConstruct
    public void init() {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));

        System.out.println("Cloudinary initialized successfully!");
        System.out.println("Cloud name: " + cloudName);
    }

    public Map<String, Object> uploadImage(MultipartFile file) throws IOException {
        return uploadImage(file, "hkpcvcr8"); // Use your upload preset by default
    }

    public Map<String, Object> uploadImage(MultipartFile file, String uploadPreset) throws IOException {
        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "upload_preset", uploadPreset,
                            "resource_type", "image"
                    )
            );

            System.out.println("Image uploaded successfully: " + uploadResult.get("secure_url"));
            return uploadResult;

        } catch (IOException e) {
            System.err.println("Error uploading image to Cloudinary: " + e.getMessage());
            throw e;
        }
    }

    public String deleteImage(String publicId) throws IOException {
        try {
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            System.out.println("Image deleted: " + result.get("result"));
            return result.get("result").toString();
        } catch (IOException e) {
            System.err.println("Error deleting image from Cloudinary: " + e.getMessage());
            throw e;
        }
    }

    // Test method to verify connection
    public boolean testConnection() {
        try {
            Map<String, Object> result = cloudinary.api().ping(ObjectUtils.emptyMap());
            System.out.println("Cloudinary connection test successful: " + result);
            return true;
        } catch (Exception e) {
            System.err.println("Cloudinary connection test failed: " + e.getMessage());
            return false;
        }
    }

    // Create user folder in Cloudinary
    public void createUserFolder(String folderName) throws Exception {
        try {
            // Create the main user folder
            cloudinary.api().createFolder(folderName, ObjectUtils.emptyMap());
            System.out.println("Created folder: " + folderName);

            // Create subfolders for organization
            cloudinary.api().createFolder(folderName + "/profile", ObjectUtils.emptyMap());
           // cloudinary.api().createFolder(folderName + "/posts", ObjectUtils.emptyMap());
           // System.out.println("Created subfolders: profile and posts");

        } catch (Exception e) {
            System.err.println("Error creating Cloudinary folder: " + e.getMessage());
            throw e;
        }
    }


    // Upload image to specific folder with preset
    public Map<String, Object> uploadImageToFolder(MultipartFile file, String folderPath) throws IOException {
        try {
            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "upload_preset", "hkpcvcr8",
                            "folder", folderPath,
                            "resource_type", "image"
                    )
            );

            System.out.println("Image uploaded successfully to " + folderPath + ": " + uploadResult.get("secure_url"));
            return uploadResult;

        } catch (IOException e) {
            System.err.println("Error uploading image to folder " + folderPath + ": " + e.getMessage());
            throw e;
        }
    }
    public Map<String, Object> uploadProfilePicture(MultipartFile file, String userEmail) throws IOException {
        try {
            String sanitizedEmail = userEmail.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
            String folderPath = "users/" + sanitizedEmail;

            Map<String, Object> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "upload_preset", "hkpcvcr8",
                            "folder", folderPath,
                            "public_id", "profilepic",
                            "resource_type", "image",
                            "overwrite", true,
                            "unique_filename", false,
                            "use_filename", false
                    )
            );

            System.out.println("Profile picture uploaded successfully for user: " + userEmail +
                    " -> " + uploadResult.get("secure_url"));
            return uploadResult;

        } catch (IOException e) {
            System.err.println("Error uploading profile picture for user " + userEmail + ": " + e.getMessage());
            throw e;
        }
    }
}