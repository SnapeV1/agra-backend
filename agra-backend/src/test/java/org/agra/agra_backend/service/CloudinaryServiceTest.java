package org.agra.agra_backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    @InjectMocks
    private CloudinaryService service;

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;

    @Test
    void uploadImageUsesUploader() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn("data".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "http://example.com"));
        setField(service, "cloudinary", cloudinary);

        Map<String, Object> result = service.uploadImage(file);

        assertThat(result).containsEntry("secure_url", "http://example.com");
    }

    @Test
    void uploadImageToFolderAddsFolderParam() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn("data".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "http://example.com"));
        setField(service, "cloudinary", cloudinary);

        service.uploadImageToFolder(file, "/courses/123/");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), captor.capture());
        assertThat(captor.getValue()).containsEntry("folder", "courses/123");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
