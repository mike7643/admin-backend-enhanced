package com.comprehensive.eureka.admin.repository;

import com.comprehensive.eureka.admin.entity.ForbiddenWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ForbiddenWordRepository extends JpaRepository<ForbiddenWord, Long> {
    List<ForbiddenWord> findByStatus(boolean status);

    List<ForbiddenWord> findByStatusAndWord(boolean status, String word);

    Optional<ForbiddenWord> findByWord(String word);

    boolean existsByWord(String word);
}
