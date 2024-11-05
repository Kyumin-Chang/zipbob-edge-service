package cloud.zipbob.edgeservice.domain.member.request;

import jakarta.validation.constraints.Size;

public record OAuth2JoinRequest(@Size(min = 2, message = "닉네임이 너무 짧습니다. 최소 2글자이어야 합니다.") String nickname) {
}
