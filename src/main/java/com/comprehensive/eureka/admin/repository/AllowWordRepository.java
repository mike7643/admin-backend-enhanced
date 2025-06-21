package com.comprehensive.eureka.admin.repository;

import com.comprehensive.eureka.admin.entity.AllowWord;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllowWordRepository extends JpaRepository<AllowWord, Long> {

    boolean existsByWord(String word);
    List<AllowWord> findByStatus(boolean status);
}
