package com.comprehensive.eureka.admin.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class ForbiddenWordRedisService {

    private static final String KEY = "forbidden:words";

    private final StringRedisTemplate redisTemplate;

    public ForbiddenWordRedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addForbiddenWord(String word) {
        redisTemplate.opsForSet().add(KEY, word);
    }

    public void removeForbiddenWord(String word) {
        redisTemplate.opsForSet().remove(KEY, word);
    }

    public Set<String> getAllForbiddenWords() {
        return redisTemplate.opsForSet().members(KEY);
    }

    public boolean isForbidden(String word) {
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(KEY, word));
    }
}