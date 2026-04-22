package com.NPCine.movies_service.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.NPCine.movies_service.entity.Movie;

public interface MovieRepository extends MongoRepository<Movie, String> {

    List<Movie> findByIsActiveTrue();
}
