package com.example.backend1.Pantip;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PantipPostRepository extends JpaRepository<PantipPost, Long> {
    Optional<PantipPost> findByUrl(String url);

}
