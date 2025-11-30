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

    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadImage(MultipartFile file, String uploadPreset) throws IOException {
        try {
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
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

    @SuppressWarnings("unchecked")
    public String deleteImage(String publicId) throws IOException {
        try {
            Map<String, Object> result = (Map<String, Object>) cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            System.out.println("Image deleted: " + result.get("result"));
            return result.get("result").toString();
        } catch (IOException e) {
            System.err.println("Error deleting image from Cloudinary: " + e.getMessage());
            throw e;
        }
    }

    // Test method to verify connection
    @SuppressWarnings("unchecked")
    public boolean testConnection() {
        try {
            Map<String, Object> result = (Map<String, Object>) cloudinary.api().ping(ObjectUtils.emptyMap());
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
    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadImageToFolder(MultipartFile file, String folderPath) throws IOException {
        try {
            // Normalize folder path (remove leading/trailing slashes, handle null/empty)
            String normalizedFolderPath = normalizeFolderPath(folderPath);

            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "upload_preset", "hkpcvcr8",
                    "resource_type", "image"
            );

            // Only add folder parameter if we have a valid folder path
            if (normalizedFolderPath != null && !normalizedFolderPath.isEmpty()) {
                uploadParams.put("folder", normalizedFolderPath);
            }

            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
                    file.getBytes(),
                    uploadParams
            );

            System.out.println("Image uploaded successfully to " +
                    (normalizedFolderPath != null ? normalizedFolderPath : "root") +
                    ": " + uploadResult.get("secure_url"));

            return uploadResult;

        } catch (IOException e) {
            System.err.println("Error uploading image to folder " + folderPath + ": " + e.getMessage());
            throw e;
        }
    }

    private String normalizeFolderPath(String folderPath) {
        if (folderPath == null || folderPath.trim().isEmpty()) {
            return null;
        }

        // Remove leading and trailing slashes, replace multiple slashes with single ones
        String normalized = folderPath.trim()
                .replaceAll("^/+", "")  // Remove leading slashes
                .replaceAll("/+$", "")  // Remove trailing slashes
                .replaceAll("/+", "/"); // Replace multiple slashes with single slash

        return normalized.isEmpty() ? null : normalized;
    }
    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadProfilePicture(MultipartFile file, String userEmail) throws IOException {
        try {
            String sanitizedEmail = userEmail.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
            String folderPath = "users/" + sanitizedEmail;

            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
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

    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadTicketAttachment(MultipartFile file, String userId, String ticketId) throws IOException {
        try {
            String safeUser = sanitizeIdentifier(userId);
            String safeTicket = sanitizeIdentifier(ticketId);
            String folderPath = "tickets/" + safeUser + "/" + safeTicket;
            String publicId = buildTicketAttachmentPublicId(file.getOriginalFilename());

            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "upload_preset", "hkpcvcr8",
                            "folder", folderPath,
                            "public_id", publicId,
                            "resource_type", "image",
                            "overwrite", false,
                            "unique_filename", false,
                            "use_filename", false
                    )
            );

            System.out.println("Ticket attachment uploaded successfully: folder=" + folderPath
                    + " url=" + uploadResult.get("secure_url"));
            return uploadResult;

        } catch (IOException e) {
            System.err.println("Error uploading ticket attachment for user=" + userId + ", ticket=" + ticketId + ": " + e.getMessage());
            throw e;
        }
    }

    // Upload raw file (documents, zips, etc.) to specific folder
    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadRawToFolder(MultipartFile file, String folderPath) throws IOException {
        try {
            String normalizedFolderPath = normalizeFolderPath(folderPath);

            String originalName = file.getOriginalFilename();
            String sanitized = sanitizeFilenameKeepingExtension(originalName, file.getContentType());

            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "upload_preset", "hkpcvcr8",
                    "resource_type", "raw",
                    // Preserve original filename (including extension) in the public_id
                    "use_filename", true,
                    "unique_filename", false,
                    // public_id applies within the provided folder
                    "public_id", sanitized
            );

            if (normalizedFolderPath != null && !normalizedFolderPath.isEmpty()) {
                uploadParams.put("folder", normalizedFolderPath);
            }

            System.out.println("[CloudinaryService] RAW upload - folder=" + normalizedFolderPath
                    + ", name=" + file.getOriginalFilename()
                    + ", contentType=" + file.getContentType()
                    + ", size=" + file.getSize() + " bytes");
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
                    file.getBytes(),
                    uploadParams
            );

            System.out.println("Raw file uploaded successfully to " +
                    (normalizedFolderPath != null ? normalizedFolderPath : "root") +
                    ": " + uploadResult.get("secure_url"));
            System.out.println("[CloudinaryService] RAW result - resource_type=" + uploadResult.get("resource_type")
                    + ", format=" + uploadResult.get("format")
                    + ", public_id=" + uploadResult.get("public_id")
                    + ", bytes=" + uploadResult.get("bytes") + ")");

            return uploadResult;

        } catch (IOException e) {
            System.err.println("Error uploading raw file to folder " + folderPath + ": " + e.getMessage());
            throw e;
        }
    }

    private String sanitizeFilenameKeepingExtension(String originalName, String contentType) {
        String base = originalName;
        if (base == null || base.isBlank()) {
            base = "file";
        }
        // Strip any path parts
        int slash = Math.max(base.lastIndexOf('/'), base.lastIndexOf('\\'));
        if (slash >= 0 && slash < base.length() - 1) {
            base = base.substring(slash + 1);
        }
        // Sanitize characters
        base = base.replaceAll("[^a-zA-Z0-9._-]", "_");
        // Ensure PDF extension when contentType indicates PDF and no extension present
        if (!base.contains(".") && contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
            base = base + ".pdf";
        }
        // Guard against empty name after sanitization
        if (base.isBlank()) {
            base = "file" + System.currentTimeMillis();
            if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
                base += ".pdf";
            }
        }
        // Cloudinary public_id should not start with a dot
        if (base.startsWith(".")) {
            base = "file" + base;
        }
        return base;
    }

    private String sanitizeIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value.replaceAll("[^a-zA-Z0-9_-]", "_").toLowerCase();
    }

    private String buildTicketAttachmentPublicId(String originalName) {
        String sanitized = sanitizeFilenameKeepingExtension(originalName, null);
        if (sanitized == null || sanitized.isBlank()) {
            sanitized = "attachment_" + System.currentTimeMillis();
        }
        return sanitized;
    }

    // Upload any file type (auto detect) to specific folder
    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadAutoToFolder(MultipartFile file, String folderPath) throws IOException {
        try {
            String normalizedFolderPath = normalizeFolderPath(folderPath);

            Map<String, Object> uploadParams = ObjectUtils.asMap(
                    "upload_preset", "hkpcvcr8",
                    "resource_type", "auto"
            );

            if (normalizedFolderPath != null && !normalizedFolderPath.isEmpty()) {
                uploadParams.put("folder", normalizedFolderPath);
            }

            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
                    file.getBytes(),
                    uploadParams
            );

            System.out.println("Auto resource uploaded successfully to " +
                    (normalizedFolderPath != null ? normalizedFolderPath : "root") +
                    ": " + uploadResult.get("secure_url"));

            return uploadResult;

        } catch (IOException e) {
            System.err.println("Error uploading (auto) file to folder " + folderPath + ": " + e.getMessage());
            throw e;
        }
    }

    // Delete a raw file by publicId
    @SuppressWarnings("unchecked")
    public String deleteRaw(String publicId) throws IOException {
        try {
            Map<String, Object> result = (Map<String, Object>) cloudinary.uploader().destroy(
                    publicId,
                    ObjectUtils.asMap("resource_type", "raw")
            );
            System.out.println("Raw file deleted: " + result.get("result"));
            return result.get("result").toString();
        } catch (IOException e) {
            System.err.println("Error deleting raw file from Cloudinary: " + e.getMessage());
            throw e;
        }
    }

    // Upload a profile picture from a remote URL into the user's folder
    @SuppressWarnings("unchecked")
    public Map<String, Object> uploadProfilePictureFromUrl(String imageUrl, String userEmail) throws IOException {
        try {
            String sanitizedEmail = userEmail.replaceAll("[^a-zA-Z0-9]", "_").toLowerCase();
            String folderPath = "users/" + sanitizedEmail;

            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
                    imageUrl,
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

            System.out.println("Profile picture (URL) uploaded successfully for user: " + userEmail +
                    " -> " + uploadResult.get("secure_url"));
            return uploadResult;

        } catch (IOException e) {
            System.err.println("Error uploading profile picture from URL for user " + userEmail + ": " + e.getMessage());
            throw e;
        }
    }
}
