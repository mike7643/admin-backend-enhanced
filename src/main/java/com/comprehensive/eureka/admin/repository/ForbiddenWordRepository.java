package com.comprehensive.eureka.admin.repository;

import com.comprehensive.eureka.admin.entity.ForbiddenWord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ForbiddenWordRepository extends JpaRepository<ForbiddenWord, Long> {
}
