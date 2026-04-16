package com.rahul.cinebook.user_service.repository;

import com.rahul.cinebook.user_service.entity.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import javax.swing.text.html.Option;
import java.util.Optional;

public interface UserRepo extends MongoRepository<User , String> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
