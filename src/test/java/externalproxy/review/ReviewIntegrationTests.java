package externalproxy.review;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import externalproxy.support.AdvisoryLockService;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(ReviewIntegrationTests.NoopLockConfig.class)
class ReviewIntegrationTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanup() {
        jdbcTemplate.execute("delete from review_like");
        jdbcTemplate.execute("delete from review");
        jdbcTemplate.execute("delete from admin");
    }

    @Test
    void thirdReviewFromSameIpIsRejected() throws Exception {
        String body = """
                {"rating":5,"comment":"ok","username":"u","email":"u@example.com"}
                """;

        mockMvc.perform(post("/api/reviews")
                        .header("X-Forwarded-For", "1.2.3.4")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reviews")
                        .header("X-Forwarded-For", "1.2.3.4")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/reviews")
                        .header("X-Forwarded-For", "1.2.3.4")
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_REVIEWS"));
    }

    @Test
    void secondLikeFromSameIpIsRejected() throws Exception {
        String create = """
                {"rating":5,"comment":"ok","username":"u","email":"u@example.com"}
                """;

        String respBody = mockMvc.perform(post("/api/reviews")
                        .header("X-Forwarded-For", "5.6.7.8")
                        .contentType(APPLICATION_JSON)
                        .content(create))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(respBody);
        String id = json.get("id").asText();

        String token = createAdminAndLogin();
        mockMvc.perform(post("/api/admin/reviews/" + id + "/approve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/reviews/" + id + "/like")
                        .header("X-Forwarded-For", "9.9.9.9"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].likeCount").value(1));

        mockMvc.perform(post("/api/reviews/" + id + "/like")
                        .header("X-Forwarded-For", "9.9.9.9"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_LIKED"));
    }

    @Test
    void averageRatingIsComputed() throws Exception {
        mockMvc.perform(get("/api/reviews/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0))
                .andExpect(jsonPath("$.averageRating").value(closeTo(0.0, 0.0001)))
                .andExpect(jsonPath("$.ratingCounts.5").value(0))
                .andExpect(jsonPath("$.ratingCounts.4").value(0))
                .andExpect(jsonPath("$.ratingCounts.3").value(0))
                .andExpect(jsonPath("$.ratingCounts.2").value(0))
                .andExpect(jsonPath("$.ratingCounts.1").value(0));

        String respBody1 = mockMvc.perform(post("/api/reviews")
                        .header("X-Forwarded-For", "10.0.0.1")
                        .contentType(APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"ok\",\"username\":\"u1\",\"email\":\"u1@example.com\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id1 = objectMapper.readTree(respBody1).get("id").asText();

        String respBody2 = mockMvc.perform(post("/api/reviews")
                        .header("X-Forwarded-For", "10.0.0.2")
                        .contentType(APPLICATION_JSON)
                        .content("{\"rating\":1,\"comment\":\"ok\",\"username\":\"u2\",\"email\":\"u2@example.com\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String id2 = objectMapper.readTree(respBody2).get("id").asText();

        String token = createAdminAndLogin();
        mockMvc.perform(post("/api/admin/reviews/" + id1 + "/approve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/admin/reviews/" + id2 + "/approve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/reviews/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.averageRating").value(closeTo(3.0, 0.0001)))
                .andExpect(jsonPath("$.ratingCounts.5").value(1))
                .andExpect(jsonPath("$.ratingCounts.4").value(0))
                .andExpect(jsonPath("$.ratingCounts.3").value(0))
                .andExpect(jsonPath("$.ratingCounts.2").value(0))
                .andExpect(jsonPath("$.ratingCounts.1").value(1));
    }

    @Test
    void reviewIsNotVisibleOrLikeableUntilApproved() throws Exception {
        String respBody = mockMvc.perform(post("/api/reviews")
                        .header("X-Forwarded-For", "3.3.3.3")
                        .contentType(APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"ok\",\"username\":\"u\",\"email\":\"u@example.com\"}"))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = objectMapper.readTree(respBody).get("id").asText();

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(post("/api/reviews/" + id + "/like")
                        .header("X-Forwarded-For", "4.4.4.4"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REVIEW_NOT_APPROVED"));

        String token = createAdminAndLogin();
        mockMvc.perform(post("/api/admin/reviews/" + id + "/approve")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    private String createAdminAndLogin() throws Exception {
        String email = "admin@example.com";
        String rawPassword = "pass";
        String hash = passwordEncoder.encode(rawPassword);

        jdbcTemplate.update("""
                insert into admin (username, password, email, first_name, last_name, role)
                values (?, ?, ?, ?, ?, ?)
                """, "admin", hash, email, "a", "b", "ROLE_ADMIN");

        String loginBody = """
                {"email":"admin@example.com","password":"pass"}
                """;

        String loginResp = mockMvc.perform(post("/api/auth/signin")
                        .contentType(APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(loginResp).get("token").asText();
    }

    @TestConfiguration
    static class NoopLockConfig {
        @Bean
        @Primary
        AdvisoryLockService advisoryLockService() {
            return new AdvisoryLockService(null) {
                @Override
                public void lock(String ipHash) {
                    // no-op for H2-backed tests
                }
            };
        }
    }
}

