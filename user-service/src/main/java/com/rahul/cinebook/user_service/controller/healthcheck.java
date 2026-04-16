package com.rahul.cinebook.user_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;

@RestController
@RequestMapping("/health")
public class healthcheck {
    @GetMapping
    public ResponseEntity<?> getHealth(){
        HashMap<String,String> output = new HashMap<>();
        output.put("time" , LocalDateTime.now().toString());
        output.put("Status" , "this is running fine");
        return new ResponseEntity<>(output , HttpStatus.OK);
    }
}
