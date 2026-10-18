package com.saas.billing;

/** Porta de saída: envio de notificações ao cliente. */
public interface NotificationService {

    void sendInvoice(String email, Money amount);
}
