package com.saas.billing;

/** Porta de saída: cobrança no cartão de crédito. */
public interface PaymentGateway {

    /** @return true se a cobrança foi aprovada. */
    boolean charge(String cardToken, Money amount);
}
