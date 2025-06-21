package com.comprehensive.eureka.admin.repository;

import com.comprehensive.eureka.admin.entity.AllowWord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllowWordRepository extends JpaRepository<AllowWord, Long> {

    boolean existsByWord(String word);
    List<AllowWord> findByStatus(boolean status);
    Optional<List<AllowWord>> findByUsedAndWord(boolean used, String word);
    Optional<List<AllowWord>> findByUsed(boolean used);
    Optional<AllowWord> findByWord(String word);
}
