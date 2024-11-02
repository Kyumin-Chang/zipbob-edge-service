package cloud.zipbob.edgeservice.domain.member.response;

import cloud.zipbob.edgeservice.domain.member.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberUpdateResponse {

    private String email;
    private String newNickname;

    public static MemberUpdateResponse of(Member member) {
        return new MemberUpdateResponse(member.getEmail(), member.getNickname());
    }
}
