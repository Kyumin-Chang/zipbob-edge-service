package cloud.zipbob.edgeservice.oauth2.handler;

import cloud.zipbob.edgeservice.auth.PrincipalDetails;
import cloud.zipbob.edgeservice.auth.dto.TokenDto;
import cloud.zipbob.edgeservice.auth.jwt.JwtTokenProperties;
import cloud.zipbob.edgeservice.auth.jwt.JwtTokenProvider;
import cloud.zipbob.edgeservice.domain.member.Member;
import cloud.zipbob.edgeservice.domain.member.Role;
import cloud.zipbob.edgeservice.domain.member.exception.MemberException;
import cloud.zipbob.edgeservice.domain.member.exception.MemberExceptionType;
import cloud.zipbob.edgeservice.domain.member.repository.MemberRepository;
import cloud.zipbob.edgeservice.global.redis.RedisService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler implements
        AuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenProperties jwtTokenProperties;
    private final MemberRepository memberRepository;
    private final RedisService redisService;

    private static final String FRONTEND_SERVER = "https://localhost:5173";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        log.info("OAuth2 Login successful");
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        String role = principalDetails.role();

        handleLogin(role, request, response, principalDetails);
    }

    private void handleLogin(String role, HttpServletRequest request, HttpServletResponse response,
                             PrincipalDetails principalDetails) throws IOException {
        TokenDto tokenDto = jwtTokenProvider.generateTokenDto(principalDetails);
        String accessToken = tokenDto.getAccessToken();
        String refreshToken = tokenDto.getRefreshToken();

        Member member = memberRepository.findByEmail(principalDetails.getUsername())
                .orElseThrow(() -> new MemberException(
                        MemberExceptionType.MEMBER_NOT_FOUND));

        jwtTokenProvider.setTokenCookie("accessToken", "Bearer " + accessToken, response);
        jwtTokenProvider.setTokenCookie("refreshToken", refreshToken, response);

        long refreshTokenExpirationPeriod = jwtTokenProperties.getRefreshExpiration();
        redisService.setValues(member.getEmail(), refreshToken, Duration.ofMillis(refreshTokenExpirationPeriod));

        String targetUrl;
        if (Objects.equals(role, Role.GUEST.getKey())) {
            log.info("OAuth Guest login successful");
            targetUrl = UriComponentsBuilder.fromUriString(FRONTEND_SERVER + "/test")
                    .queryParam("id", member.getId())
                    .queryParam("email", member.getEmail())
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();
        } else if (Objects.equals(role, Role.USER.getKey())) {
            log.info("OAuth User login successful");
            targetUrl = UriComponentsBuilder.fromUriString(FRONTEND_SERVER + "/home")
                    .queryParam("memberId", member.getId())
                    .queryParam("email", member.getEmail())
                    .queryParam("nickname", member.getNickname())
                    .build()
                    .encode(StandardCharsets.UTF_8)
                    .toUriString();
        } else {
            throw new MemberException(MemberExceptionType.WRONG_ROLE);
        }
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
