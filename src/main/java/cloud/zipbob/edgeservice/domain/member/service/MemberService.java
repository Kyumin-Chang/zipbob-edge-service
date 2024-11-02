package cloud.zipbob.edgeservice.domain.member.service;

import cloud.zipbob.edgeservice.domain.member.request.MemberUpdateRequest;
import cloud.zipbob.edgeservice.domain.member.request.MemberWithdrawRequest;
import cloud.zipbob.edgeservice.domain.member.request.OAuth2JoinRequest;
import cloud.zipbob.edgeservice.domain.member.response.MemberUpdateResponse;
import cloud.zipbob.edgeservice.domain.member.response.MemberWithdrawResponse;
import cloud.zipbob.edgeservice.domain.member.response.MyInfoResponse;
import cloud.zipbob.edgeservice.domain.member.response.OAuth2JoinResponse;

public interface MemberService {
    MemberUpdateResponse update(MemberUpdateRequest request, String email);

    OAuth2JoinResponse oauth2Join(OAuth2JoinRequest request, String email);

    MemberWithdrawResponse withdraw(MemberWithdrawRequest request, String email);

    MyInfoResponse myInfo(String email);

    boolean checkNickname(String nickname);
}
