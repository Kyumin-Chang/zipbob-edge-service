package cloud.zipbob.edgeservice.auth.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "jwt")
public class JwtTokenProperties {
    private String secretKey;

    private long accessExpiration;

    private long refreshExpiration;

}
