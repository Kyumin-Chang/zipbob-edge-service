package cloud.zipbob.edgeservice.domain.member.response;

import cloud.zipbob.edgeservice.domain.member.Member;
import cloud.zipbob.edgeservice.domain.member.Role;
import cloud.zipbob.edgeservice.oauth2.SocialType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OAuth2JoinResponse {
    private String email;
    private String nickname;
    private Role role;
    private SocialType socialType;

    public static OAuth2JoinResponse of(Member member) {
        return new OAuth2JoinResponse(member.getEmail(), member.getNickname(), member.getRole(), member.getSocialType());
    }
}
