package com.saas.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object imutável que representa um valor monetário (em R$).
 * Centraliza a escala (2 casas) e a política de arredondamento (HALF_UP),
 * evitando o uso de double para dinheiro.
 */
public record Money(BigDecimal amount) {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "amount não pode ser nulo");
        amount = amount.setScale(SCALE, ROUNDING);
    }

    public static Money of(double value) {
        return new Money(BigDecimal.valueOf(value));
    }

    public static Money of(String value) {
        return new Money(new BigDecimal(value));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(other.amount));
    }

    /** Multiplica pelo fator informado (ex.: 0.02 para 2%) e arredonda. */
    public Money times(BigDecimal factor) {
        return new Money(amount.multiply(factor));
    }

    /** Aplica um desconto percentual (ex.: 0.15 para 15% de desconto). */
    public Money discountBy(BigDecimal rate) {
        return times(BigDecimal.ONE.subtract(rate));
    }

    /** Garante que o valor nunca seja negativo. */
    public Money nonNegative() {
        return amount.signum() < 0 ? ZERO : this;
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
