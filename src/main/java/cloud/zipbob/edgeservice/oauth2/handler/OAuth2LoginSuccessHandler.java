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
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenProperties jwtTokenProperties;
    private final MemberRepository memberRepository;
    private final RedisService redisService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        log.info("OAuth2 Login successful");
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        String role = principalDetails.role();

        handleLogin(role, response, principalDetails);
    }

    private void handleLogin(String role, HttpServletResponse response,
                             PrincipalDetails principalDetails) throws IOException {
        TokenDto tokenDto = jwtTokenProvider.generateTokenDto(principalDetails);
        String accessToken = tokenDto.getAccessToken();
        String refreshToken = tokenDto.getRefreshToken();

        Member member = memberRepository.findByEmail(principalDetails.getUsername())
                .orElseThrow(() -> new MemberException(
                        MemberExceptionType.MEMBER_NOT_FOUND));

        jwtTokenProvider.accessTokenSetHeader(accessToken, response);
        jwtTokenProvider.refreshTokenSetHeader(refreshToken, response);

        long refreshTokenExpirationPeriod = jwtTokenProperties.getRefreshExpiration();
        redisService.setValues(member.getEmail(), refreshToken, Duration.ofMillis(refreshTokenExpirationPeriod));

        Map<String, String> jsonResponse = new HashMap<>();
        if (Objects.equals(role, Role.GUEST.getKey())) {
            jsonResponse.put("message", "New Member OAuth Login Success");
            jsonResponse.put("email", member.getEmail());
        } else {
            jsonResponse.put("message", "Existing Member OAuth Login Success");
            jsonResponse.put("email", member.getEmail());
        }
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        new ObjectMapper().writeValue(response.getWriter(), jsonResponse);
    }
}
