package com.comprehensive.eureka.admin.repository;

import com.comprehensive.eureka.admin.entity.ForbiddenWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ForbiddenWordRepository extends JpaRepository<ForbiddenWord, Long> {
    List<ForbiddenWord> findByStatus(boolean status);

    List<ForbiddenWord> findByStatusAndWord(boolean status, String word);

    Optional<ForbiddenWord> findByWord(String word);

    @Query("SELECT f.id FROM ForbiddenWord f WHERE f.word = :word AND f.status = true")
    Optional<Long> findIdByWord(@Param("word") String word);

    boolean existsByWord(String word);
}
