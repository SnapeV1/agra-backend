package org.agra.agra_backend.service;

import org.agra.agra_backend.model.User;
import org.agra.agra_backend.dao.UserRepository;

import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements UserServiceInterface {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Add a new user
    public User saveUser(User user) {
        user.setRegisteredAt(new Date());
        return userRepository.save(user);
    }


    public User updateUser(User user) {
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        if (userRepository.existsById(String.valueOf(id))) {
            userRepository.deleteById(String.valueOf(id));
        } else {
            throw new RuntimeException("User with id " + id + " not found");
        }
    }
}
