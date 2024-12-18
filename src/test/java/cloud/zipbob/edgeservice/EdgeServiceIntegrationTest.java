package cloud.zipbob.edgeservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import cloud.zipbob.edgeservice.auth.PrincipalDetails;
import cloud.zipbob.edgeservice.domain.member.Member;
import cloud.zipbob.edgeservice.domain.member.Role;
import cloud.zipbob.edgeservice.domain.member.repository.MemberRepository;
import cloud.zipbob.edgeservice.global.email.EmailService;
import cloud.zipbob.edgeservice.global.redis.RedisService;
import cloud.zipbob.edgeservice.oauth2.SocialType;
import cloud.zipbob.edgeservice.oauth2.handler.OAuth2LoginSuccessHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles("test")
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EdgeServiceIntegrationTest {
    @Container
    static final MariaDBContainer<?> mariaDBContainer = new MariaDBContainer<>("mariadb:latest")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass");

    @Container
    static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:latest")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureTestContainers(DynamicPropertyRegistry registry) {
        if (!mariaDBContainer.isRunning()) {
            mariaDBContainer.start();
        }
        if (!redisContainer.isRunning()) {
            redisContainer.start();
        }
        registry.add("spring.datasource.url", mariaDBContainer::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDBContainer::getUsername);
        registry.add("spring.datasource.password", mariaDBContainer::getPassword);
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private RedisService redisService;

    @Autowired
    private OAuth2LoginSuccessHandler successHandler;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private EmailService emailService;

    private String accessToken;
    private String refreshToken;
    private Member testMember;

    @BeforeEach
    public void setupMock() {
        doNothing().when(emailService).sendEmail(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @BeforeAll
    public void setup() throws Exception {
        // given
        testMember = Member.builder()
                .email("test@example.com")
                .password(passwordEncoder.encode("test1234!@#"))
                .socialType(SocialType.GOOGLE)
                .socialId("1234")
                .role(Role.GUEST)
                .build();
        memberRepository.save(testMember);

        Authentication authentication = mock(Authentication.class);
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("sub", "1234567890");
        attributes.put("email", "test@example.com");
        PrincipalDetails principalDetails = new PrincipalDetails(testMember, attributes);
        when(authentication.getPrincipal()).thenReturn(principalDetails);

        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        successHandler.onAuthenticationSuccess(mock(HttpServletRequest.class), response, authentication);

        // then
        String authorizationHeader = response.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            this.accessToken = authorizationHeader.substring(7);
        }
        this.refreshToken = response.getHeader("Refresh");

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(response.getContentAsString());
        assertThat(jsonNode.get("message").asText()).isEqualTo("New Member OAuth Login Success");

        // given
        mockMvc.perform(patch("/members/oauth2/join")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\": \"newuser\"}"))
                .andExpect(status().isOk());

        // then
        Member updatedMember = memberRepository.findByEmail("test@example.com").orElseThrow();
        assertThat(updatedMember.getRole()).isEqualTo(Role.USER);

        MockHttpServletResponse response2 = mockMvc.perform(patch("/auth/reissue")
                        .header("Refresh", refreshToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        String newAccessToken = Objects.requireNonNull(response2.getHeader("Authorization")).substring(7);
        assertThat(newAccessToken).isNotNull();
        this.accessToken = newAccessToken;
    }

    @AfterAll
    public void cleanup() {
        memberRepository.delete(testMember);
        redisService.deleteValues("test@example.com");
    }

    @Test
    public void testUpdateNickname() throws Exception {
        // when
        MockHttpServletResponse response = mockMvc.perform(patch("/members/update")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newNickname\": \"updatedUser2\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("application/json");

        String responseBody = response.getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        assertThat(jsonNode.get("email").asText()).isEqualTo("test@example.com");

        Member updatedMember = memberRepository.findByEmail("test@example.com").orElseThrow();
        assertThat(updatedMember.getNickname()).isEqualTo("updatedUser2");
    }

    @Test
    public void testMyInfoRetrieval() throws Exception {
        // when
        MockHttpServletResponse response = mockMvc.perform(get("/members/myInfo")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentType()).isEqualTo("application/json");

        String responseBody = response.getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        assertThat(jsonNode.get("email").asText()).isEqualTo("test@example.com");
        assertThat(jsonNode.get("nickname").asText()).isEqualTo("newuser");
    }

    @Test
    public void testNicknameCheck() throws Exception {
        // when
        MockHttpServletResponse response = mockMvc.perform(get("/members/nickname-check/namecheckUser"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse();

        // then
        String responseBody = response.getContentAsString();
        assertThat(responseBody).isEqualTo("false");
    }
}
