package org.agra.agra_backend.service;

import org.agra.agra_backend.model.User;
import org.agra.agra_backend.dao.UserRepository;

import org.agra.agra_backend.payload.RegisterRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class UserService implements UserServiceInterface {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email is already in use");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCountry(request.getCountry());
        user.setLanguage(request.getLanguage());
        user.setDomain(request.getDomain());
        user.setRole(request.getRole());
        user.setRegisteredAt(new Date());

        return userRepository.save(user);
    }
}
