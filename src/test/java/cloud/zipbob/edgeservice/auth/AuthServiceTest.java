package cloud.zipbob.edgeservice.auth;

import cloud.zipbob.edgeservice.auth.dto.TokenDto;
import cloud.zipbob.edgeservice.auth.exception.TokenException;
import cloud.zipbob.edgeservice.auth.exception.TokenExceptionType;
import cloud.zipbob.edgeservice.auth.jwt.JwtTokenProvider;
import cloud.zipbob.edgeservice.domain.member.Member;
import cloud.zipbob.edgeservice.domain.member.Role;
import cloud.zipbob.edgeservice.domain.member.repository.MemberRepository;
import cloud.zipbob.edgeservice.global.redis.RedisService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisService redisService;

    @Mock
    private MemberRepository memberRepository;

    private String email = "test@example.com";
    private String accessToken = "access-token";
    private String refreshToken = "refresh-token";
    private Member member;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        member = Member.builder()
                .email(email)
                .role(Role.USER)
                .password(passwordEncoder.encode("password"))
                .nickname("TestUser")
                .build();
    }

    @Test
    @DisplayName("Reissue Access Token 성공")
    void reissueAccessTokenSuccess() {
        // given
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(email);
        when(jwtTokenProvider.parseClaims(refreshToken)).thenReturn(claims);
        when(redisService.getValues(email)).thenReturn(refreshToken);
        when(redisService.checkExistsValue(refreshToken)).thenReturn(true);
        when(memberRepository.findByEmail(email)).thenReturn(Optional.of(member));
        TokenDto tokenDto = TokenDto.builder()
                .grantType("Bearer")
                .authorizationType("Authorization")
                .accessToken("newAccessToken")
                .refreshToken("newRefreshToken")
                .accessTokenExpiresIn(3600000L)
                .build();
        when(jwtTokenProvider.generateTokenDto(any())).thenReturn(tokenDto);

        // when
        String newAccessToken = authService.reissueAccessToken(refreshToken);

        // then
        assertEquals("newAccessToken", newAccessToken);
        verify(jwtTokenProvider).parseClaims(refreshToken);
        verify(redisService).getValues(email);
        verify(redisService).checkExistsValue(refreshToken);
        verify(memberRepository).findByEmail(email);
    }

    @Test
    @DisplayName("Reissue Access Token 실패 - 잘못된 토큰")
    void reissueAccessTokenFail_InvalidToken() {
        // given
        when(jwtTokenProvider.parseClaims(refreshToken)).thenThrow(new TokenException(TokenExceptionType.TOKEN_INVALID));

        // when & then
        assertThrows(TokenException.class, () -> authService.reissueAccessToken(refreshToken));
        verify(jwtTokenProvider).parseClaims(refreshToken);
    }

    @Test
    @DisplayName("로그아웃 성공")
    void logoutSuccess() {
        // given
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(email);
        when(jwtTokenProvider.parseClaims(refreshToken)).thenReturn(claims);
        when(redisService.getValues(email)).thenReturn(refreshToken);
        when(redisService.checkExistsValue(refreshToken)).thenReturn(true);
        long expirationMillis = 3600000L;
        when(jwtTokenProvider.getAccessTokenExpirationPeriod()).thenReturn(expirationMillis);

        // when
        authService.logout(refreshToken, accessToken);

        // then
        verify(redisService).deleteValues(email);
        verify(redisService).setValues(accessToken, "logout", Duration.ofMillis(expirationMillis));
    }

    @Test
    @DisplayName("로그아웃 실패 - 잘못된 토큰")
    void logoutFail_InvalidToken() {
        // given
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(email);
        when(jwtTokenProvider.parseClaims(refreshToken)).thenReturn(claims);
        when(redisService.getValues(email)).thenReturn(null);

        // when & then
        assertThrows(TokenException.class, () -> authService.logout(refreshToken, accessToken));
        verify(redisService, never()).deleteValues(email);
    }
}
