package com.rahul.cinebook.booking_service.repository;

import com.rahul.cinebook.booking_service.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShowtimeRepo extends JpaRepository<Showtime , String> {
}
