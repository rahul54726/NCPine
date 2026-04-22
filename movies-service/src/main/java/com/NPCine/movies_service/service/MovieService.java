package com.NPCine.movies_service.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.NPCine.movies_service.dto.request.CreateShowtimeRequest;
import com.NPCine.movies_service.dto.response.CatalogMovieResponse;
import com.NPCine.movies_service.entity.Movie;
import com.NPCine.movies_service.entity.Showtime;
import com.NPCine.movies_service.repository.MovieRepository;
import com.NPCine.movies_service.repository.ShowtimeRepository;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;

    public MovieService(MovieRepository movieRepository, ShowtimeRepository showtimeRepository) {
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
    }

    public List<CatalogMovieResponse> getAllActiveMovies() {
        List<Movie> activeMovies = movieRepository.findByIsActiveTrue();
        Map<String, List<Showtime>> showsByMovie = showtimeRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(Showtime::getMovieId));

        List<CatalogMovieResponse> response = new ArrayList<>();
        for (Movie movie : activeMovies) {
            List<Showtime> showtimes = showsByMovie.getOrDefault(movie.getId(), List.of());
            if (showtimes.isEmpty()) {
                response.add(toCatalogMovieResponse(movie, null));
                continue;
            }
            for (Showtime showtime : showtimes) {
                response.add(toCatalogMovieResponse(movie, showtime.getId()));
            }
        }
        return response;
    }

    public Movie getMovieById(String id) {
        return movieRepository.findById(id)
                .filter(Movie::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));
    }

    public Movie createMovie(Movie movie) {
        movie.setId(null);
        if (movie.getGenres() == null) {
            movie.setGenres(List.of());
        }
        movie.setActive(true);
        return movieRepository.save(movie);
    }

    public Showtime createShowtime(CreateShowtimeRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Movie not found"));
        if (!movie.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot create show for inactive movie");
        }
        if (request.getStartTime() == null || request.getEndTime() == null || !request.getEndTime().isAfter(request.getStartTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Show endTime must be after startTime");
        }
        if (request.getStartTime().isBefore(LocalDateTime.now().minusMinutes(1))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Show startTime must be in the future");
        }

        Showtime showtime = new Showtime();
        showtime.setMovieId(request.getMovieId());
        showtime.setScreenId(request.getScreenId());
        showtime.setStartTime(request.getStartTime());
        showtime.setEndTime(request.getEndTime());
        return showtimeRepository.save(showtime);
    }

    private CatalogMovieResponse toCatalogMovieResponse(Movie movie, String showtimeId) {
        String genre = movie.getGenres() == null || movie.getGenres().isEmpty() ? "N/A" : movie.getGenres().get(0);
        String duration = movie.getDurationMinutes() == null ? "N/A" : movie.getDurationMinutes() + " min";
        return new CatalogMovieResponse(
                movie.getId(),
                movie.getTitle(),
                genre,
                duration,
                "U/A",
                250.0,
                movie.getPosterUrl(),
                showtimeId == null ? "" : showtimeId);
    }
}
