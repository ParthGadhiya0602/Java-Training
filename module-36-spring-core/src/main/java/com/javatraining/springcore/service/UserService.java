package com.javatraining.springcore.service;

import com.javatraining.springcore.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Demonstrates constructor injection - the preferred DI style.
 *
 * <p>Constructor injection:
 * <ul>
 *   <li>Makes dependencies explicit and mandatory</li>
 *   <li>Enables the field to be {@code final} - guarantees immutability</li>
 *   <li>Works without {@code @Autowired} when there is exactly one constructor
 *       (Spring 4.3+)</li>
 *   <li>Makes the class trivially testable without a Spring context</li>
 * </ul>
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    // @Autowired is optional when there is a single constructor
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<String> findUser(long id) {
        return userRepository.findById(id);
    }

    public void createUser(long id, String name) {
        userRepository.save(id, name);
    }
}
