package cloud.zipbob.edgeservice.domain.member.service;

import cloud.zipbob.edgeservice.auth.PrincipalDetails;
import cloud.zipbob.edgeservice.auth.dto.TokenDto;
import cloud.zipbob.edgeservice.auth.jwt.JwtTokenProperties;
import cloud.zipbob.edgeservice.auth.jwt.JwtTokenProvider;
import cloud.zipbob.edgeservice.domain.member.Member;
import cloud.zipbob.edgeservice.domain.member.Role;
import cloud.zipbob.edgeservice.domain.member.exception.MemberException;
import cloud.zipbob.edgeservice.domain.member.exception.MemberExceptionType;
import cloud.zipbob.edgeservice.domain.member.repository.MemberRepository;
import cloud.zipbob.edgeservice.domain.member.request.MemberUpdateRequest;
import cloud.zipbob.edgeservice.domain.member.request.MemberWithdrawRequest;
import cloud.zipbob.edgeservice.domain.member.request.OAuth2JoinRequest;
import cloud.zipbob.edgeservice.domain.member.response.*;
import cloud.zipbob.edgeservice.global.redis.RedisService;
import cloud.zipbob.edgeservice.oauth2.SocialType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;
    private final JwtTokenProperties jwtTokenProperties;

    @Override
    @Transactional
    @CacheEvict(value = "memberInfoCache", key = "#root.args[1] != null ? #root.args[1] : 'defaultKey'")
    public MemberUpdateResponse update(MemberUpdateRequest request, String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException(MemberExceptionType.MEMBER_NOT_FOUND));
        if (memberRepository.findByNickname(request.newNickname()).isPresent()) {
            throw new MemberException(MemberExceptionType.ALREADY_EXIST_NICKNAME);
        }
        member.updateNickname(request.newNickname());
        return MemberUpdateResponse.of(member);
    }

    @Override
    @Transactional
    public OAuth2JoinResponse oauth2Join(OAuth2JoinRequest request, String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException(MemberExceptionType.MEMBER_NOT_FOUND));
        if (memberRepository.findByNickname(request.nickname()).isPresent()) {
            throw new MemberException(MemberExceptionType.ALREADY_EXIST_NICKNAME);
        }
        member.updateNickname(request.nickname());
        member.authorizeUser();
        return OAuth2JoinResponse.of(member);
    }

    @Override
    @Transactional
    @CacheEvict(value = "memberInfoCache", key = "#root.args[1] != null ? #root.args[1] : 'defaultKey'")
    public MemberWithdrawResponse withdraw(MemberWithdrawRequest request, String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException(MemberExceptionType.MEMBER_NOT_FOUND));
        if (!member.getNickname().equals(request.nickname())) {
            throw new MemberException(MemberExceptionType.NOT_MATCH_NICKNAME);
        }
        memberRepository.delete(member);
        return MemberWithdrawResponse.of(email);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "memberInfoCache", key = "#root.args[0] != null ? #root.args[0] : 'defaultKey'")
    public MyInfoResponse getMyInfo(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException(MemberExceptionType.MEMBER_NOT_FOUND));
        return MyInfoResponse.of(member);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean checkNickname(String nickname) {
        return memberRepository.findByNickname(nickname).isPresent();
    }

    //TODO test 후 배포할 때 제거 필수
    @Override
    public TestJoinResponse testJoin(String email) {
        if (memberRepository.findByEmail(email).isPresent()) {
            throw new MemberException(MemberExceptionType.ALREADY_EXIST_EMAIL);
        }
        String password = "{bcrypt}$2a$10$N9qo8uLO2XxS5Tp25KXZy.sqzotZ9dhJdV32wBd4YwyvZ1CzzZ9cK";
        String nickname = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        Member member = Member.builder()
                .socialType(SocialType.KAKAO)
                .socialId("123456789")
                .email(email)
                .nickname(nickname)
                .password(password)
                .role(Role.USER)
                .build();
        memberRepository.save(member);
        PrincipalDetails principalDetails = new PrincipalDetails(member);
        TokenDto tokenDto = jwtTokenProvider.generateTokenDto(principalDetails);
        String accessToken = tokenDto.getAccessToken();
        String refreshToken = tokenDto.getRefreshToken();
        long refreshTokenExpirationPeriod = jwtTokenProperties.getRefreshExpiration();
        redisService.setValues(member.getEmail(), refreshToken,
                Duration.ofMillis(refreshTokenExpirationPeriod));
        return TestJoinResponse.of(member, accessToken, tokenDto.getRefreshToken());
    }
}
