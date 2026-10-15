package com.example.jpalab;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

// Pure unit tests: no Spring context, database or HTTP server.
@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock CustomerRepository repository;
    @InjectMocks CustomerService service;

    @Test void listsCustomersInIdOrder() {
        Customer customer = new Customer("Ada", "Lovelace");
        when(repository.findAll(Sort.by("id"))).thenReturn(List.of(customer));
        assertThat(service.list(null)).containsExactly(customer);
        verify(repository, never()).findByLastNameIgnoreCaseOrderByIdAsc(anyString());
    }

    @Test void trimsSearchTermAndReturnsMatchingRecords() {
        Customer customer = new Customer("Ada", "Lovelace");
        when(repository.findByLastNameIgnoreCaseOrderByIdAsc("LOVELACE")).thenReturn(List.of(customer));
        assertThat(service.list("  LOVELACE  ")).containsExactly(customer);
    }

    @Test void returnsExistingCustomer() {
        Customer customer = new Customer("Ada", "Lovelace");
        when(repository.findById(7L)).thenReturn(Optional.of(customer));
        assertThat(service.get(7L)).isSameAs(customer);
    }

    @Test void reportsMissingCustomer() {
        when(repository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(7L)).isInstanceOfSatisfying(
            ResponseStatusException.class, ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test void trimsNamesBeforeSaving() {
        when(repository.save(any(Customer.class))).thenAnswer(call -> call.getArgument(0));
        Customer saved = service.create(new CustomerRequest(" Ada ", " Lovelace "));
        assertThat(saved.getFirstName()).isEqualTo("Ada");
        assertThat(saved.getLastName()).isEqualTo("Lovelace");
        ArgumentCaptor<Customer> captured = ArgumentCaptor.forClass(Customer.class);
        verify(repository).save(captured.capture());
        assertThat(captured.getValue()).isSameAs(saved);
    }

    @Test void updatesManagedEntityWithoutChangingItsIdentity() {
        Customer existing = new Customer("Ada", "Lovelace");
        when(repository.findById(7L)).thenReturn(Optional.of(existing));
        assertThat(service.update(7L, new CustomerRequest(" Grace ", " Hopper "))).isSameAs(existing);
        assertThat(existing.getFirstName()).isEqualTo("Grace");
        assertThat(existing.getLastName()).isEqualTo("Hopper");
    }

    @Test void deletesRequestedCustomer() {
        Customer customer = new Customer("Ada", "Lovelace");
        when(repository.findById(7L)).thenReturn(Optional.of(customer));
        service.delete(7L);
        verify(repository).delete(customer);
    }

    @Test void doesNotDeleteWhenCustomerIsMissing() {
        when(repository.findById(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.delete(7L)).isInstanceOf(ResponseStatusException.class);
        verify(repository, never()).delete(any(Customer.class));
    }
}
