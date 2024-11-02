package cloud.zipbob.edgeservice.oauth2;

import cloud.zipbob.edgeservice.domain.member.Member;
import cloud.zipbob.edgeservice.domain.member.Role;
import cloud.zipbob.edgeservice.oauth2.userinfo.GoogleOAuth2MemberInfo;
import cloud.zipbob.edgeservice.oauth2.userinfo.KakaoOAuth2MemberInfo;
import cloud.zipbob.edgeservice.oauth2.userinfo.NaverOAuth2MemberInfo;
import cloud.zipbob.edgeservice.oauth2.userinfo.OAuth2MemberInfo;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
public class OAuth2Attributes {
    private final String nameAttributeKey;
    private final OAuth2MemberInfo oAuth2MemberInfo;

    @Builder
    private OAuth2Attributes(String nameAttributeKey, OAuth2MemberInfo oAuth2MemberInfo) {
        this.nameAttributeKey = nameAttributeKey;
        this.oAuth2MemberInfo = oAuth2MemberInfo;
    }

    public static OAuth2Attributes of(SocialType socialType, String userNameAttributeName,
                                      Map<String, Object> attributes) {
        if (socialType == SocialType.NAVER) {
            return ofNaver(userNameAttributeName, attributes);
        }
        if (socialType == SocialType.KAKAO) {
            return ofKakao(userNameAttributeName, attributes);
        }
        return ofGoogle(userNameAttributeName, attributes);
    }

    private static OAuth2Attributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuth2Attributes.builder()
                .nameAttributeKey(userNameAttributeName)
                .oAuth2MemberInfo(new NaverOAuth2MemberInfo(attributes))
                .build();
    }

    private static OAuth2Attributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuth2Attributes.builder()
                .nameAttributeKey(userNameAttributeName)
                .oAuth2MemberInfo(new KakaoOAuth2MemberInfo(attributes))
                .build();
    }

    private static OAuth2Attributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuth2Attributes.builder()
                .nameAttributeKey(userNameAttributeName)
                .oAuth2MemberInfo(new GoogleOAuth2MemberInfo(attributes))
                .build();
    }

    public Member toEntity(SocialType socialType, OAuth2MemberInfo oAuth2MemberInfo) {
        return Member.builder()
                .socialType(socialType)
                .socialId(oAuth2MemberInfo.getId())
                .email(oAuth2MemberInfo.getEmail())
                .role(Role.GUEST)
                .build();
    }
}
