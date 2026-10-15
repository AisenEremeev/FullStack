package com.example.jpalab;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CustomerRepositoryIT {
    @Autowired CustomerRepository repository;
    @Autowired TestEntityManager entityManager;

    @Test void savesAndReadsFromDatabaseAfterClearingPersistenceContext() {
        Customer saved = repository.saveAndFlush(new Customer("Ada", "Lovelace"));
        assertThat(saved.getId()).isPositive();
        entityManager.clear();
        Customer loaded = repository.findById(saved.getId()).orElseThrow();
        assertThat(loaded).isNotSameAs(saved);
        assertThat(loaded.getFirstName()).isEqualTo("Ada");
    }

    @Test void searchesCaseInsensitivelyAndSortsById() {
        Customer first = repository.save(new Customer("Ada", "Lovelace"));
        Customer second = repository.save(new Customer("Ann", "lovelace"));
        repository.saveAndFlush(new Customer("Grace", "Hopper"));
        entityManager.clear();
        assertThat(repository.findByLastNameIgnoreCaseOrderByIdAsc("LOVELACE"))
            .extracting(Customer::getId).containsExactly(first.getId(), second.getId());
        assertThat(repository.findByLastNameIgnoreCaseOrderByIdAsc("missing")).isEmpty();
    }

    @Test void flushesUpdatesAndDeletes() {
        Customer saved = repository.saveAndFlush(new Customer("Ada", "Lovelace"));
        saved.rename("Grace", "Hopper");
        entityManager.flush();
        entityManager.clear();
        assertThat(repository.findById(saved.getId()).orElseThrow().getLastName()).isEqualTo("Hopper");
        repository.deleteById(saved.getId());
        repository.flush();
        entityManager.clear();
        assertThat(repository.findById(saved.getId())).isEmpty();
    }
}
