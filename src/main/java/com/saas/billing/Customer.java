package com.saas.billing;

/** Dados do cliente necessários para a cobrança. */
public record Customer(String email, String status) {

    public boolean isActive() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
