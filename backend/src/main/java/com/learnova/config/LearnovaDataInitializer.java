package com.learnova.config;

import com.learnova.model.Course;
import com.learnova.repository.CourseRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class LearnovaDataInitializer {

    @Bean
    CommandLineRunner initCourses(CourseRepository courseRepository) {
        return args -> {
            if (courseRepository.count() > 0) {
                return;
            }

            Course java = new Course();
            java.setName("Java Fundamentals");
            java.setDescription("Introduzione alla programmazione Java");
            java.setPrice(new BigDecimal("49.90"));

            Course spring = new Course();
            spring.setName("Spring Boot Essentials");
            spring.setDescription("Creazione di API REST con Spring Boot");
            spring.setPrice(new BigDecimal("79.90"));

            Course database = new Course();
            database.setName("PostgreSQL Basics");
            database.setDescription("Fondamenti di database relazionali");
            database.setPrice(new BigDecimal("39.90"));

            courseRepository.save(java);
            courseRepository.save(spring);
            courseRepository.save(database);
        };
    }
}
