# Relatório Técnico — Atividade Prática: Refatoração
**Disciplina:** Implantação e Entrega de Software (INF0468) — 2026/2  
**Projeto:** SaaS Billing Co. — `SubscriptionBillingService`  
**Dupla:** Erick Vaz e Beatriz Menezes

### 1. Matriz de Code Smells e Violações Arquiteturais

| # | Linha/trecho do código afetado | Code Smell / Princípio violado | "Juros" (impacto na manutenibilidade e na entrega contínua) |
|---|---|---|---|
| **1** | Linhas 59–87 (`DriverManager.getConnection(...)` e comandos SQL) | Violação do Dependency Inversion Principle (DIP) / Acoplamento Direto com I/O (Banco de Dados) | Impossibilita a execução de testes unitários na esteira de CI sem uma instância ativa do PostgreSQL. Torna a execução de testes lenta e sujeita a falhas de ambiente, violando a restrição de não depender de infraestrutura externa. |
| **2** | Linhas 18–105 (Método `processMonthlyBilling` completo) | God Method / Violação do Single Responsibility Principle (SRP) | Concentra regras de precificação, aplicação de cupons, cobrança via cartão, SQL e envio de e-mail em um único ponto. Qualquer alteração em regras fiscais ou promocionais pode quebrar o fluxo de pagamento ou banco, elevando o risco de regressões em produção. |
| **3** | Linhas 18-20 (Assinatura com 8 argumentos em tipos primitivos e `String`) | Long Parameter List / Primitive Obsession | Dificulta a leitura, aumenta a probabilidade de inversão acidental de argumentos e utiliza `double` para valores financeiros (gerando imprecisão decimal) e `String` para datas (com risco de exceções em tempo de execução ao aplicar `LocalDate.parse()`). |
| **4** | Linha 25 e Linha 42 (`LocalDate today = LocalDate.now();`) | Dependência Direta do Relógio do Sistema (Hardcoded System Clock) | Impede a escrita de testes unitários determinísticos para regras de negócio atreladas ao tempo (ex.: verificação da regra de Black Friday em novembro ou tempo de contrato). Os testes na esteira de CI produzirão resultados inconsistentes dependendo do dia em que forem executados. |
| **5** | Linhas 89–96 (`SimpleEmail emailSender = new SimpleEmail();`) | Instanciação Direta de Infraestrutura (SMTP) / Violação do DIP e Open/Closed Principle (OCP) | Acopla o serviço de domínio diretamente a uma biblioteca de terceiros e a configurações de rede hardcoded. Impede a substituição do serviço de e-mail por um Mock/Fake durante o ciclo de CI e exige alteração no código de negócio caso o provedor de e-mail mude. |
