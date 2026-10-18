package com.saas.billing;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Regras de precificação da assinatura. Classe de domínio pura: sem I/O,
 * sem relógio global e sem efeitos colaterais.
 */
public class PricingCalculator {

    private static final BigDecimal LOYALTY_2_YEARS = new BigDecimal("0.15");
    private static final BigDecimal LOYALTY_1_YEAR = new BigDecimal("0.10");
    private static final BigDecimal NON_PROFIT_DISCOUNT = new BigDecimal("0.30");
    private static final BigDecimal WELCOME10_DISCOUNT = new BigDecimal("0.10");
    private static final Money BLACK_FRIDAY_DISCOUNT = Money.of("30.00");
    private static final BigDecimal LATE_PENALTY_RATE = new BigDecimal("0.02");
    private static final BigDecimal DAILY_INTEREST_RATE = new BigDecimal("0.001");

    public Money calculate(BillingCommand command, LocalDate today) {
        int monthsActive = monthsBetween(command.signupDate(), today);

        Money price = applyLoyaltyDiscount(command.basePrice(), monthsActive);
        price = applyNonProfitDiscount(price, command.nonProfit());
        price = applyPromoCode(price, command, monthsActive, today);
        price = applyLateFees(price, command.overdueDays());
        return price.nonNegative();
    }

    private int monthsBetween(LocalDate signupDate, LocalDate today) {
        return (today.getYear() - signupDate.getYear()) * 12
                + (today.getMonthValue() - signupDate.getMonthValue());
    }

    private Money applyLoyaltyDiscount(Money price, int monthsActive) {
        if (monthsActive > 24) {
            return price.discountBy(LOYALTY_2_YEARS);
        }
        if (monthsActive > 12) {
            return price.discountBy(LOYALTY_1_YEAR);
        }
        return price;
    }

    private Money applyNonProfitDiscount(Money price, boolean nonProfit) {
        return nonProfit ? price.discountBy(NON_PROFIT_DISCOUNT) : price;
    }

    private Money applyPromoCode(Money price, BillingCommand command, int monthsActive, LocalDate today) {
        if (!command.hasPromoCode()) {
            return price;
        }
        String promoCode = command.promoCode();
        if (promoCode.equalsIgnoreCase("BLACKFRIDAY") && today.getMonthValue() == 11) {
            return price.minus(BLACK_FRIDAY_DISCOUNT);
        }
        if (promoCode.equalsIgnoreCase("WELCOME10") && monthsActive <= 1) {
            return price.discountBy(WELCOME10_DISCOUNT);
        }
        return price;
    }

    private Money applyLateFees(Money price, int overdueDays) {
        if (overdueDays <= 0) {
            return price;
        }
        Money penalty = price.times(LATE_PENALTY_RATE);
        Money dailyInterest = price.times(DAILY_INTEREST_RATE.multiply(BigDecimal.valueOf(overdueDays)));
        return price.plus(penalty).plus(dailyInterest);
    }
}
