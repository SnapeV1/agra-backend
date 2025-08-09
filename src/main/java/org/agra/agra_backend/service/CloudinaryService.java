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
        cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    public String uploadFile(MultipartFile file) throws IOException {
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
        return uploadResult.get("url").toString();
    }

    public String uploadFile(MultipartFile file, String folder) throws IOException {
        Map<String, Object> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("folder", folder)
        );
        return uploadResult.get("url").toString();
    }

    public String uploadFileWithTransformation(MultipartFile file, Map<String, Object> transformations) throws IOException {
        Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), transformations);
        return uploadResult.get("url").toString();
    }

    public void deleteFile(String publicId) throws IOException {
        cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
    }

    public String getOptimizedUrl(String publicId, int width, int height) {
        return cloudinary.url()
                .transformation(new com.cloudinary.Transformation()
                        .width(width)
                        .height(height)
                        .crop("fill")
                        .quality("auto")
                        .fetchFormat("auto"))
                .generate(publicId);
    }

    public String getOptimizedUrl(String publicId) {
        return cloudinary.url()
                .transformation(new com.cloudinary.Transformation()
                        .quality("auto")
                        .fetchFormat("auto"))
                .generate(publicId);
    }
}