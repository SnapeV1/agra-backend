package org.agra.agra_backend.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

public interface ICloudinaryService {

    /**
     * Upload an image using the default upload preset.
     *
     * @param file MultipartFile to upload
     * @return Map containing upload details
     * @throws IOException if upload fails
     */
    Map<String, Object> uploadImage(MultipartFile file) throws IOException;

    /**
     * Upload an image using a specific upload preset.
     *
     * @param file         MultipartFile to upload
     * @param uploadPreset Preset name configured in Cloudinary
     * @return Map containing upload details
     * @throws IOException if upload fails
     */
    Map<String, Object> uploadImage(MultipartFile file, String uploadPreset) throws IOException;

    /**
     * Delete an image by its public ID.
     *
     * @param publicId The public ID of the image in Cloudinary
     * @return Result string ("ok" if deleted)
     * @throws IOException if deletion fails
     */
    String deleteImage(String publicId) throws IOException;

    /**
     * Test Cloudinary connection.
     *
     * @return true if connection is successful, false otherwise
     */
    boolean testConnection();
}
