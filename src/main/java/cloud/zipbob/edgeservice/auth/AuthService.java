package cloud.zipbob.edgeservice.auth;

import cloud.zipbob.edgeservice.auth.dto.TokenDto;
import cloud.zipbob.edgeservice.auth.exception.TokenException;
import cloud.zipbob.edgeservice.auth.exception.TokenExceptionType;
import cloud.zipbob.edgeservice.auth.jwt.JwtTokenProperties;
import cloud.zipbob.edgeservice.auth.jwt.JwtTokenProvider;
import cloud.zipbob.edgeservice.domain.member.Member;
import cloud.zipbob.edgeservice.domain.member.exception.MemberException;
import cloud.zipbob.edgeservice.domain.member.exception.MemberExceptionType;
import cloud.zipbob.edgeservice.domain.member.repository.MemberRepository;
import cloud.zipbob.edgeservice.global.redis.RedisService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtTokenProperties jwtTokenProperties;
    private final RedisService redisService;
    private final MemberRepository memberRepository;

    public String reissueAccessToken(String refreshToken) {
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        String email = claims.getSubject();
        String redisRefreshToken = redisService.getValues(email);

        if (redisService.checkExistsValue(redisRefreshToken) && refreshToken.equals(redisRefreshToken)) {
            Member findMember = this.findMemberByEmail(email);
            PrincipalDetails principalDetails = PrincipalDetails.of(findMember);
            TokenDto tokenDto = jwtTokenProvider.generateTokenDto(principalDetails);
            return tokenDto.getAccessToken();
        } else {
            log.debug("ReissueAccessToken exception occur redisRefreshToken: {}", redisRefreshToken);
            throw new TokenException(TokenExceptionType.TOKEN_INVALID);
        }
    }

    public void logout(String refreshToken, String accessToken) {
        Claims claims = jwtTokenProvider.parseClaims(refreshToken);
        String email = claims.getSubject();
        String redisRefreshToken = redisService.getValues(email);
        if (redisService.checkExistsValue(redisRefreshToken)) {
            redisService.deleteValues(email);
            long accessTokenExpirationMillis = jwtTokenProperties.getAccessExpiration();
            redisService.setValues(accessToken, "logout", Duration.ofMillis(accessTokenExpirationMillis));
        } else {
            throw new TokenException(TokenExceptionType.TOKEN_INVALID);
        }
    }

    private Member findMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException(MemberExceptionType.MEMBER_NOT_FOUND));
    }
}
