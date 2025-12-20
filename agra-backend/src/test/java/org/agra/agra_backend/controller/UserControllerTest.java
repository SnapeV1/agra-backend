package org.agra.agra_backend.controller;

import org.agra.agra_backend.model.User;
import org.agra.agra_backend.payload.ChangePasswordRequest;
import org.agra.agra_backend.service.CourseLikeService;
import org.agra.agra_backend.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private CourseLikeService courseLikeService;
    @Mock
    private UserService userService;

    @InjectMocks
    private UserController controller;

    @Test
    void getMyLikedCoursesReturnsUnauthorizedWithoutAuth() {
        ResponseEntity<?> response = controller.getMyLikedCourses(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getMyLikedCoursesReturnsLikedIds() {
        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        User user = new User();
        user.setId("user-1");
        when(authentication.getPrincipal()).thenReturn(user);
        when(courseLikeService.listLikedCourseIds("user-1")).thenReturn(List.of("c1", "c2"));

        ResponseEntity<?> response = controller.getMyLikedCourses(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(List.of("c1", "c2"));
    }

    @Test
    void updateUserProfileJsonReturnsUnauthorizedWithoutAuth() {
        ResponseEntity<?> response = controller.updateUserProfileJson(new User(), null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateUserByAdminReturnsNotFoundWhenMissing() throws Exception {
        when(userService.findById("user-1")).thenReturn(null);

        ResponseEntity<?> response = controller.updateUserByAdmin("user-1", "{}", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void changePasswordReturnsUnauthorizedWithoutAuth() {
        ChangePasswordRequest request = new ChangePasswordRequest();

        ResponseEntity<?> response = controller.changePassword(request, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void changePasswordCallsServiceWhenAuthenticated() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old");
        request.setNewPassword("new");

        Authentication authentication = org.mockito.Mockito.mock(Authentication.class);
        User user = new User();
        user.setId("user-1");
        when(authentication.getPrincipal()).thenReturn(user);

        ResponseEntity<?> response = controller.changePassword(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        verify(userService).changePassword("user-1", "old", "new");
    }
}
