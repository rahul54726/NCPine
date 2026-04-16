package com.rahul.cinebook.user_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class UserServiceApplication {

	public static void main(String[] args) {
		System.setProperty("spring.data.mongodb.uri", "mongodb+srv://rahul23256:HkPpPssSIpJPdgUY@cluster0.qe3y3wr.mongodb.net/user_db?authSource=admin&retryWrites=true&w=majority");

		System.setProperty("spring.data.mongodb.database", "user_db");

		SpringApplication.run(UserServiceApplication.class, args);
	}
}