package com.rahul.cinebook.booking_service.repository;

import com.rahul.cinebook.booking_service.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IdempotencyRepo extends JpaRepository<IdempotencyRecord , String> {
}
