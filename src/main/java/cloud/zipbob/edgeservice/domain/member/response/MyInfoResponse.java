package cloud.zipbob.edgeservice.domain.member.response;

import cloud.zipbob.edgeservice.domain.member.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MyInfoResponse {
    private String email;
    private String nickname;

    public static MyInfoResponse of(Member member) {
        return new MyInfoResponse(member.getEmail(), member.getNickname());
    }
}
