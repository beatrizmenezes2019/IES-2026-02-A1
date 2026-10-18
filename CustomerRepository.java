package com.saas.billing;

import java.util.Optional;

/** Porta de saída: acesso aos dados de clientes e faturas. */
public interface CustomerRepository {

    Optional<Customer> findById(String customerId);

    void saveInvoice(String customerId, Money amount, String status);
}
