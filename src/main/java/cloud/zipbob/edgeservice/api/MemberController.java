package cloud.zipbob.edgeservice.api;

import cloud.zipbob.edgeservice.auth.PrincipalDetails;
import cloud.zipbob.edgeservice.domain.member.request.MemberUpdateRequest;
import cloud.zipbob.edgeservice.domain.member.request.MemberWithdrawRequest;
import cloud.zipbob.edgeservice.domain.member.request.OAuth2JoinRequest;
import cloud.zipbob.edgeservice.domain.member.response.MemberUpdateResponse;
import cloud.zipbob.edgeservice.domain.member.response.MemberWithdrawResponse;
import cloud.zipbob.edgeservice.domain.member.response.MyInfoResponse;
import cloud.zipbob.edgeservice.domain.member.response.OAuth2JoinResponse;
import cloud.zipbob.edgeservice.domain.member.service.MemberService;
import cloud.zipbob.edgeservice.global.Responder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PatchMapping("/update")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<MemberUpdateResponse> update(@AuthenticationPrincipal PrincipalDetails user,
                                                       @RequestBody final MemberUpdateRequest request) {
        MemberUpdateResponse response = memberService.update(request, user.getUsername());
        return Responder.success(response);
    }

    @PatchMapping("/oauth2/join")
    @PreAuthorize("hasAnyAuthority('ROLE_GUEST')")
    public ResponseEntity<OAuth2JoinResponse> join(@AuthenticationPrincipal PrincipalDetails user, @RequestBody final OAuth2JoinRequest request) {
        OAuth2JoinResponse response = memberService.oauth2Join(request, user.getUsername());
        return Responder.success(response);
    }

    @DeleteMapping("/withdraw")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<MemberWithdrawResponse> withdraw(@AuthenticationPrincipal PrincipalDetails user,
                                                           @RequestBody final MemberWithdrawRequest request) {
        MemberWithdrawResponse response = memberService.withdraw(request, user.getUsername());
        return Responder.success(response);
    }

    @GetMapping("/myInfo")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<MyInfoResponse> myInfo(@AuthenticationPrincipal PrincipalDetails user) {
        MyInfoResponse response = memberService.myInfo(user.getUsername());
        return Responder.success(response);
    }

    @GetMapping("/nickname-check/{nickname}")
    public ResponseEntity<Boolean> checkNickname(@PathVariable("nickname") String nickname) {
        boolean result = memberService.checkNickname(nickname);
        return Responder.success(result);
    }
}
