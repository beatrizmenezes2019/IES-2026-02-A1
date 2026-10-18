package com.saas.billing;

/** Falha de infraestrutura (banco, SMTP, gateway) propagada sem checked exceptions. */
public class InfrastructureException extends RuntimeException {

    public InfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}
