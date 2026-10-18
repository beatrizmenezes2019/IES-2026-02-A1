package com.saas.billing;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Parameter Object imutável com os dados de entrada de uma cobrança mensal.
 * Substitui a lista de 8 parâmetros soltos de processMonthlyBilling.
 */
public record BillingCommand(
        String customerId,
        String planType,
        Money basePrice,
        LocalDate signupDate,
        int overdueDays,
        String promoCode,
        boolean nonProfit,
        String paymentCardToken) {

    public BillingCommand {
        Objects.requireNonNull(basePrice, "basePrice não pode ser nulo");
        Objects.requireNonNull(signupDate, "signupDate não pode ser nula");
    }

    public boolean isOverdue() {
        return overdueDays > 0;
    }

    public boolean hasPromoCode() {
        return promoCode != null && !promoCode.trim().isEmpty();
    }
}
