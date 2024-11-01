package cloud.zipbob.edgeservice.auth.jwt;

import cloud.zipbob.edgeservice.auth.PrincipalDetails;
import cloud.zipbob.edgeservice.auth.dto.TokenDto;
import cloud.zipbob.edgeservice.auth.exception.TokenException;
import cloud.zipbob.edgeservice.auth.exception.TokenExceptionType;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Calendar;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    private final JwtTokenProperties jwtTokenProperties;
    private static final String BEARER = "Bearer";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String REFRESH_TOKEN_HEADER = "Refresh";
    private static final String BEARER_PREFIX = "Bearer ";

    private Key key;

    @PostConstruct
    public void init() {
        String base64EncodedSecretKey = encodeBase64SecretKey(jwtTokenProperties.getSecretKey());
        this.key = getKeyFromBase64EncodeKey(base64EncodedSecretKey);
    }

    public String encodeBase64SecretKey(String secretKey) {
        return Encoders.BASE64.encode(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    private Key getKeyFromBase64EncodeKey(String base64EncodedSecretKey) {
        byte[] keyBytes = Decoders.BASE64.decode(base64EncodedSecretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public TokenDto generateTokenDto(PrincipalDetails principalDetails) {
        log.info("GenerateTokenDto execute principalDetails : {} {}",
                principalDetails.getUsername(), principalDetails.getAuthorities());
        log.info("expiration period : {}", jwtTokenProperties.getAccessExpiration());
        Date accessTokenExpirationDate = getTokenExpiration(jwtTokenProperties.getAccessExpiration());
        Date refreshTokenExpirationDate = getTokenExpiration(jwtTokenProperties.getRefreshExpiration());
        String role = principalDetails.getAuthorities().iterator().next().getAuthority();

        String accessToken = Jwts.builder()
                .claim("role", role)
                .setSubject(principalDetails.getUsername())
                .setExpiration(accessTokenExpirationDate)
                .setIssuedAt(Calendar.getInstance().getTime())
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        String refreshToken = Jwts.builder()
                .setSubject(principalDetails.getUsername())
                .setExpiration(refreshTokenExpirationDate)
                .setIssuedAt(Calendar.getInstance().getTime())
                .signWith(key)
                .compact();

        log.info("accessToken: {} refreshToken: {}", accessToken, refreshToken);

        return TokenDto.builder()
                .grantType(BEARER)
                .authorizationType(AUTHORIZATION_HEADER)
                .accessToken(accessToken)
                .accessTokenExpiresIn(accessTokenExpirationDate.getTime())
                .refreshToken(refreshToken)
                .build();
    }

    public Authentication getAuthentication(String accessToken) {
        log.info("getAuthentication execute accessToken: {}", accessToken);
        Claims claims = parseClaims(accessToken);

        if (claims.get("role") == null) {
            log.debug("getAuthentication exception execute : no role in accessToken : {}", accessToken);
            throw new BadCredentialsException("Invalid access token");
        }

        String authority = claims.get("role").toString();
        PrincipalDetails principalDetails = PrincipalDetails.of(
                claims.getSubject(), authority);
        log.info("getAuthentication Role check : {}",
                principalDetails.getAuthorities().iterator().next().getAuthority());
        return new UsernamePasswordAuthenticationToken(principalDetails, null, principalDetails.getAuthorities());
    }

    public boolean validateToken(String token) {
        log.info("ValidateToken execute, token = {}", token);
        try {
            Jwts.parserBuilder()
                    .setSigningKey(this.key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (MalformedJwtException e) {
            log.info("Invalid JWT token");
            log.trace("Invalid JWT token trace = { }", e);
            throw new TokenException(TokenExceptionType.TOKEN_INVALID);
        } catch (ExpiredJwtException e) {
            log.info("Expired JWT token");
            log.info("Expired JWT token trace = { }", e);
            throw new TokenException(TokenExceptionType.TOKEN_EXPIRED);
        } catch (UnsupportedJwtException e) {
            log.info("Unsupported JWT token");
            log.trace("Unsupported JWT token trace = { }", e);
            throw new TokenException(TokenExceptionType.TOKEN_INVALID);
        } catch (IllegalArgumentException e) {
            log.info("JWT claims string is empty.");
            log.trace("JWT claims string is empty trace = { }", e);
            throw new TokenException(TokenExceptionType.TOKEN_INVALID);
        }
    }

    private Date getTokenExpiration(long expirationPeriod) {
        Date date = new Date();
        return new Date(date.getTime() + expirationPeriod);
    }

    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }

    public void accessTokenSetHeader(String accessToken, HttpServletResponse response) {
        String headerValue = BEARER_PREFIX + accessToken;
        response.setHeader(AUTHORIZATION_HEADER, headerValue);
    }

    public void refreshTokenSetHeader(String refreshToken, HttpServletResponse response) {
        response.setHeader(REFRESH_TOKEN_HEADER, refreshToken);
    }

    public String resolveAccessToken(HttpServletRequest request) {
        log.info("ResolveAccessToken execute request = {}", request.toString());
        String accessToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(accessToken) && accessToken.startsWith(BEARER_PREFIX)) {
            return accessToken.substring(7);
        }
        return null;
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        log.info("ResolveRefreshToken execute, request = {}", request.toString());
        String refreshToken = request.getHeader(REFRESH_TOKEN_HEADER);
        if (StringUtils.hasText(refreshToken)) {
            return refreshToken;
        }
        return null;
    }
}
