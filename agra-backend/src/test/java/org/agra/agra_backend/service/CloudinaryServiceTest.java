package org.agra.agra_backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Api;
import com.cloudinary.Uploader;
import com.cloudinary.api.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    @InjectMocks
    private CloudinaryService service;

    @Mock
    private Cloudinary cloudinary;
    @Mock
    private Uploader uploader;
    @Mock
    private Api api;

    @Test
    void initCreatesCloudinaryInstance() throws Exception {
        setField(service, "cloudName", "cloud");
        setField(service, "apiKey", "key");
        setField(service, "apiSecret", "secret");

        service.init();

        Object created = getField(service, "cloudinary");
        assertThat(created).isNotNull();
    }

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
    void uploadImageWithPresetUsesProvidedPreset() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn("data".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "http://example.com"));
        setField(service, "cloudinary", cloudinary);

        service.uploadImage(file, "preset-1");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), captor.capture());
        assertThat(captor.getValue()).containsEntry("upload_preset", "preset-1");
    }

    @Test
    void uploadImageRethrowsIOException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn("data".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenThrow(new IOException("boom"));
        setField(service, "cloudinary", cloudinary);

        assertThatThrownBy(() -> service.uploadImage(file))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("boom");
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

    @Test
    void uploadImageToFolderSkipsEmptyFolder() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn("data".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "http://example.com"));
        setField(service, "cloudinary", cloudinary);

        service.uploadImageToFolder(file, "   ");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), captor.capture());
        assertThat(captor.getValue()).doesNotContainKey("folder");
    }

    @Test
    void uploadImageToFolderRethrowsIOException() throws Exception {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getBytes()).thenReturn("data".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenThrow(new IOException("boom"));
        setField(service, "cloudinary", cloudinary);

        assertThatThrownBy(() -> service.uploadImageToFolder(file, "/courses/123/"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void deleteImageReturnsResult() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("public-id"), anyMap()))
                .thenReturn(Map.of("result", "ok"));
        setField(service, "cloudinary", cloudinary);

        String result = service.deleteImage("public-id");

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void deleteImageRethrowsIOException() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("public-id"), anyMap())).thenThrow(new IOException("boom"));
        setField(service, "cloudinary", cloudinary);

        assertThatThrownBy(() -> service.deleteImage("public-id"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void testConnectionReturnsTrueOnSuccess() throws Exception {
        when(cloudinary.api()).thenReturn(api);
        when(api.ping(anyMap())).thenReturn(mock(ApiResponse.class));
        setField(service, "cloudinary", cloudinary);

        assertThat(service.testConnection()).isTrue();
    }

    @Test
    void testConnectionReturnsFalseOnFailure() throws Exception {
        when(cloudinary.api()).thenReturn(api);
        when(api.ping(anyMap())).thenThrow(new RuntimeException("boom"));
        setField(service, "cloudinary", cloudinary);

        assertThat(service.testConnection()).isFalse();
    }

    @Test
    void createUserFolderCreatesSubfolder() throws Exception {
        when(cloudinary.api()).thenReturn(api);
        setField(service, "cloudinary", cloudinary);

        service.createUserFolder("user-1");

        verify(api).createFolder(eq("user-1"), anyMap());
        verify(api).createFolder(eq("user-1/profile"), anyMap());
    }

    @Test
    void createUserFolderRethrowsException() throws Exception {
        when(cloudinary.api()).thenReturn(api);
        doThrow(new Exception("boom")).when(api).createFolder(eq("user-1"), anyMap());
        setField(service, "cloudinary", cloudinary);

        assertThatThrownBy(() -> service.createUserFolder("user-1"))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("boom");
    }

    @Test
    void uploadProfilePictureUsesSanitizedFolder() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "pic.png", "image/png", "data".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "http://example.com"));
        setField(service, "cloudinary", cloudinary);

        service.uploadProfilePicture(file, "User+Test@example.com");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), captor.capture());
        assertThat(captor.getValue()).containsEntry("folder", "users/user_test_example_com");
        assertThat(captor.getValue()).containsEntry("public_id", "profilepic");
    }

    @Test
    void uploadProfilePictureRethrowsIOException() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "pic.png", "image/png", "data".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenThrow(new IOException("boom"));
        setField(service, "cloudinary", cloudinary);

        assertThatThrownBy(() -> service.uploadProfilePicture(file, "user@example.com"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void uploadTicketAttachmentBuildsFolderAndPublicId() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", "data".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "http://example.com"));
        setField(service, "cloudinary", cloudinary);

        service.uploadTicketAttachment(file, "User 1", "Ticket#1");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), captor.capture());
        assertThat(captor.getValue()).containsEntry("folder", "tickets/user_1/ticket_1");
        assertThat(captor.getValue()).containsEntry("public_id", "report.pdf");
    }

    @Test
    void uploadTicketAttachmentRethrowsIOException() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "report.pdf", "application/pdf", "data".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenThrow(new IOException("boom"));
        setField(service, "cloudinary", cloudinary);

        assertThatThrownBy(() -> service.uploadTicketAttachment(file, null, null))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void uploadRawToFolderUsesPdfExtensionAndFolder() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "report", "application/pdf", "data".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "http://example.com", "resource_type", "raw"));
        setField(service, "cloudinary", cloudinary);

        service.uploadRawToFolder(file, "/docs/");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), captor.capture());
        assertThat(captor.getValue()).containsEntry("public_id", "report.pdf");
        assertThat(captor.getValue()).containsEntry("folder", "docs");
        assertThat(captor.getValue()).containsEntry("resource_type", "raw");
    }

    @Test
    void uploadRawToFolderHandlesLeadingDotName() throws Exception {
        MultipartFile file = new MockMultipartFile("file", ".env", "text/plain", "data".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "http://example.com", "resource_type", "raw"));
        setField(service, "cloudinary", cloudinary);

        service.uploadRawToFolder(file, null);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), captor.capture());
        assertThat(captor.getValue()).containsEntry("public_id", "file.env");
        assertThat(captor.getValue()).doesNotContainKey("folder");
    }

    @Test
    void uploadRawToFolderRethrowsIOException() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "report", "application/pdf", "data".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenThrow(new IOException("boom"));
        setField(service, "cloudinary", cloudinary);

        assertThatThrownBy(() -> service.uploadRawToFolder(file, "docs"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void uploadAutoToFolderUsesNormalizedFolder() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "pic.png", "image/png", "data".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class)))
                .thenReturn(Map.of("secure_url", "http://example.com"));
        setField(service, "cloudinary", cloudinary);

        service.uploadAutoToFolder(file, "/auto/");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), captor.capture());
        assertThat(captor.getValue()).containsEntry("folder", "auto");
        assertThat(captor.getValue()).containsEntry("resource_type", "auto");
    }

    @Test
    void uploadAutoToFolderRethrowsIOException() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "pic.png", "image/png", "data".getBytes());
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenThrow(new IOException("boom"));
        setField(service, "cloudinary", cloudinary);

        assertThatThrownBy(() -> service.uploadAutoToFolder(file, "/auto/"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void deleteRawReturnsResult() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("public-id"), anyMap()))
                .thenReturn(Map.of("result", "ok"));
        setField(service, "cloudinary", cloudinary);

        String result = service.deleteRaw("public-id");

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void deleteRawRethrowsIOException() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.destroy(eq("public-id"), anyMap())).thenThrow(new IOException("boom"));
        setField(service, "cloudinary", cloudinary);

        assertThatThrownBy(() -> service.deleteRaw("public-id"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void uploadProfilePictureFromUrlUsesUploader() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(eq("http://img"), any(Map.class)))
                .thenReturn(Map.of("secure_url", "http://example.com"));
        setField(service, "cloudinary", cloudinary);

        service.uploadProfilePictureFromUrl("http://img", "User+Test@example.com");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(eq("http://img"), captor.capture());
        assertThat(captor.getValue()).containsEntry("folder", "users/user_test_example_com");
        assertThat(captor.getValue()).containsEntry("public_id", "profilepic");
    }

    @Test
    void uploadProfilePictureFromUrlRethrowsIOException() throws Exception {
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(eq("http://img"), any(Map.class))).thenThrow(new IOException("boom"));
        setField(service, "cloudinary", cloudinary);

        assertThatThrownBy(() -> service.uploadProfilePictureFromUrl("http://img", "user@example.com"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("boom");
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
