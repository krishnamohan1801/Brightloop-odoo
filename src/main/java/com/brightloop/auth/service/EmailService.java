package com.brightloop.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) { this.mailSender = mailSender; }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Brightloop - Password Reset Request");
        message.setText("""
                We received a request to reset your Brightloop password.

                Use the link below to choose a new password. This link is valid for 15 minutes and can only be used once.

                %s

                If you did not request this change, you can ignore this email.
                """.formatted(resetLink));
        mailSender.send(message);
    }
}
