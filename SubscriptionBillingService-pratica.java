package com.saas.billing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SubscriptionBillingService {

    private final CustomerRepository customerRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;
    private final Clock clock;

    public SubscriptionBillingService(CustomerRepository customerRepository,
                                      PaymentGateway paymentGateway,
                                      NotificationService notificationService,
                                      Clock clock) {
        this.customerRepository = Objects.requireNonNull(customerRepository);
        this.paymentGateway = Objects.requireNonNull(paymentGateway);
        this.notificationService = Objects.requireNonNull(notificationService);
        this.clock = Objects.requireNonNull(clock);
    }

    public List<String> processMonthlyBilling(BillingCommand command) {

        List<String> logs = new ArrayList<>();

        String customerId = command.customerId();
        LocalDate signupDate = command.signupDate();
        LocalDate today = LocalDate.now(clock);

        int monthsActive = (today.getYear() - signupDate.getYear()) * 12 + (today.getMonthValue() - signupDate.getMonthValue());

        Money calculatedPrice = command.basePrice();

        if (monthsActive > 24) {
            calculatedPrice = calculatedPrice.discountBy(new BigDecimal("0.15")); // 15% após 2 anos
        } else if (monthsActive > 12) {
            calculatedPrice = calculatedPrice.discountBy(new BigDecimal("0.10")); // 10% após 1 ano
        }

        if (command.nonProfit()) {
            calculatedPrice = calculatedPrice.discountBy(new BigDecimal("0.30")); // 30% off para ONGs
        }

        if (command.hasPromoCode()) {
            String promoCode = command.promoCode();
            if (promoCode.equalsIgnoreCase("BLACKFRIDAY") && today.getMonthValue() == 11) {
                calculatedPrice = calculatedPrice.minus(Money.of(30.0));
            } else if (promoCode.equalsIgnoreCase("WELCOME10") && monthsActive <= 1) {
                calculatedPrice = calculatedPrice.discountBy(new BigDecimal("0.10"));
            }
        }

        if (command.isOverdue()) {
            Money penalty = calculatedPrice.times(new BigDecimal("0.02"));
            BigDecimal dailyRate = new BigDecimal("0.001").multiply(BigDecimal.valueOf(command.overdueDays()));
            Money dailyInterest = calculatedPrice.times(dailyRate);
            calculatedPrice = calculatedPrice.plus(penalty).plus(dailyInterest);
        }

        calculatedPrice = calculatedPrice.nonNegative();

        try {
            Optional<Customer> found = customerRepository.findById(customerId);

            if (found.isPresent()) {
                Customer customer = found.get();

                if (!"ACTIVE".equalsIgnoreCase(customer.status())) {
                    logs.add("Cliente inativo. Cobrança cancelada: " + customerId);
                    return logs;
                }

                boolean paymentSuccess = paymentGateway.charge(command.paymentCardToken(), calculatedPrice);
                if (paymentSuccess) {
                    logs.add("Pagamento processado com sucesso: R$ " + calculatedPrice);
                } else {
                    logs.add("Falha no pagamento do cartão para: " + customerId);
                }

                customerRepository.saveInvoice(customerId, calculatedPrice, paymentSuccess ? "PAID" : "FAILED");

                notificationService.sendInvoice(customer.email(), calculatedPrice);

                logs.add("E-mail disparado para: " + customer.email());
            }
        } catch (Exception e) {
            logs.add("Erro fatal no processamento: " + e.getMessage());
        }

        return logs;
    }
}
