package cloud.zipbob.edgeservice.domain.member.service;

import cloud.zipbob.edgeservice.auth.jwt.JwtTokenProperties;
import cloud.zipbob.edgeservice.auth.jwt.JwtTokenProvider;
import cloud.zipbob.edgeservice.domain.member.Member;
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
import cloud.zipbob.edgeservice.global.redis.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
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
}
