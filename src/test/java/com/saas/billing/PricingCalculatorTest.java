package com.saas.billing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);
    private static final LocalDate NOVEMBER = LocalDate.of(2026, 11, 20);

    private final PricingCalculator calculator = new PricingCalculator();

    private BillingCommand command(String base, LocalDate signup, int overdueDays, String promo, boolean nonProfit) {
        return new BillingCommand("c1", "BASIC", Money.of(base), signup, overdueDays, promo, nonProfit, "tok_abc");
    }

    @Test
    void semDescontosMantemPrecoBase() {
        Money price = calculator.calculate(command("100.00", LocalDate.of(2026, 10, 1), 0, null, false), TODAY);
        assertEquals(Money.of("100.00"), price);
    }

    @Test
    void descontoDe10PorCentoAposUmAno() {
        Money price = calculator.calculate(command("100.00", LocalDate.of(2025, 8, 1), 0, null, false), TODAY);
        assertEquals(Money.of("90.00"), price);
    }

    @Test
    void descontoDe15PorCentoAposDoisAnos() {
        Money price = calculator.calculate(command("100.00", LocalDate.of(2024, 1, 1), 0, null, false), TODAY);
        assertEquals(Money.of("85.00"), price);
    }

    @Test
    void limitesExatosDe12E24MesesNaoGanhamODescontoSuperior() {
        Money em12 = calculator.calculate(command("100.00", LocalDate.of(2025, 10, 1), 0, null, false), TODAY);
        Money em24 = calculator.calculate(command("100.00", LocalDate.of(2024, 10, 1), 0, null, false), TODAY);
        assertEquals(Money.of("100.00"), em12);
        assertEquals(Money.of("90.00"), em24);
    }

    @Test
    void descontoParaOng() {
        Money price = calculator.calculate(command("100.00", LocalDate.of(2026, 10, 1), 0, null, true), TODAY);
        assertEquals(Money.of("70.00"), price);
    }

    @Test
    void blackFridayApenasEmNovembro() {
        Money emNovembro = calculator.calculate(command("100.00", LocalDate.of(2026, 11, 1), 0, "blackfriday", false), NOVEMBER);
        Money emOutubro = calculator.calculate(command("100.00", LocalDate.of(2026, 10, 1), 0, "BLACKFRIDAY", false), TODAY);
        assertEquals(Money.of("70.00"), emNovembro);
        assertEquals(Money.of("100.00"), emOutubro);
    }

    @Test
    void welcome10ApenasParaAssinaturaRecente() {
        Money recente = calculator.calculate(command("100.00", LocalDate.of(2026, 10, 1), 0, "WELCOME10", false), TODAY);
        Money antiga = calculator.calculate(command("100.00", LocalDate.of(2025, 10, 1), 0, "WELCOME10", false), TODAY);
        assertEquals(Money.of("90.00"), recente);
        assertEquals(Money.of("100.00"), antiga);
    }

    @Test
    void cupomEmBrancoOuDesconhecidoEhIgnorado() {
        Money vazio = calculator.calculate(command("100.00", LocalDate.of(2026, 10, 1), 0, "   ", false), TODAY);
        Money invalido = calculator.calculate(command("100.00", LocalDate.of(2026, 10, 1), 0, "XYZ", false), TODAY);
        assertEquals(Money.of("100.00"), vazio);
        assertEquals(Money.of("100.00"), invalido);
    }

    @Test
    void multaEJurosPorAtraso() {
        // multa 2% = 2.00; juros 0.1% * 10 dias = 1.00
        Money price = calculator.calculate(command("100.00", LocalDate.of(2026, 10, 1), 10, null, false), TODAY);
        assertEquals(Money.of("103.00"), price);
    }

    @Test
    void precoNuncaFicaNegativo() {
        Money price = calculator.calculate(command("20.00", LocalDate.of(2026, 11, 1), 0, "BLACKFRIDAY", false), NOVEMBER);
        assertEquals(Money.ZERO, price);
    }

    @Test
    void combinacaoDeRegrasComArredondamento() {
        // 200 -> 15% (170.00) -> ONG (119.00) -> multa 2.38 + juros 0.595 (arredonda para 0.60)
        Money price = calculator.calculate(command("200.00", LocalDate.of(2024, 1, 1), 5, null, true), TODAY);
        assertEquals(Money.of("121.98"), price);
    }
}
