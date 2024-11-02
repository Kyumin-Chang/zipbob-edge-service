package cloud.zipbob.edgeservice.domain.member.service;

import cloud.zipbob.edgeservice.domain.member.Member;
import cloud.zipbob.edgeservice.domain.member.Role;
import cloud.zipbob.edgeservice.domain.member.exception.MemberException;
import cloud.zipbob.edgeservice.domain.member.exception.MemberExceptionType;
import cloud.zipbob.edgeservice.domain.member.repository.MemberRepository;
import cloud.zipbob.edgeservice.domain.member.request.MemberUpdateRequest;
import cloud.zipbob.edgeservice.domain.member.request.MemberWithdrawRequest;
import cloud.zipbob.edgeservice.domain.member.request.OAuth2JoinRequest;
import cloud.zipbob.edgeservice.domain.member.response.MemberUpdateResponse;
import cloud.zipbob.edgeservice.domain.member.response.MemberWithdrawResponse;
import cloud.zipbob.edgeservice.domain.member.response.MyInfoResponse;
import cloud.zipbob.edgeservice.domain.member.response.OAuth2JoinResponse;
import cloud.zipbob.edgeservice.oauth2.SocialType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberServiceImpl memberService;

    private Member member;

    @BeforeEach
    void setUp() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        member = Member.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("password"))
                .nickname("TestNickname")
                .role(Role.USER)
                .socialType(SocialType.GOOGLE)
                .socialId(null)
                .build();
    }

    @Test
    @DisplayName("닉네임이 사용 가능한 경우 업데이트된 멤버를 반환")
    void update_ShouldReturnUpdatedMember_WhenNicknameIsAvailable() {
        // given
        String email = "test@example.com";
        MemberUpdateRequest request = new MemberUpdateRequest("NewNickname");

        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.of(member));
        when(memberRepository.findByNickname(anyString())).thenReturn(Optional.empty());

        // when
        MemberUpdateResponse response = memberService.update(request, email);

        // then
        assertEquals("NewNickname", response.getNewNickname());
        verify(memberRepository, times(1)).findByEmail(anyString());
        verify(memberRepository, times(1)).findByNickname(anyString());
    }

    @Test
    @DisplayName("닉네임이 이미 존재할 경우 예외를 발생")
    void update_ShouldThrowException_WhenNicknameAlreadyExists() {
        // given
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String email = "test@example.com";
        MemberUpdateRequest request = new MemberUpdateRequest("ExistingNickname");

        Member existingMember = Member.builder()
                .email("existing@example.com")
                .password(passwordEncoder.encode("password"))
                .nickname("ExistingNickname")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .build();

        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.of(member));
        when(memberRepository.findByNickname(anyString())).thenReturn(Optional.of(existingMember));

        // when & then
        MemberException exception = assertThrows(MemberException.class,
                () -> memberService.update(request, email));

        assertEquals(MemberExceptionType.ALREADY_EXIST_NICKNAME, exception.getExceptionType());
        verify(memberRepository, times(1)).findByEmail(anyString());
        verify(memberRepository, times(1)).findByNickname(anyString());
    }

    @Test
    @DisplayName("닉네임이 일치할 때 회원을 삭제 가능")
    void withdraw_ShouldDeleteMember_WhenNicknameMatches() {
        // given
        String email = "test@example.com";
        MemberWithdrawRequest request = new MemberWithdrawRequest("TestNickname");

        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.of(member));

        // when
        MemberWithdrawResponse response = memberService.withdraw(request, email);

        // then
        assertEquals(email, response.getEmail());
        verify(memberRepository, times(1)).findByEmail(anyString());
        verify(memberRepository, times(1)).delete(member);
    }

    @Test
    @DisplayName("닉네임이 일치하지 않으면 예외를 발생")
    void withdraw_ShouldThrowException_WhenNicknameDoesNotMatch() {
        // given
        String email = "test@example.com";
        MemberWithdrawRequest request = new MemberWithdrawRequest("WrongNickname");

        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.of(member));

        // when & then
        MemberException exception = assertThrows(MemberException.class,
                () -> memberService.withdraw(request, email));

        assertEquals(MemberExceptionType.NOT_MATCH_NICKNAME, exception.getExceptionType());
        verify(memberRepository, times(1)).findByEmail(anyString());
        verify(memberRepository, times(0)).delete(member);
    }

    @Test
    @DisplayName("회원이 존재할 경우 회원 정보를 반환해야 한다")
    void myInfo_ShouldReturnMemberInfo_WhenMemberExists() {
        // given
        String email = "test@example.com";

        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.of(member));

        // when
        MyInfoResponse response = memberService.myInfo(email);

        // then
        assertEquals(member.getEmail(), response.getEmail());
        assertEquals(member.getNickname(), response.getNickname());
        verify(memberRepository, times(1)).findByEmail(anyString());
    }

    @Test
    @DisplayName("회원이 존재하지 않으면 예외를 발생")
    void myInfo_ShouldThrowException_WhenMemberNotFound() {
        // given
        String email = "nonexistent@example.com";

        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // when & then
        MemberException exception = assertThrows(MemberException.class,
                () -> memberService.myInfo(email));

        assertEquals(MemberExceptionType.MEMBER_NOT_FOUND, exception.getExceptionType());
        verify(memberRepository, times(1)).findByEmail(anyString());
    }

    @Test
    @DisplayName("닉네임이 존재할 경우 true를 반환")
    void checkNickname_ShouldReturnTrue_WhenNicknameExists() {
        // given
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String nickname = "ExistingNickname";
        Member existingMember = Member.builder()
                .email("existing@example.com")
                .password(passwordEncoder.encode("password"))
                .nickname(nickname)
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .build();

        when(memberRepository.findByNickname(anyString())).thenReturn(Optional.of(existingMember));

        // when
        boolean result = memberService.checkNickname(nickname);

        // then
        assertTrue(result);
        verify(memberRepository, times(1)).findByNickname(anyString());
    }

    @Test
    @DisplayName("닉네임이 존재하지 않으면 false를 반환")
    void checkNickname_ShouldReturnFalse_WhenNicknameDoesNotExist() {
        // given
        String nickname = "NonExistentNickname";

        when(memberRepository.findByNickname(anyString())).thenReturn(Optional.empty());

        // when
        boolean result = memberService.checkNickname(nickname);

        // then
        assertFalse(result);
        verify(memberRepository, times(1)).findByNickname(anyString());
    }

    @Test
    @DisplayName("oauth2join 호출 시 멤버의 역할이 GUEST에서 USER로 변경되는지 확인")
    void oauth2Join_ShouldChangeRoleFromGuestToUser_WhenCalled() {
        // given
        String email = "test@example.com";
        Member guestMember = Member.builder()
                .email(email)
                .nickname(null) // Assuming nickname is null initially
                .role(Role.GUEST)
                .socialType(SocialType.GOOGLE)
                .build();
        OAuth2JoinRequest request = new OAuth2JoinRequest("NewNickname");

        when(memberRepository.findByEmail(anyString())).thenReturn(Optional.of(guestMember));
        when(memberRepository.findByNickname(anyString())).thenReturn(Optional.empty());

        // when
        OAuth2JoinResponse response = memberService.oauth2Join(request, email);

        // then
        assertEquals("NewNickname", guestMember.getNickname());
        assertEquals(Role.USER, guestMember.getRole());
        assertEquals(Role.USER, response.getRole());
        verify(memberRepository, times(1)).findByEmail(anyString());
        verify(memberRepository, times(1)).findByNickname(anyString());
    }
}
