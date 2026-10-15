package com.example.jpalab;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:labtest;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class CustomerApiIT {
    @Autowired MockMvc mvc;
    @Autowired CustomerRepository repository;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository users;
    private String token;

    @BeforeEach void clean() throws Exception {
        repository.deleteAll();
        users.deleteAll();
        String credentials = "{\"username\":\"cruduser\",\"password\":\"Test-password-123\"}";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(credentials))
            .andExpect(status().isCreated());
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        token = mapper.readTree(body).get("accessToken").asText();
    }

    private org.springframework.test.web.servlet.ResultActions authorized(
            org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.header("Authorization", "Bearer " + token));
    }

    @Test void completeCrudCyclePersistsChanges() throws Exception {
        String body = authorized(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
            .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(body).get("id").asLong();
        assertThat(repository.findById(id)).isPresent();
        authorized(get("/api/customers/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Ada"));
        authorized(get("/api/customers")).andExpect(jsonPath("$.length()").value(1));
        authorized(get("/api/customers").param("lastName", "LOVELACE"))
            .andExpect(jsonPath("$[0].id").value(id));
        authorized(get("/api/customers").param("lastName", "Unknown"))
            .andExpect(jsonPath("$.length()").value(0));
        authorized(put("/api/customers/" + id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"firstName\":\"Grace\",\"lastName\":\"Hopper\"}"))
            .andExpect(status().isOk());
        assertThat(repository.findById(id).orElseThrow().getLastName()).isEqualTo("Hopper");
        authorized(delete("/api/customers/" + id)).andExpect(status().isNoContent());
        assertThat(repository.findById(id)).isEmpty();
        authorized(get("/api/customers/" + id)).andExpect(status().isNotFound());
    }

    @Test void rejectsInvalidInputWithoutWriting() throws Exception {
        for (String body : new String[]{"{}", "{\"firstName\":\"  \",\"lastName\":\"Smith\"}",
                mapper.writeValueAsString(new CustomerRequest("x".repeat(101), "Smith"))}) {
            authorized(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        }
        assertThat(repository.count()).isZero();
    }

    @Test void missingCustomerReturns404ForMutations() throws Exception {
        authorized(put("/api/customers/999999").contentType(MediaType.APPLICATION_JSON)
            .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"))
            .andExpect(status().isNotFound());
        authorized(delete("/api/customers/999999")).andExpect(status().isNotFound());
    }
}
