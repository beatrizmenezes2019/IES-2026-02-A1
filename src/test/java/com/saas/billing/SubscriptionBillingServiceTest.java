package com.saas.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionBillingServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC);

    private FakeCustomerRepository repository;
    private List<String> sentEmails;
    private boolean paymentApproved;
    private SubscriptionBillingService service;

    @BeforeEach
    void setUp() {
        repository = new FakeCustomerRepository();
        sentEmails = new ArrayList<>();
        paymentApproved = true;
        service = new SubscriptionBillingService(
                repository,
                (token, amount) -> paymentApproved,
                (email, amount) -> sentEmails.add(email + "|" + amount),
                FIXED_CLOCK);
    }

    private BillingCommand command() {
        return new BillingCommand("c1", "BASIC", Money.of("100.00"),
                LocalDate.of(2026, 10, 1), 0, null, false, "tok_abc");
    }

    @Test
    void clienteInexistenteNaoGeraLogsNemEfeitos() {
        List<String> logs = service.processMonthlyBilling(command());

        assertTrue(logs.isEmpty());
        assertTrue(repository.invoices.isEmpty());
        assertTrue(sentEmails.isEmpty());
    }

    @Test
    void clienteInativoCancelaCobranca() {
        repository.customer = new Customer("a@b.com", "SUSPENDED");

        List<String> logs = service.processMonthlyBilling(command());

        assertEquals(List.of("Cliente inativo. Cobrança cancelada: c1"), logs);
        assertTrue(repository.invoices.isEmpty());
        assertTrue(sentEmails.isEmpty());
    }

    @Test
    void cobrancaComSucessoGravaFaturaPagaEEnviaEmail() {
        repository.customer = new Customer("a@b.com", "active");

        List<String> logs = service.processMonthlyBilling(command());

        assertEquals(List.of("Pagamento processado com sucesso: R$ 100.00",
                "E-mail disparado para: a@b.com"), logs);
        assertEquals(List.of("c1|100.00|PAID"), repository.invoices);
        assertEquals(List.of("a@b.com|100.00"), sentEmails);
    }

    @Test
    void falhaNoPagamentoGravaFaturaFailedEMantemEnvioDeEmail() {
        repository.customer = new Customer("a@b.com", "ACTIVE");
        paymentApproved = false;

        List<String> logs = service.processMonthlyBilling(command());

        assertEquals(List.of("Falha no pagamento do cartão para: c1",
                "E-mail disparado para: a@b.com"), logs);
        assertEquals(List.of("c1|100.00|FAILED"), repository.invoices);
    }

    @Test
    void falhaDeInfraestruturaEhRegistradaNoLog() {
        repository.failure = new InfrastructureException("db fora do ar", new RuntimeException());

        List<String> logs = service.processMonthlyBilling(command());

        assertEquals(List.of("Erro fatal no processamento: db fora do ar"), logs);
    }

    @Test
    void errosInesperadosNaoSaoMaisSilenciados() {
        repository.failure = new IllegalStateException("bug");

        assertThrows(IllegalStateException.class, () -> service.processMonthlyBilling(command()));
    }

    private static class FakeCustomerRepository implements CustomerRepository {
        Customer customer;
        RuntimeException failure;
        final List<String> invoices = new ArrayList<>();

        @Override
        public Optional<Customer> findById(String customerId) {
            if (failure != null) {
                throw failure;
            }
            return Optional.ofNullable(customer);
        }

        @Override
        public void saveInvoice(String customerId, Money amount, String status) {
            invoices.add(customerId + "|" + amount + "|" + status);
        }
    }
}
