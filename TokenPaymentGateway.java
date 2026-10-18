package com.saas.billing;

/** Adaptador da porta PaymentGateway: aprova qualquer token com prefixo "tok_" (regra do legado). */
public class TokenPaymentGateway implements PaymentGateway {

    @Override
    public boolean charge(String cardToken, Money amount) {
        return cardToken != null && cardToken.startsWith("tok_");
    }
}
