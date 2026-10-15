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
class CustomerIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CustomerRepository repository;
    @Autowired ObjectMapper mapper;

    @BeforeEach void clean() { repository.deleteAll(); }

    @Test void completeCrudCyclePersistsChanges() throws Exception {
        String body = mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
            .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"))
            .andExpect(status().isCreated()).andExpect(header().exists("Location"))
            .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(body).get("id").asLong();
        assertThat(repository.findById(id)).isPresent();
        mvc.perform(get("/api/customers/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.firstName").value("Ada"));
        mvc.perform(get("/api/customers")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/customers").param("lastName", "LOVELACE"))
            .andExpect(jsonPath("$[0].id").value(id));
        mvc.perform(get("/api/customers").param("lastName", "Unknown"))
            .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(put("/api/customers/" + id).contentType(MediaType.APPLICATION_JSON)
            .content("{\"firstName\":\"Grace\",\"lastName\":\"Hopper\"}"))
            .andExpect(status().isOk());
        assertThat(repository.findById(id).orElseThrow().getLastName()).isEqualTo("Hopper");
        mvc.perform(delete("/api/customers/" + id)).andExpect(status().isNoContent());
        assertThat(repository.findById(id)).isEmpty();
        mvc.perform(get("/api/customers/" + id)).andExpect(status().isNotFound());
    }

    @Test void rejectsInvalidInputWithoutWriting() throws Exception {
        for (String body : new String[]{"{}", "{\"firstName\":\"  \",\"lastName\":\"Smith\"}",
                mapper.writeValueAsString(new CustomerRequest("x".repeat(101), "Smith"))}) {
            mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        }
        assertThat(repository.count()).isZero();
    }

    @Test void missingCustomerReturns404ForMutations() throws Exception {
        mvc.perform(put("/api/customers/999999").contentType(MediaType.APPLICATION_JSON)
            .content("{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\"}"))
            .andExpect(status().isNotFound());
        mvc.perform(delete("/api/customers/999999")).andExpect(status().isNotFound());
    }

    @Test void errorsHaveUsefulJsonBodies() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
            .andExpect(jsonPath("$.errors.firstName").exists())
            .andExpect(jsonPath("$.errors.lastName").exists());
        mvc.perform(get("/api/customers/999999"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.detail").value("Customer not found"));
        mvc.perform(get("/api/customers/not-a-number")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest());
    }
}
