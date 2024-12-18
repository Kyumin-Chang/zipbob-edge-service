package cloud.zipbob.edgeservice.global.redis;

import lombok.AllArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class RedisRateLimiterService {

    private StringRedisTemplate redisTemplate;

    private final int MAX_REQUESTS_PER_SECOND = 100;
    private final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    public boolean isRequestAllowed(String key) {
        String redisKey = RATE_LIMIT_KEY_PREFIX + key;
        Long currentCount = redisTemplate.opsForValue().increment(redisKey, 1);
        if (currentCount == null) {
            currentCount = 1L;
        }
        return currentCount <= MAX_REQUESTS_PER_SECOND;
    }
}
