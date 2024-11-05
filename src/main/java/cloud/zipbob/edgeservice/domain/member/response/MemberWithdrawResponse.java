package cloud.zipbob.edgeservice.domain.member.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberWithdrawResponse {

    private String email;

    public static MemberWithdrawResponse of(String email) {
        return new MemberWithdrawResponse(email);
    }
}
