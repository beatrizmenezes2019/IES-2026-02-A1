package com.saas.billing;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;

/** Adaptador SMTP da porta NotificationService. */
public class SmtpNotificationService implements NotificationService {

    @Override
    public void sendInvoice(String email, Money amount) {
        try {
            SimpleEmail emailSender = new SimpleEmail();
            emailSender.setHostName("smtp.empresa.com");
            emailSender.setSmtpPort(587);
            emailSender.setFrom("billing@empresa.com");
            emailSender.setSubject("Sua Fatura Mensal");
            emailSender.setMsg("Olá! Sua fatura foi processada no valor de R$ " + amount);
            emailSender.addTo(email);
            // emailSender.send();
        } catch (EmailException e) {
            throw new InfrastructureException(e.getMessage(), e);
        }
    }
}
