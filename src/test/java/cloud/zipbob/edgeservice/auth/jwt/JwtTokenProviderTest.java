package cloud.zipbob.edgeservice.auth.jwt;

import cloud.zipbob.edgeservice.auth.PrincipalDetails;
import cloud.zipbob.edgeservice.auth.dto.TokenDto;
import cloud.zipbob.edgeservice.auth.exception.TokenException;
import cloud.zipbob.edgeservice.auth.exception.TokenExceptionType;
import cloud.zipbob.edgeservice.domain.member.Member;
import cloud.zipbob.edgeservice.domain.member.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
class JwtTokenProviderTest {

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtTokenProperties jwtTokenProperties;

    private Key key;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        when(jwtTokenProperties.getSecretKey()).thenReturn("test-secret-key-that-is-long-enough-very-very-very-long-key");
        when(jwtTokenProperties.getAccessExpiration()).thenReturn(3600000L);  // 1시간
        when(jwtTokenProperties.getRefreshExpiration()).thenReturn(1209600000L);  // 2주

        jwtTokenProvider = new JwtTokenProvider(jwtTokenProperties);
        jwtTokenProvider.init();
        key = (Key) ReflectionTestUtils.getField(jwtTokenProvider, "key");
    }

    @Test
    @DisplayName("JWT 토큰 생성 확인")
    void generateTokenDto_Check() {
        // given
        Member member = Member.builder()
                .email("test@example.com")
                .password("password")
                .role(Role.USER)
                .build();
        PrincipalDetails principalDetails = new PrincipalDetails(member);

        // when
        TokenDto tokenDto = jwtTokenProvider.generateTokenDto(principalDetails);

        // then
        assertNotNull(tokenDto.getAccessToken());
        assertNotNull(tokenDto.getRefreshToken());
    }

    @Test
    @DisplayName("유효한 토큰의 유효성을 검증")
    void validateToken() {
        // given
        String token = Jwts.builder()
                .setSubject("test@example.com")
                .setExpiration(new Date(System.currentTimeMillis() + 60000)) // 1분
                .signWith(key)
                .compact();

        // when
        boolean isValid = jwtTokenProvider.validateToken(token);

        // then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("만료된 토큰은 예외를 발생")
    void validateToken_ShouldThrowExceptionForExpiredToken() {
        // given
        String token = Jwts.builder()
                .setSubject("test@example.com")
                .setExpiration(new Date(System.currentTimeMillis() - 60000)) // 1분 전 만료
                .signWith(key)
                .compact();

        // when & then
        TokenException exception = assertThrows(TokenException.class, () -> jwtTokenProvider.validateToken(token));
        assertEquals(TokenExceptionType.TOKEN_EXPIRED, exception.getExceptionType());
    }

    @Test
    @DisplayName("토큰에서 인증 정보를 올바르게 추출")
    void getAuthentication_ShouldReturnAuthenticationFromToken() {
        // given
        Member member = Member.builder()
                .id(1L)
                .email("test@example.com")
                .password("password")
                .role(Role.USER)
                .build();
        PrincipalDetails principalDetails = new PrincipalDetails(member);
        String token = jwtTokenProvider.generateTokenDto(principalDetails).getAccessToken();

        // when
        Claims claims = jwtTokenProvider.parseClaims(token);
        assertEquals("test@example.com", claims.getSubject());

        // getAuthentication 검증
        var auth = jwtTokenProvider.getAuthentication(token);
        assertEquals(principalDetails.getUsername(), auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(granted -> granted.getAuthority().equals("ROLE_USER")));
    }
}
