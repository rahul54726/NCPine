package com.NPCine.movies_service.config;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.NPCine.movies_service.entity.Movie;
import com.NPCine.movies_service.entity.Showtime;
import com.NPCine.movies_service.repository.MovieRepository;
import com.NPCine.movies_service.repository.ShowtimeRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;

    public DataSeeder(MovieRepository movieRepository, ShowtimeRepository showtimeRepository) {
        this.movieRepository = movieRepository;
        this.showtimeRepository = showtimeRepository;
    }

    @Override
    public void run(String... args) {
        if (movieRepository.count() == 0) {
            seedMovies();
        }
        if (showtimeRepository.count() == 0) {
            seedShowtimes();
        }
    }

    private void seedMovies() {
        List<Movie> seedMovies = List.of(
                new Movie(
                        null,
                        "Inception",
                        "A skilled thief enters dreams to steal secrets but is tasked with planting an idea that could change reality forever.",
                        "https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?auto=format&fit=crop&w=800&q=80",
                        List.of("Sci-Fi", "Thriller", "Action"),
                        148,
                        "English",
                        true),
                new Movie(
                        null,
                        "The Dark Knight",
                        "Batman faces the Joker, a criminal mastermind who plunges Gotham into chaos and forces impossible moral choices.",
                        "https://images.unsplash.com/photo-1536440136628-849c177e76a1?auto=format&fit=crop&w=800&q=80",
                        List.of("Action", "Crime", "Drama"),
                        152,
                        "English",
                        true),
                new Movie(
                        null,
                        "Interstellar",
                        "A team of explorers travels through a wormhole in search of a new home for humanity as Earth nears collapse.",
                        "https://images.unsplash.com/photo-1502134249126-9f3755a50d78?auto=format&fit=crop&w=800&q=80",
                        List.of("Sci-Fi", "Adventure", "Drama"),
                        169,
                        "English",
                        true),
                new Movie(
                        null,
                        "Avatar",
                        "A marine on the distant moon Pandora is torn between military orders and protecting the world he has grown to love.",
                        "https://images.unsplash.com/photo-1440404653325-ab127d49abc1?auto=format&fit=crop&w=800&q=80",
                        List.of("Sci-Fi", "Adventure", "Fantasy"),
                        162,
                        "English",
                        true),
                new Movie(
                        null,
                        "The Matrix",
                        "A hacker discovers that reality is a simulation and joins a rebellion to free humanity from machine control.",
                        "https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?auto=format&fit=crop&w=800&q=80",
                        List.of("Sci-Fi", "Action"),
                        136,
                        "English",
                        true));

        movieRepository.saveAll(seedMovies);
    }

    private void seedShowtimes() {
        List<Movie> movies = movieRepository.findByIsActiveTrue();
        if (movies.isEmpty()) {
            return;
        }

        List<Showtime> showtimes = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        for (int i = 0; i < Math.min(5, movies.size()); i++) {
            Showtime showtime = new Showtime();
            showtime.setMovieId(movies.get(i).getId());
            showtime.setScreenId("SCREEN-" + (i + 1));
            showtime.setStartTime(baseTime.plusHours(i * 3L));
            showtime.setEndTime(baseTime.plusHours(i * 3L + 2));
            showtimes.add(showtime);
        }
        showtimeRepository.saveAll(showtimes);
    }
}
