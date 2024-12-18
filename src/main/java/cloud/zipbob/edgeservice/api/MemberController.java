package cloud.zipbob.edgeservice.api;

import cloud.zipbob.edgeservice.auth.PrincipalDetails;
import cloud.zipbob.edgeservice.domain.member.request.MemberUpdateRequest;
import cloud.zipbob.edgeservice.domain.member.request.MemberWithdrawRequest;
import cloud.zipbob.edgeservice.domain.member.request.OAuth2JoinRequest;
import cloud.zipbob.edgeservice.domain.member.request.TestJoinRequest;
import cloud.zipbob.edgeservice.domain.member.response.MemberUpdateResponse;
import cloud.zipbob.edgeservice.domain.member.response.MemberWithdrawResponse;
import cloud.zipbob.edgeservice.domain.member.response.MyInfoResponse;
import cloud.zipbob.edgeservice.domain.member.response.OAuth2JoinResponse;
import cloud.zipbob.edgeservice.domain.member.response.TestJoinResponse;
import cloud.zipbob.edgeservice.domain.member.service.MemberService;
import cloud.zipbob.edgeservice.global.Responder;
import cloud.zipbob.edgeservice.global.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final EmailService emailService;

    @PatchMapping("/update")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<MemberUpdateResponse> update(@AuthenticationPrincipal PrincipalDetails user,
                                                       @RequestBody final MemberUpdateRequest request) {
        MemberUpdateResponse response = memberService.update(request, user.getUsername());
        return Responder.success(response);
    }

    @PatchMapping("/oauth2/join")
    @PreAuthorize("hasAnyAuthority('ROLE_GUEST')")
    public ResponseEntity<OAuth2JoinResponse> join(@AuthenticationPrincipal PrincipalDetails user,
                                                   @RequestBody final OAuth2JoinRequest request) {
        OAuth2JoinResponse response = memberService.oauth2Join(request, user.getUsername());
        emailService.sendEmailRequest(user.getUsername(), request.nickname(), "welcome");
        return Responder.success(response);
    }

    @DeleteMapping("/withdraw")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<MemberWithdrawResponse> withdraw(@AuthenticationPrincipal PrincipalDetails user,
                                                           @RequestBody final MemberWithdrawRequest request) {
        MemberWithdrawResponse response = memberService.withdraw(request, user.getUsername());
        emailService.sendEmailRequest(user.getUsername(), request.nickname(), "goodbye");
        return Responder.success(response);
    }

    @GetMapping("/myInfo")
    @PreAuthorize("hasAnyAuthority('ROLE_USER', 'ROLE_ADMIN')")
    public ResponseEntity<MyInfoResponse> myInfo(@AuthenticationPrincipal PrincipalDetails user) {
        MyInfoResponse response = memberService.getMyInfo(user.getUsername());
        return Responder.success(response);
    }

    @GetMapping("/nickname-check/{nickname}")
    public ResponseEntity<Boolean> checkNickname(@PathVariable("nickname") String nickname) {
        boolean result = memberService.checkNickname(nickname);
        return Responder.success(result);
    }

    //TODO test 후 배포할 때 제거 필수 (url 필터 제외도 제거)
    @PostMapping("/test/join")
    public ResponseEntity<TestJoinResponse> testJoin(@RequestBody final TestJoinRequest request) {
        TestJoinResponse response = memberService.testJoin(request.email());
        return Responder.success(response);
    }

}
