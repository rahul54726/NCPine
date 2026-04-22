package com.NPCine.movies_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.NPCine.movies_service.dto.request.CreateShowtimeRequest;
import com.NPCine.movies_service.dto.response.CatalogMovieResponse;
import com.NPCine.movies_service.entity.Movie;
import com.NPCine.movies_service.entity.Showtime;
import com.NPCine.movies_service.service.MovieService;

@RestController
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/catalog/movies")
    public List<CatalogMovieResponse> getAllActiveMovies() {
        return movieService.getAllActiveMovies();
    }

    @GetMapping("/catalog/movies/{id}")
    public Movie getMovieById(@PathVariable String id) {
        return movieService.getMovieById(id);
    }

    @PostMapping("/admin/catalog/movies")
    public ResponseEntity<Movie> createMovie(@RequestBody Movie movie,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(movieService.createMovie(movie));
    }

    @PostMapping("/admin/catalog/shows")
    public ResponseEntity<Showtime> createShowtime(@RequestBody CreateShowtimeRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(movieService.createShowtime(request));
    }

    private void requireAdmin(String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role required");
        }
    }
}
