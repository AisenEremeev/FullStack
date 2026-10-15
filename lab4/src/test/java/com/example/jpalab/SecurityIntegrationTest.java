package com.example.jpalab;

import java.time.Instant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.web.servlet.MockMvc;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:securitytest;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired CustomerRepository customers;
    @Autowired ObjectMapper mapper;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;
    private static final String CREDENTIALS = "{\"username\":\"Student\",\"password\":\"Test-password-123\"}";

    @BeforeEach void clean() { customers.deleteAll(); users.deleteAll(); }

    private void register() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.username").value("student"))
            .andExpect(jsonPath("$.password").doesNotExist())
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    private String login() throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(CREDENTIALS))
            .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(cookie().doesNotExist("JSESSIONID"))
            .andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).get("accessToken").asText();
    }

    @Test void registrationHashesPasswordAndLoginAuthenticates() throws Exception {
        register();
        String hash = users.findByUsername("student").orElseThrow().getPasswordHash();
        assertThat(hash).isNotEqualTo("Test-password-123");
        assertThat(passwords.matches("Test-password-123", hash)).isTrue();
        String token = login();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("student"));
        mvc.perform(get("/api/customers").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    @Test void rejectsMissingAndMalformedTokenOnEveryCrudOperation() throws Exception {
        mvc.perform(get("/api/customers")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/customers/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
            .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/customers/1").contentType(MediaType.APPLICATION_JSON)
            .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/customers/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/customers").header("Authorization", "Bearer garbage"))
            .andExpect(status().isUnauthorized());
        assertThat(customers.count()).isZero();
    }

    @Test void rejectsDuplicateRegistrationAndWrongPassword() throws Exception {
        register();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
            .content(CREDENTIALS.replace("Student", "STUDENT"))).andExpect(status().isConflict());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(CREDENTIALS.replace("Test-password-123", "Wrong-password-123")))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.accessToken").doesNotExist());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(CREDENTIALS.replace("Student", "unknown"))).andExpect(status().isUnauthorized());
        assertThat(users.count()).isEqualTo(1);
    }

    @Test void rejectsInvalidRegistration() throws Exception {
        for (String body : new String[]{"{}", CREDENTIALS.replace("Student", "a"),
                CREDENTIALS.replace("Test-password-123", "short"),
                mapper.writeValueAsString(new AuthController.Credentials("validuser", "я".repeat(40)))}) {
            mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        }
        assertThat(users.count()).isZero();
    }

    @Test void rejectsTamperedExpiredAndWrongIssuerTokens() throws Exception {
        register();
        String valid = login();
        String[] parts = valid.split("\\.");
        String signature = (parts[2].charAt(0) == 'A' ? "B" : "A") + parts[2].substring(1);
        String tampered = parts[0] + "." + parts[1] + "." + signature;
        Instant now = Instant.now();
        String expired = encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
            .issuer(SecurityConfig.ISSUER).subject("student").issuedAt(now.minusSeconds(120))
            .expiresAt(now.minusSeconds(60)).build())).getTokenValue();
        String wrongIssuer = encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
            .issuer("untrusted").subject("student").issuedAt(now)
            .expiresAt(now.plusSeconds(900)).build())).getTokenValue();
        for (String token : new String[]{tampered, expired, wrongIssuer}) {
            mvc.perform(get("/api/customers").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        }
    }

    @Test void deniesUnlistedPathsEvenAfterLogin() throws Exception {
        register();
        mvc.perform(get("/h2-console").header("Authorization", "Bearer " + login()))
            .andExpect(status().isForbidden());
    }
}
