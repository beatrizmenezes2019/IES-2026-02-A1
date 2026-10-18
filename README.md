# IES-2026-02-A1 — Refatoração do núcleo financeiro (SaaS Billing Co.)

Atividade prática de **Refatoração** da disciplina **Implantação e Entrega de Software (INF0468)** — UFG/INF, 2026/2, Profª. Sofia Costa Paiva.

**Dupla:** Erick Vaz e Beatriz Menezes

## Contexto

O projeto parte de um código legado, `SubscriptionBillingService`, que calcula a assinatura mensal, aplica cupons, cobra no cartão e envia o recibo por e-mail. Todo esse fluxo estava concentrado em um único método, com acesso direto a banco de dados (JDBC), SMTP e data do sistema.

**Objetivo:** pagar o débito técnico de código e de arquitetura sem quebrar nenhum comportamento de negócio, deixando os testes unitários executáveis na CI sem infraestrutura externa (banco ou SMTP).

## Documentação

O diagnóstico do débito técnico (matriz de *code smells* e violações arquiteturais) e a comparação de métricas antes/depois estão no relatório técnico:

**[RELATORIO.md](RELATORIO.md)**

## O que foi feito

A refatoração foi feita em passos, com um commit por etapa:

| Commit | Etapa | O que mudou |
|--------|-------|-------------|
| 1 | Código legado | Adição do código legado base e do template do `RELATORIO.md`. |
| 2 | Value Objects | `Money` (record imutável sobre `BigDecimal`, com arredondamento centralizado) e `BillingCommand` (Parameter Object que substitui os 8 parâmetros soltos). Remove *Primitive Obsession*. |
| 3 | Inversão de dependência | Portas `CustomerRepository`, `PaymentGateway` e `NotificationService`, com adaptadores `JdbcCustomerRepository`, `TokenPaymentGateway` e `SmtpNotificationService`. Injeção de `java.time.Clock`, eliminando `LocalDate.now()` estático. |
| 4 | Precificação e testes | `PricingCalculator` (domínio puro), orquestrador com até 25 linhas, remoção do `catch (Exception)` genérico e suíte de testes unitários JUnit 5. |

## Arquitetura resultante

```
SubscriptionBillingService (orquestrador / fachada)
 ├── PricingCalculator ............ regras de preço (domínio puro, sem efeitos colaterais)
 ├── CustomerRepository  (porta) ← JdbcCustomerRepository
 ├── PaymentGateway      (porta) ← TokenPaymentGateway
 ├── NotificationService (porta) ← SmtpNotificationService
 └── Clock ........................ injetado, torna os cálculos de data determinísticos
```

Entrada: `BillingCommand`. Valores monetários: `Money`.

## Estrutura do projeto

```
src/main/java/com/saas/billing/   código de produção
src/test/java/com/saas/billing/   testes unitários (fakes em memória, Clock fixo)
pom.xml                           build Maven (Java 17, JUnit 5)
RELATORIO.md                      relatório técnico da atividade
```

## Como executar os testes

Requisitos: JDK 17+ e Maven.

```bash
mvn test
```

Os testes não dependem de banco de dados nem de servidor SMTP.

## Observações

- A refatoração preserva as regras de negócio, com uma diferença: os valores agora são arredondados a centavos (`HALF_UP`) por `Money`, e a mensagem de log passa a exibir duas casas decimais (ex.: `R$ 100.00`). O legado usava `double` sem arredondamento.
- O `catch (Exception)` do legado foi substituído por `catch (InfrastructureException)`; erros inesperados deixam de ser silenciados.
- O envio real do e-mail (`send()`) continua comentado, como no legado.
