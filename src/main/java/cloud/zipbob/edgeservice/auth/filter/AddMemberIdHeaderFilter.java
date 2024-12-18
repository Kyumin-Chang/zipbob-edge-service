package cloud.zipbob.edgeservice.auth.filter;

import cloud.zipbob.edgeservice.auth.PrincipalDetails;
import cloud.zipbob.edgeservice.global.MutableHttpServletRequest;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Slf4j
public class AddMemberIdHeaderFilter implements Filter {

    private static final List<String> EXCLUDED_PATHS = List.of("/auth/reissue", "/members/nickname-check",
            "/members/test/join");

    @Override
    public void doFilter(jakarta.servlet.ServletRequest request,
                         jakarta.servlet.ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();
        if (shouldNotFilter(requestURI)) {
            log.info("Request URI excluded from AddMemberIdHeaderFilter: {}", requestURI);
            chain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
            Long memberId = principalDetails.getId();
            MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(httpRequest);
            mutableRequest.addHeader("X-Member-Id", String.valueOf(memberId));
            log.info("Member Id Header Set Complete :{}", memberId);
            chain.doFilter(mutableRequest, httpResponse);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean shouldNotFilter(String requestURI) {
        if ("/".equals(requestURI)) {
            return true;
        }
        return EXCLUDED_PATHS.stream().anyMatch(requestURI::startsWith);
    }
}
