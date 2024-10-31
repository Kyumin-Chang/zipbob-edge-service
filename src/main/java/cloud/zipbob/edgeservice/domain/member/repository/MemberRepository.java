package cloud.zipbob.edgeservice.domain.member.repository;

import cloud.zipbob.edgeservice.domain.member.Member;
import cloud.zipbob.edgeservice.oauth2.SocialType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);

    Optional<Member> findByNickname(String nickname);

    Optional<Member> findBySocialTypeAndSocialId(SocialType socialType, String socialId);
}
