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
import static org.mockito.Mockito.mock;
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

    private Authentication authWithUser(String userId) {
        Authentication authentication = mock(Authentication.class);
        User user = new User();
        user.setId(userId);
        when(authentication.getPrincipal()).thenReturn(user);
        return authentication;
    }

    @Test
    void getMyLikedCoursesReturnsUnauthorizedWithoutAuth() {
        ResponseEntity<?> response = controller.getMyLikedCourses(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getMyLikedCoursesReturnsLikedIds() {
        Authentication authentication = authWithUser("user-1");
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
    void updateUserProfileJsonUpdatesUser() {
        Authentication authentication = authWithUser("user-1");
        User existing = new User();
        existing.setId("user-1");
        existing.setName("Old");
        when(userService.findById("user-1")).thenReturn(existing);
        when(userService.updateUser(existing)).thenReturn(existing);

        User incoming = new User();
        incoming.setName("New");
        incoming.setCountry("GH");

        ResponseEntity<?> response = controller.updateUserProfileJson(incoming, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(existing.getName()).isEqualTo("New");
        assertThat(existing.getCountry()).isEqualTo("GH");
    }

    @Test
    void updateUserProfileJsonReturnsServerErrorOnException() {
        Authentication authentication = authWithUser("user-1");
        User existing = new User();
        existing.setId("user-1");
        when(userService.findById("user-1")).thenReturn(existing);
        when(userService.updateUser(existing)).thenThrow(new RuntimeException("boom"));

        ResponseEntity<?> response = controller.updateUserProfileJson(new User(), authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void updateUserProfileReturnsUnauthorizedWithoutAuth() throws Exception {
        ResponseEntity<?> response = controller.updateUserProfile("{}", null, null, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateUserProfileUpdatesUser() throws Exception {
        Authentication authentication = authWithUser("user-1");
        User existing = new User();
        existing.setId("user-1");
        existing.setName("Old");
        when(userService.findById("user-1")).thenReturn(existing);
        when(userService.updateUser(existing, null)).thenReturn(existing);

        String json = "{\"name\":\"New\",\"country\":\"GH\"}";

        ResponseEntity<?> response = controller.updateUserProfile(json, null, authentication, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(existing.getName()).isEqualTo("New");
        assertThat(existing.getCountry()).isEqualTo("GH");
    }

    @Test
    void updateUserProfileReturnsServerErrorOnBadJson() throws Exception {
        Authentication authentication = authWithUser("user-1");
        User existing = new User();
        existing.setId("user-1");
        when(userService.findById("user-1")).thenReturn(existing);

        ResponseEntity<?> response = controller.updateUserProfile("{bad", null, authentication, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void updateUserByAdminReturnsUpdatedUser() throws Exception {
        User existing = new User();
        existing.setId("user-1");
        when(userService.findById("user-1")).thenReturn(existing);
        when(userService.updateUser(existing, null)).thenReturn(existing);

        ResponseEntity<?> response = controller.updateUserByAdmin("user-1", "{\"name\":\"Admin\"}", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateUserByAdminReturnsNotFoundWhenMissing() throws Exception {
        when(userService.findById("user-1")).thenReturn(null);

        ResponseEntity<?> response = controller.updateUserByAdmin("user-1", "{}", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void updateUserByAdminReturnsServerErrorOnBadJson() throws Exception {
        ResponseEntity<?> response = controller.updateUserByAdmin("user-1", "{bad", null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void addUserReturnsSavedUser() {
        User user = new User();
        user.setId("u1");
        when(userService.saveUser(user)).thenReturn(user);

        ResponseEntity<User> response = controller.addUser(user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(user);
    }

    @Test
    void getAllUsersReturnsUsers() {
        when(userService.getAllUsers()).thenReturn(List.of(new User(), new User()));

        ResponseEntity<List<User>> response = controller.getAllUsers();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
    }

    @Test
    void pingReturnsPong() {
        ResponseEntity<String> response = controller.ping();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("pong");
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

        Authentication authentication = authWithUser("user-1");

        ResponseEntity<?> response = controller.changePassword(request, authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        verify(userService).changePassword("user-1", "old", "new");
    }
}
