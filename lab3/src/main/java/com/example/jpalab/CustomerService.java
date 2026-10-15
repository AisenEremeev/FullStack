package com.example.jpalab;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CustomerService {
    private final CustomerRepository repository;
    public CustomerService(CustomerRepository repository) { this.repository = repository; }
    public List<Customer> list(String lastName) {
        return lastName == null ? repository.findAll(Sort.by("id"))
            : repository.findByLastNameIgnoreCaseOrderByIdAsc(lastName.trim());
    }
    public Customer get(Long id) {
        return repository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }
    @Transactional
    public Customer create(CustomerRequest request) {
        return repository.save(new Customer(request.firstName().trim(), request.lastName().trim()));
    }
    @Transactional
    public Customer update(Long id, CustomerRequest request) {
        Customer customer = get(id);
        customer.rename(request.firstName().trim(), request.lastName().trim());
        return customer;
    }
    @Transactional
    public void delete(Long id) { repository.delete(get(id)); }
}
