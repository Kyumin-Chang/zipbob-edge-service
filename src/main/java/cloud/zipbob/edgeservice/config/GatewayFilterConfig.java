package cloud.zipbob.edgeservice.config;

import cloud.zipbob.edgeservice.auth.filter.AddMemberIdHeaderFilter;
import cloud.zipbob.edgeservice.global.redis.RedisRateLimiterService;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@RequiredArgsConstructor
public class GatewayFilterConfig {

    private final RedisRateLimiterService redisRateLimiterService;

    @Bean
    public FilterRegistrationBean<AddMemberIdHeaderFilter> addMemberIdHeaderFilter() {
        FilterRegistrationBean<AddMemberIdHeaderFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new AddMemberIdHeaderFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(2);
        return registrationBean;
    }

    @Bean
    public WebFilter rateLimitingFilter() {
        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            String key = (authentication != null && authentication.isAuthenticated())
                    ? authentication.getName()
                    : getClientIP(request);

            if (!redisRateLimiterService.isRequestAllowed(key)) {
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

                String message = "{\"error\": \"Rate limit exceeded\", \"message\": \"Too many requests. Please try again later.\"}";
                byte[] bytes = message.getBytes(StandardCharsets.UTF_8);

                return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
            }

            return chain.filter(exchange);
        };
    }

    private String getClientIP(ServerHttpRequest request) {
        return request.getHeaders().getFirst("X-Forwarded-For") != null
                ? request.getHeaders().getFirst("X-Forwarded-For")
                : Objects.requireNonNull(request.getRemoteAddress()).getAddress().getHostAddress();
    }
}
