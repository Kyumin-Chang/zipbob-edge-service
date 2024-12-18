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
import cloud.zipbob.edgeservice.domain.member.response.MemberUpdateResponse;
import cloud.zipbob.edgeservice.domain.member.response.MemberWithdrawResponse;
import cloud.zipbob.edgeservice.domain.member.response.MyInfoResponse;
import cloud.zipbob.edgeservice.domain.member.response.OAuth2JoinResponse;
import cloud.zipbob.edgeservice.domain.member.response.TestJoinResponse;
import cloud.zipbob.edgeservice.global.redis.RedisService;
import cloud.zipbob.edgeservice.oauth2.SocialType;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
// TODO 관리자 전용 api 제작하기 (멤버 삭제)
public class MemberServiceImpl implements MemberService {
    private final MemberRepository memberRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;
    private final JwtTokenProperties jwtTokenProperties;

    @Override
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
    public MyInfoResponse myInfo(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new MemberException(MemberExceptionType.MEMBER_NOT_FOUND));
        return MyInfoResponse.of(member);
    }

    @Override
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
        Member member = Member.builder()
                .socialType(SocialType.KAKAO)
                .socialId("123456789")
                .email(email)
                .password(password)
                .role(Role.GUEST)
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
