package com.NPCine.movies_service.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.NPCine.movies_service.entity.Showtime;

public interface ShowtimeRepository extends MongoRepository<Showtime, String> {

    List<Showtime> findByMovieId(String movieId);
}
