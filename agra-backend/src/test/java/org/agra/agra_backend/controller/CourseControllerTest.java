package org.agra.agra_backend.controller;

import org.agra.agra_backend.dao.NotificationRepository;
import org.agra.agra_backend.model.Course;
import org.agra.agra_backend.model.CourseFile;
import org.agra.agra_backend.model.CourseProgress;
import org.agra.agra_backend.model.User;
import org.agra.agra_backend.service.CloudinaryService;
import org.agra.agra_backend.service.CourseLikeService;
import org.agra.agra_backend.service.CourseProgressService;
import org.agra.agra_backend.service.CourseService;
import org.agra.agra_backend.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private CloudinaryService cloudinaryService;
    @Mock
    private CourseService courseService;
    @Mock
    private CourseProgressService courseProgressService;
    @Mock
    private CourseLikeService courseLikeService;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CourseController controller;

    @Test
    void createCourseReturnsCreatedOnSuccess() throws IOException {
        when(courseService.localizeCourse(any(Course.class), any(Locale.class))).thenAnswer(invocation -> {
            Course course = invocation.getArgument(0);
            if (course.getTitle() == null) {
                course.setTitle("Title");
            }
            return course;
        });
        Course created = new Course();
        created.setTitle("Title");
        when(courseService.createCourse(any(Course.class), nullable(MultipartFile.class))).thenReturn(created);

        ResponseEntity<Course> response = controller.createCourse("{\"title\":\"Title\"}", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTitle()).isEqualTo("Title");
        verify(notificationService).createStatusesForAllUsers(any());
        verify(messagingTemplate).convertAndSend(anyString(), any(Object.class));
    }

    @Test
    void createCourseReturnsBadRequestOnInvalidJson() {
        ResponseEntity<Course> response = controller.createCourse("{", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createCourseReturnsBadRequestOnIOException() throws IOException {
        when(courseService.localizeCourse(any(Course.class), any(Locale.class))).thenReturn(new Course());
        when(courseService.createCourse(any(Course.class), nullable(MultipartFile.class)))
                .thenThrow(new IOException("fail"));

        ResponseEntity<Course> response = controller.createCourse("{\"title\":\"Title\"}", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void getAllCoursesSetsLikedWhenAuthenticated() {
        Course c1 = new Course();
        c1.setId("c1");
        Course c2 = new Course();
        c2.setId("c2");
        when(courseService.getAllCourses()).thenReturn(List.of(c1, c2));
        when(courseLikeService.listLikedCourseIds("u1")).thenReturn(List.of("c2"));
        when(courseService.localizeCourses(anyList(), any(Locale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<List<Course>> response = controller.getAllCourses(authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).isLiked()).isFalse();
        assertThat(response.getBody().get(1).isLiked()).isTrue();
    }

    @Test
    void getAllCoursesWithoutAuthReturnsCourses() {
        when(courseService.getAllCourses()).thenReturn(List.of(new Course()));
        when(courseService.localizeCourses(anyList(), any(Locale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<List<Course>> response = controller.getAllCourses(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getActiveCoursesSetsLikedWhenAuthenticated() {
        Course c1 = new Course();
        c1.setId("c1");
        when(courseService.getActiveCourses()).thenReturn(List.of(c1));
        when(courseLikeService.listLikedCourseIds("u1")).thenReturn(List.of("c1"));
        when(courseService.localizeCourses(anyList(), any(Locale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<List<Course>> response = controller.getActiveCourses(authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).isLiked()).isTrue();
    }

    @Test
    void getCourseByIdReturnsNotFoundWhenMissing() {
        when(courseService.getCourseById("c1")).thenReturn(Optional.empty());

        ResponseEntity<Course> response = controller.getCourseById("c1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getCourseByIdSetsLiked() {
        Course course = new Course();
        course.setId("c1");
        when(courseService.getCourseById("c1")).thenReturn(Optional.of(course));
        when(courseLikeService.isLiked("u1", "c1")).thenReturn(true);
        when(courseService.localizeCourse(any(Course.class), any(Locale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Course> response = controller.getCourseById("c1", authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isLiked()).isTrue();
    }

    @Test
    void updateCourseReturnsOk() throws IOException {
        Course updated = new Course();
        when(courseService.updateCourse(anyString(), any(Course.class), nullable(MultipartFile.class)))
                .thenReturn(Optional.of(updated));
        when(courseService.localizeCourse(any(Course.class), any(Locale.class))).thenReturn(updated);

        ResponseEntity<Course> response = controller.updateCourse("c1", "{\"title\":\"X\"}", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateCourseReturnsNotFound() throws IOException {
        when(courseService.updateCourse(anyString(), any(Course.class), nullable(MultipartFile.class)))
                .thenReturn(Optional.empty());

        ResponseEntity<Course> response = controller.updateCourse("c1", "{\"title\":\"X\"}", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateCourseReturnsBadRequestOnInvalidJson() throws IOException {
        ResponseEntity<Course> response = controller.updateCourse("c1", "{", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateCourseReturnsServerErrorOnIOException() throws IOException {
        when(courseService.updateCourse(anyString(), any(Course.class), nullable(MultipartFile.class)))
                .thenThrow(new IOException("fail"));

        ResponseEntity<Course> response = controller.updateCourse("c1", "{\"title\":\"X\"}", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void archiveCourseReturnsNoContent() {
        ResponseEntity<Void> response = controller.archiveCourse("c1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(courseService).ArchiveCourse("c1");
    }

    @Test
    void deleteCourseReturnsNoContent() {
        ResponseEntity<Void> response = controller.deleteCourse("c1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(courseService).deleteCourse("c1");
    }

    @Test
    void getCoursesByCountryReturnsLocalized() {
        when(courseService.getCoursesByCountry("GH")).thenReturn(List.of(new Course()));
        when(courseService.localizeCourses(anyList(), any(Locale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<List<Course>> response = controller.getCoursesByCountry("GH");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getCoursesByDomainReturnsLocalized() {
        when(courseService.getCoursesByDomain("agri")).thenReturn(List.of(new Course()));
        when(courseService.localizeCourses(anyList(), any(Locale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<List<Course>> response = controller.getCoursesByDomain("agri");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getEnrollmentStatusReturnsUnauthorizedWhenNoAuth() {
        ResponseEntity<Object> response = controller.getEnrollmentStatus("course-1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getEnrollmentStatusReturnsEnrolledWhenProgressFound() {
        CourseProgress progress = new CourseProgress();
        progress.setEnrolledAt(new Date());
        progress.setProgressPercentage(50);
        when(courseProgressService.getEnrollmentStatus("u1", "course-1"))
                .thenReturn(Optional.of(progress));

        ResponseEntity<Object> response = controller.getEnrollmentStatus("course-1", authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) response.getBody();
        assertThat(payload).containsEntry("enrolled", true);
    }

    @Test
    void getEnrollmentStatusReturnsNotEnrolledWhenMissing() {
        when(courseProgressService.getEnrollmentStatus("u1", "course-1")).thenReturn(Optional.empty());

        ResponseEntity<Object> response = controller.getEnrollmentStatus("course-1", authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) response.getBody();
        assertThat(payload).containsEntry("enrolled", false);
    }

    @Test
    void getEnrollmentStatusHandlesException() {
        when(courseProgressService.getEnrollmentStatus("u1", "course-1")).thenThrow(new RuntimeException("fail"));

        ResponseEntity<Object> response = controller.getEnrollmentStatus("course-1", authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getOtherUnenrolledCoursesReturnsUnauthorizedWhenNoAuth() {
        ResponseEntity<Object> response = controller.getOtherUnenrolledCourses("course-1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getOtherUnenrolledCoursesReturnsFilteredCourses() {
        CourseProgress enrollment = new CourseProgress();
        enrollment.setCourseId("c1");
        when(courseProgressService.getUserEnrollments("u1")).thenReturn(List.of(enrollment));

        Course enrolled = new Course();
        enrolled.setId("c1");
        Course available = new Course();
        available.setId("c2");
        Course archived = new Course();
        archived.setId("c3");
        archived.setArchived(true);
        Course current = new Course();
        current.setId("course-1");
        Course noId = new Course();
        when(courseService.getAllCourses()).thenReturn(List.of(enrolled, available, archived, current, noId));
        when(courseLikeService.listLikedCourseIds("u1")).thenReturn(List.of("c2"));
        when(courseService.localizeCourses(anyList(), any(Locale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Object> response = controller.getOtherUnenrolledCourses("course-1", authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<Course> result = (List<Course>) response.getBody();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("c2");
        assertThat(result.get(0).isLiked()).isTrue();
    }

    @Test
    void getOtherUnenrolledCoursesHandlesException() {
        when(courseProgressService.getUserEnrollments("u1")).thenThrow(new RuntimeException("fail"));

        ResponseEntity<Object> response = controller.getOtherUnenrolledCourses("course-1", authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void likeCourseReturnsUnauthorizedWhenNoAuth() {
        ResponseEntity<Object> response = controller.likeCourse("c1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void likeCourseReturnsNotFoundWhenMissing() {
        when(courseLikeService.likeCourse("u1", "c1")).thenReturn(false);

        ResponseEntity<Object> response = controller.likeCourse("c1", authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void likeCourseReturnsOk() {
        when(courseLikeService.likeCourse("u1", "c1")).thenReturn(true);

        ResponseEntity<Object> response = controller.likeCourse("c1", authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void unlikeCourseReturnsUnauthorizedWhenNoAuth() {
        ResponseEntity<Object> response = controller.unlikeCourse("c1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void unlikeCourseReturnsNotFoundWhenMissing() {
        when(courseLikeService.unlikeCourse("u1", "c1")).thenReturn(false);

        ResponseEntity<Object> response = controller.unlikeCourse("c1", authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void unlikeCourseReturnsNoContent() {
        when(courseLikeService.unlikeCourse("u1", "c1")).thenReturn(true);

        ResponseEntity<Object> response = controller.unlikeCourse("c1", authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void enrollInCourseReturnsUnauthorizedWhenNoAuth() {
        ResponseEntity<Object> response = controller.enrollInCourse("course-1", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void enrollInCourseReturnsNotFoundWhenCourseMissing() {
        when(courseService.getCourseById("course-1")).thenReturn(Optional.empty());

        ResponseEntity<Object> response = controller.enrollInCourse("course-1", authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void enrollInCourseReturnsCreatedOnSuccess() {
        when(courseService.getCourseById("course-1")).thenReturn(Optional.of(new Course()));

        CourseProgress progress = new CourseProgress();
        progress.setEnrolledAt(new Date());
        progress.setProgressPercentage(25);
        when(courseProgressService.enrollUserInCourse("u1", "course-1")).thenReturn(progress);

        ResponseEntity<Object> response = controller.enrollInCourse("course-1", authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Object body = response.getBody();
        assertThat(body).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) body;
        assertThat(payload).containsEntry("enrolled", true);
    }

    @Test
    void enrollInCourseHandlesException() {
        when(courseService.getCourseById("course-1")).thenReturn(Optional.of(new Course()));
        when(courseProgressService.enrollUserInCourse("u1", "course-1")).thenThrow(new RuntimeException("fail"));

        ResponseEntity<Object> response = controller.enrollInCourse("course-1", authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getEnrolledCoursesReturnsUnauthorizedWhenNoAuth() {
        ResponseEntity<Object> response = controller.getEnrolledCourses(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getEnrolledCoursesReturnsCourses() {
        CourseProgress ok = new CourseProgress();
        ok.setCourseId("c1");
        ok.setEnrolledAt(new Date());
        ok.setProgressPercentage(10);
        CourseProgress missing = new CourseProgress();
        missing.setCourseId("missing");
        CourseProgress archivedProgress = new CourseProgress();
        archivedProgress.setCourseId("archived");

        when(courseProgressService.getUserEnrollments("u1")).thenReturn(List.of(ok, missing, archivedProgress));

        Course okCourse = new Course();
        okCourse.setId("c1");
        okCourse.setTitle("Course");
        Course archived = new Course();
        archived.setId("archived");
        archived.setArchived(true);

        when(courseService.getCourseById("c1")).thenReturn(Optional.of(okCourse));
        when(courseService.getCourseById("missing")).thenReturn(Optional.empty());
        when(courseService.getCourseById("archived")).thenReturn(Optional.of(archived));
        when(courseService.localizeCourse(any(Course.class), any(Locale.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<Object> response = controller.getEnrolledCourses(authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) response.getBody();
        assertThat(payload).containsEntry("totalEnrollments", 3);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> courses = (List<Map<String, Object>>) payload.get("courses");
        assertThat(courses).hasSize(1);
        assertThat(courses.get(0)).containsEntry("id", "c1");
    }

    @Test
    void getEnrolledCoursesHandlesException() {
        when(courseProgressService.getUserEnrollments("u1")).thenThrow(new RuntimeException("fail"));

        ResponseEntity<Object> response = controller.getEnrolledCourses(authWithUser("u1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void cleanupOrphanedEnrollmentsReturnsOk() {
        when(courseProgressService.cleanupOrphanedEnrollments(courseService)).thenReturn(2);

        ResponseEntity<Object> response = controller.cleanupOrphanedEnrollments();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void cleanupOrphanedEnrollmentsReturnsError() {
        when(courseProgressService.cleanupOrphanedEnrollments(courseService)).thenThrow(new RuntimeException("fail"));

        ResponseEntity<Object> response = controller.cleanupOrphanedEnrollments();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void testCloudinaryConnectionReturnsOk() {
        ResponseEntity<Object> response = controller.testCloudinaryConnection();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("cloudinaryConfigured", true);
    }

    @Test
    void testCloudinaryConnectionHandlesException() {
        CourseController spyController = spy(new CourseController(
                messagingTemplate,
                cloudinaryService,
                courseService,
                courseProgressService,
                courseLikeService,
                notificationRepository,
                notificationService));
        doThrow(new RuntimeException("fail")).when(spyController).buildCloudinaryConfig();

        ResponseEntity<Object> response = spyController.testCloudinaryConnection();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void uploadCourseFileRejectsEmptyFile() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        ResponseEntity<Object> response = controller.uploadCourseFile("c1", file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadCourseFileReturnsNotFoundWhenCourseMissing() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(courseService.getCourseById("c1")).thenReturn(Optional.empty());

        ResponseEntity<Object> response = controller.uploadCourseFile("c1", file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void uploadCourseFileReturnsCreated() throws IOException {
        Course course = new Course();
        course.setId("c1");
        when(courseService.getCourseById("c1")).thenReturn(Optional.of(course));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "doc.pdf",
                "application/pdf",
                "content".getBytes(StandardCharsets.UTF_8));

        when(cloudinaryService.uploadImageToFolder(any(MultipartFile.class), anyString())).thenReturn(Map.of(
                "secure_url", "https://cdn.example.com/doc.pdf",
                "public_id", "courses/c1/files/doc",
                "format", "pdf",
                "bytes", 5L,
                "resource_type", "image"
        ));

        ResponseEntity<Object> response = controller.uploadCourseFile("c1", file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOf(CourseFile.class);
        CourseFile courseFile = (CourseFile) response.getBody();
        assertThat(courseFile.getType()).isEqualTo("pdf");
        verify(courseService).save(any(Course.class));
    }

    @Test
    void uploadCourseFileHandlesIOException() throws IOException {
        Course course = new Course();
        course.setId("c1");
        when(courseService.getCourseById("c1")).thenReturn(Optional.of(course));
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(cloudinaryService.uploadImageToFolder(any(MultipartFile.class), anyString()))
                .thenThrow(new IOException("fail"));

        ResponseEntity<Object> response = controller.uploadCourseFile("c1", file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void deleteCourseFileReturnsNotFoundWhenCourseMissing() {
        when(courseService.getCourseById("c1")).thenReturn(Optional.empty());

        ResponseEntity<Object> response = controller.deleteCourseFile("c1", "f1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteCourseFileReturnsNotFoundWhenFilesMissing() {
        Course course = new Course();
        when(courseService.getCourseById("c1")).thenReturn(Optional.of(course));

        ResponseEntity<Object> response = controller.deleteCourseFile("c1", "f1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteCourseFileReturnsNotFoundWhenTargetMissing() {
        Course course = new Course();
        course.setFiles(new ArrayList<>());
        when(courseService.getCourseById("c1")).thenReturn(Optional.of(course));

        ResponseEntity<Object> response = controller.deleteCourseFile("c1", "f1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteCourseFileReturnsNoContentOnSuccess() throws IOException {
        Course course = new Course();
        CourseFile file = new CourseFile("f1", "doc.pdf", "pdf", "url", "pid", 1L, new Date());
        course.setFiles(new ArrayList<>(List.of(file)));
        when(courseService.getCourseById("c1")).thenReturn(Optional.of(course));

        ResponseEntity<Object> response = controller.deleteCourseFile("c1", "f1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(cloudinaryService).deleteRaw("pid");
        verify(courseService).save(course);
    }

    @Test
    void deleteCourseFileHandlesIOException() throws IOException {
        Course course = new Course();
        CourseFile file = new CourseFile("f1", "doc.pdf", "pdf", "url", "pid", 1L, new Date());
        course.setFiles(new ArrayList<>(List.of(file)));
        when(courseService.getCourseById("c1")).thenReturn(Optional.of(course));
        doThrow(new IOException("fail")).when(cloudinaryService).deleteRaw("pid");

        ResponseEntity<Object> response = controller.deleteCourseFile("c1", "f1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void inferFileExtensionCoversBranches() {
        String fromFormat = ReflectionTestUtils.invokeMethod(controller, "inferFileExtension",
                "doc.pdf", "PDF", "https://cdn/doc.pdf", "public.id");
        assertThat(fromFormat).isEqualTo("pdf");

        String fromOriginal = ReflectionTestUtils.invokeMethod(controller, "inferFileExtension",
                "doc.txt", null, "https://cdn/file", "public.id");
        assertThat(fromOriginal).isEqualTo("txt");

        String fromUrl = ReflectionTestUtils.invokeMethod(controller, "inferFileExtension",
                "doc", null, "https://cdn/path/file.csv?x=1", "public.id");
        assertThat(fromUrl).isEqualTo("csv");

        String fromPublicId = ReflectionTestUtils.invokeMethod(controller, "inferFileExtension",
                "doc", null, "https://cdn/path/file", "courses/id/file.jpg");
        assertThat(fromPublicId).isEqualTo("jpg");

        String nullExt = ReflectionTestUtils.invokeMethod(controller, "inferFileExtension",
                "doc", null, "https://cdn/path/file", "courses/id/file");
        assertThat(nullExt).isNull();

        String longExt = ReflectionTestUtils.invokeMethod(controller, "inferFileExtension",
                "doc.averyverylongext", null, "https://cdn/path/file", "courses/id/file");
        assertThat(longExt).isNull();
    }

    private Authentication authWithUser(String userId) {
        Authentication authentication = mock(Authentication.class);
        User user = new User();
        user.setId(userId);
        when(authentication.getPrincipal()).thenReturn(user);
        return authentication;
    }
}
