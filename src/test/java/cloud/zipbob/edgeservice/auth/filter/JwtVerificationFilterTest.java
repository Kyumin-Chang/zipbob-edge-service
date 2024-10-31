package cloud.zipbob.edgeservice.auth.filter;

import cloud.zipbob.edgeservice.auth.jwt.JwtTokenProvider;
import cloud.zipbob.edgeservice.global.redis.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class JwtVerificationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private JwtVerificationFilter jwtVerificationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    private static final String TEST_TOKEN = "valid-access-token";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        request.addHeader("Authorization", "Bearer " + TEST_TOKEN);
    }

    @Test
    @DisplayName("유효한 토큰이 있는 경우 인증 설정")
    void doFilterInternal_ShouldSetAuthentication_WhenValidToken() throws ServletException, IOException {
        //given
        when(jwtTokenProvider.resolveAccessToken(request)).thenReturn(TEST_TOKEN);
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(true);
        when(redisService.getValues(TEST_TOKEN)).thenReturn("false");
        Authentication authentication = mock(Authentication.class);
        when(jwtTokenProvider.getAuthentication(TEST_TOKEN)).thenReturn(authentication);

        //when
        jwtVerificationFilter.doFilterInternal(request, response, filterChain);

        //then
        assertEquals(SecurityContextHolder.getContext().getAuthentication(), authentication);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("로그아웃 상태의 토큰에 대해 401 반환")
    void doFilterInternal_ShouldReturnUnauthorized_WhenLogoutToken() throws ServletException, IOException {
        //given
        when(jwtTokenProvider.resolveAccessToken(request)).thenReturn(TEST_TOKEN);
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenReturn(true);
        when(redisService.getValues(TEST_TOKEN)).thenReturn("true");

        //when
        jwtVerificationFilter.doFilterInternal(request, response, filterChain);

        //then
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }

    @Test
    @DisplayName("유효하지 않은 토큰일 때 예외 발생")
    void doFilterInternal_ShouldHandleException_WhenInvalidToken() throws ServletException, IOException {
        //given
        when(jwtTokenProvider.resolveAccessToken(request)).thenReturn(TEST_TOKEN);
        when(jwtTokenProvider.validateToken(TEST_TOKEN)).thenThrow(new RuntimeException("Invalid token"));

        //when
        jwtVerificationFilter.doFilterInternal(request, response, filterChain);

        //then
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }
}
