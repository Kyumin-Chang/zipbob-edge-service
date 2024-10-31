package cloud.zipbob.edgeservice.oauth2;

import cloud.zipbob.edgeservice.auth.PrincipalDetails;
import cloud.zipbob.edgeservice.domain.member.Member;
import cloud.zipbob.edgeservice.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2MemberService extends DefaultOAuth2UserService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        log.info("OAuth2 Login Request Detect");
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialType socialType = getSocialType(registrationId);
        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint()
                .getUserNameAttributeName();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuth2Attributes extractAttributes = OAuth2Attributes.of(socialType, userNameAttributeName, attributes);
        Member createdMember = getMember(extractAttributes, socialType);

        return PrincipalDetails.of(createdMember, attributes);
    }

    private SocialType getSocialType(String registrationId) {
        return SocialType.valueOf(registrationId.toUpperCase());
    }

    private Member getMember(OAuth2Attributes attributes, SocialType socialType) {
        Member findMember = memberRepository.findBySocialTypeAndSocialId(socialType,
                attributes.getOAuth2MemberInfo().getId()).orElse(null);

        if (findMember == null) {
            return saveMember(attributes, socialType);
        }
        return findMember;
    }

    private Member saveMember(OAuth2Attributes oAuth2Attributes, SocialType socialType) {
        if (memberRepository.findByEmail(oAuth2Attributes.getOAuth2MemberInfo().getEmail()).isPresent()) {
            Member existingMember = memberRepository.findByEmail(oAuth2Attributes.getOAuth2MemberInfo().getEmail())
                    .get();
            SocialType existingSocialType = existingMember.getSocialType();
            throw new BadCredentialsException(
                    oAuth2Attributes.getOAuth2MemberInfo().getEmail() + "&socialType=" + existingSocialType);
        }
        Member createdMember = oAuth2Attributes.toEntity(socialType, oAuth2Attributes.getOAuth2MemberInfo());
        createdMember.updatePassword(passwordEncoder, getRandomPassword(10));
        return memberRepository.save(createdMember);
    }

    private static final String NUMBER_CHARACTERS = "0123456789";
    private static final String UPPERCASE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE_CHARACTERS = "abcdefghijklmnopqrstuvwxyz";
    private static final String SPECIAL_SYMBOL_CHARACTERS = "@$!%*?&";

    private static final String ALL_CHARACTERS = NUMBER_CHARACTERS + UPPERCASE_CHARACTERS + LOWERCASE_CHARACTERS + SPECIAL_SYMBOL_CHARACTERS;
    private static final SecureRandom random = new SecureRandom();

    public static String getRandomPassword(int length) {
        return Stream.of(
                        getRandomCharacter(NUMBER_CHARACTERS),
                        getRandomCharacter(UPPERCASE_CHARACTERS),
                        getRandomCharacter(LOWERCASE_CHARACTERS),
                        getRandomCharacter(SPECIAL_SYMBOL_CHARACTERS),
                        getRandomCharacters(length - 4)
                ).flatMap(Stream::of)
                .collect(Collectors.collectingAndThen(Collectors.toList(), collected -> {
                    Collections.shuffle(collected);
                    return collected.stream();
                }))
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

    private static String getRandomCharacter(String characters) {
        return String.valueOf(characters.charAt(random.nextInt(characters.length())));
    }

    private static String[] getRandomCharacters(int count) {
        return random.ints(count, 0, CustomOAuth2MemberService.ALL_CHARACTERS.length())
                .mapToObj(CustomOAuth2MemberService.ALL_CHARACTERS::charAt)
                .map(String::valueOf)
                .toArray(String[]::new);
    }
}
