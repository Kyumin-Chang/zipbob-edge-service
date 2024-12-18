package cloud.zipbob.edgeservice.domain.member.service;

import cloud.zipbob.edgeservice.domain.member.request.MemberUpdateRequest;
import cloud.zipbob.edgeservice.domain.member.request.MemberWithdrawRequest;
import cloud.zipbob.edgeservice.domain.member.request.OAuth2JoinRequest;
import cloud.zipbob.edgeservice.domain.member.response.MemberUpdateResponse;
import cloud.zipbob.edgeservice.domain.member.response.MemberWithdrawResponse;
import cloud.zipbob.edgeservice.domain.member.response.MyInfoResponse;
import cloud.zipbob.edgeservice.domain.member.response.OAuth2JoinResponse;
import cloud.zipbob.edgeservice.domain.member.response.TestJoinResponse;

public interface MemberService {
    MemberUpdateResponse update(MemberUpdateRequest request, String email);

    OAuth2JoinResponse oauth2Join(OAuth2JoinRequest request, String email);

    MemberWithdrawResponse withdraw(MemberWithdrawRequest request, String email);

    MyInfoResponse getMyInfo(String email);

    boolean checkNickname(String nickname);

    //TODO test 후 배포할 때 제거 필수
    TestJoinResponse testJoin(String email);
}
