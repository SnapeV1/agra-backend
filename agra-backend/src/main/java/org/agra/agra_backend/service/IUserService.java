package org.agra.agra_backend.service;

import org.agra.agra_backend.model.User;

import java.util.List;

public interface IUserService {
    User saveUser(User user);
    List<User> getAllUsers();
    User updateUser(User user);
    void deleteUser(Long id);

}
