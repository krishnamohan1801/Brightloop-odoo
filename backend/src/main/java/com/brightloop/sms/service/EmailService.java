package com.brightloop.sms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:kmp8107@gmail.com}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        String subject = "Brightloop Software - Password Reset Request";
        String content = "Hello,\n\n" +
                "You requested to reset your password for your Brightloop Subscription Management System account.\n\n" +
                "Please click the link below to set a new password:\n" +
                resetLink + "\n\n" +
                "This link will expire in 1 hour. If you did not request this, please disregard this email.\n\n" +
                "Best regards,\nBrightloop Software Team";

        log.info("------------------------------------------------------------------------");
        log.info("PASSWORD RESET LINK FOR {}: {}", toEmail, resetLink);
        log.info("------------------------------------------------------------------------");

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("Could not dispatch email via SMTP (using fallback logging): {}", e.getMessage());
        }
    }

    public void sendInvoiceNotification(String toEmail, String invoiceNumber, BigDecimal amount, String dueDate) {
        String subject = "Brightloop Invoice Generated: " + invoiceNumber;
        String content = "Hello,\n\n" +
                "A new subscription invoice " + invoiceNumber + " in the amount of $" + amount + " has been generated.\n" +
                "Payment Due Date: " + dueDate + "\n\n" +
                "Please log in to your customer portal to view and pay this invoice.\n\n" +
                "Best regards,\nBrightloop Software Team";

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
        } catch (Exception e) {
            log.debug("Invoice email dispatch notice: {}", e.getMessage());
        }
    }
}
