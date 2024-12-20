package cloud.zipbob.edgeservice.domain.member.response;

import cloud.zipbob.edgeservice.domain.member.Member;
import cloud.zipbob.edgeservice.domain.member.Role;
import cloud.zipbob.edgeservice.oauth2.SocialType;
import lombok.AllArgsConstructor;
import lombok.Getter;

//TODO test 후 배포할 때 제거 필수
@Getter
@AllArgsConstructor
public class TestJoinResponse {
    private Long id;
    private String email;
    private String nickname;
    private Role role;
    private SocialType socialType;
    private String accessToken;
    private String refreshToken;

    public static TestJoinResponse of(Member member, String accessToken, String refreshToken) {
        return new TestJoinResponse(member.getId(), member.getEmail(), member.getNickname(), member.getRole(),
                member.getSocialType(), accessToken, refreshToken);
    }
}
