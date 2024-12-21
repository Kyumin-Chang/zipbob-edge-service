package cloud.zipbob.edgeservice.oauth2.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler implements
        AuthenticationFailureHandler {

    private static final String FRONTEND_SERVER = "https://localhost:5173";

    @Override
    public void onAuthenticationFailure(final HttpServletRequest request, final HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        log.info("Fail Social Login: {}", exception.getMessage());
        log.info("Request URL: {}", request.getRequestURL());
        log.info("Query String: {}", request.getQueryString());
        log.info("User Agent: {}", request.getHeader("User-Agent"));
        log.error("Authentication failed due to: ", exception);

        String targetUrl = UriComponentsBuilder.fromUriString(FRONTEND_SERVER + "/error")
                .queryParam("error", "failedSocialLogin")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
