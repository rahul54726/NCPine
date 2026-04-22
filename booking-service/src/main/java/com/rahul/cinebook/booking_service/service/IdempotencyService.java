package com.rahul.cinebook.booking_service.service;

import com.rahul.cinebook.booking_service.entity.IdempotencyRecord;
import com.rahul.cinebook.booking_service.repository.IdempotencyRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {
    private final IdempotencyRepo idempotencyRepo ;
    public  boolean isDuplicate(String key){
        return idempotencyRepo.existsById(key);
    }
    public void save(String key ,
                     String userEmail ,
                     String responseHash){
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setUserEmail(userEmail);
        record.setResponseHash(responseHash);
        record.setCreatedAt(LocalDateTime.now());
        idempotencyRepo.save(record);
    }
    public Optional<IdempotencyRecord> get(String key){
        return idempotencyRepo.findById(key);
    }
    @Transactional
    public Optional<ResponseEntity<?>> handleIdempotency(String key ,
                                                         String userEmail ,
                                                         Object requestBody ){
        Optional<IdempotencyRecord> record = idempotencyRepo.findById(key);
        if (record.isPresent()){
            if (record.get().getUserEmail().equals(userEmail)){
                return Optional.of(ResponseEntity.ok("Request already processed."));
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency key belongs to another user.");
        }
        IdempotencyRecord newRecord = new IdempotencyRecord();
        newRecord.setIdempotencyKey(key);
        newRecord.setUserEmail(userEmail);
        newRecord.setCreatedAt(LocalDateTime.now());
        idempotencyRepo.save(newRecord);
        return Optional.empty();
    }
}
