package com.saas.billing;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Orquestrador (fachada) do fluxo de cobrança mensal. */
public class SubscriptionBillingService {

    private final CustomerRepository customerRepository;
    private final PaymentGateway paymentGateway;
    private final NotificationService notificationService;
    private final Clock clock;
    private final PricingCalculator pricingCalculator = new PricingCalculator();

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
        Money price = pricingCalculator.calculate(command, LocalDate.now(clock));
        try {
            Optional<Customer> found = customerRepository.findById(command.customerId());
            if (found.isEmpty()) {
                return logs;
            }
            Customer customer = found.get();
            if (!customer.isActive()) {
                logs.add("Cliente inativo. Cobrança cancelada: " + command.customerId());
                return logs;
            }
            boolean paid = paymentGateway.charge(command.paymentCardToken(), price);
            logs.add(paid ? "Pagamento processado com sucesso: R$ " + price
                          : "Falha no pagamento do cartão para: " + command.customerId());
            customerRepository.saveInvoice(command.customerId(), price, paid ? "PAID" : "FAILED");
            notificationService.sendInvoice(customer.email(), price);
            logs.add("E-mail disparado para: " + customer.email());
        } catch (InfrastructureException e) {
            logs.add("Erro fatal no processamento: " + e.getMessage());
        }
        return logs;
    }
}
