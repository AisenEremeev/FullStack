package com.example.jpalab;

import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import static org.assertj.core.api.Assertions.assertThat;

// Starts real embedded Tomcat on a random port; no external server is needed.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:http-test;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class BackendHttpIT {
    @Autowired TestRestTemplate http;

    @Test void authenticatesAndRunsCrudOverRealHttp() {
        assertThat(http.getForEntity("/api/customers", String.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        var credentials = Map.of("username", "httpstudent", "password", "Http-test-password-123");
        assertThat(http.postForEntity("/api/auth/register", credentials, String.class).getStatusCode())
            .isEqualTo(HttpStatus.CREATED);
        var login = http.postForEntity("/api/auth/login", credentials, JsonNode.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(login.getBody().get("accessToken").asText());
        var create = http.exchange("/api/customers", HttpMethod.POST,
            new HttpEntity<>(new CustomerRequest("Ada", "Lovelace"), headers), JsonNode.class);
        assertThat(create.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String path = create.getHeaders().getLocation().toString();
        var read = http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(read.getBody().get("firstName").asText()).isEqualTo("Ada");
        assertThat(http.exchange(path, HttpMethod.PUT,
            new HttpEntity<>(new CustomerRequest("Grace", "Hopper"), headers), String.class).getStatusCode())
            .isEqualTo(HttpStatus.OK);
        var updated = http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);
        assertThat(updated.getBody().get("lastName").asText()).isEqualTo("Hopper");
        assertThat(http.exchange(path, HttpMethod.DELETE, new HttpEntity<>(headers), String.class).getStatusCode())
            .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(http.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class).getStatusCode())
            .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
