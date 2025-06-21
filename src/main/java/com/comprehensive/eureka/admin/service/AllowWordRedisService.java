package com.comprehensive.eureka.admin.service;

import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AllowWordRedisService {

    private static final String KEY = "allow:words";

    private final StringRedisTemplate redisTemplate;

    public AllowWordRedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addAllowWord(String word) {
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