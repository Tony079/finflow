package com.finflow.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisService(
            RedisTemplate<String, String> redisTemplate,
            ObjectMapper objectMapper) {

        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void set(
            String key,
            String value,
            Duration ttl) {

        redisTemplate
                .opsForValue()
                .set(key, value, ttl);
    }

    public String get(String key) {

        return redisTemplate
                .opsForValue()
                .get(key);
    }

    public void delete(String key) {

        redisTemplate.delete(key);
    }

    public <T> void setObject(
            String key,
            T value,
            Duration ttl) {

        try {

            String json =
                    objectMapper.writeValueAsString(value);

            set(key, json, ttl);

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Failed to serialize Redis value",
                    exception
            );
        }
    }

    public <T> T getObject(
            String key,
            Class<T> type) {

        String json = get(key);

        if (json == null) {
            return null;
        }

        try {

            return objectMapper.readValue(
                    json,
                    type
            );

        } catch (JsonProcessingException exception) {

            throw new IllegalStateException(
                    "Failed to deserialize Redis value",
                    exception
            );
        }
    }
}