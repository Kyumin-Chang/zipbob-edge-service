package cloud.zipbob.edgeservice.oauth2.userinfo;

import java.util.Map;

public class GoogleOAuth2MemberInfo extends OAuth2MemberInfo {
    public GoogleOAuth2MemberInfo(Map<String, Object> attributes) {
        super(attributes);
    }

    @Override
    public String getId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }
}
