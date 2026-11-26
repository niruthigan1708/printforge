package com.printforge.service;

import com.printforge.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Delivers password reset links by email over SMTP (configured with MAIL_HOST etc.). When no SMTP
 * server is configured, as in local development, the link is written to the log instead so the
 * flow can still be used; a warning says so, because a production setup must configure SMTP.
 */
@Component
public class PasswordResetMailer {
  private static final Logger log = LoggerFactory.getLogger(PasswordResetMailer.class);
  private final ObjectProvider<JavaMailSender> mailSender;
  private final String from;

  private final boolean smtpConfigured;

  public PasswordResetMailer(ObjectProvider<JavaMailSender> mailSender, @Value("${app.mail.from:PrintForge <no-reply@printforge.local>}") String from,
                             @Value("${spring.mail.host:}") String smtpHost) {
    this.mailSender = mailSender;
    this.from = from;
    // application.yml always defines spring.mail.host (as ${MAIL_HOST:}), and Spring Boot creates a
    // mail sender whenever the property exists, even when empty. Decide by the value instead.
    this.smtpConfigured = smtpHost != null && !smtpHost.isBlank();
  }

  public void send(User user, String link, int validMinutes) {
    JavaMailSender sender = smtpConfigured ? mailSender.getIfAvailable() : null;
    if (sender == null) {
      log.warn("SMTP is not configured (MAIL_HOST unset), so the password reset link for {} is logged instead of emailed: {}", user.getEmail(), link);
      return;
    }
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(user.getEmail());
    message.setSubject("Reset your PrintForge password");
    message.setText("Hi " + user.getName() + ",\n\n"
      + "Someone (hopefully you) asked to reset the password for your PrintForge account.\n"
      + "Open this link within " + validMinutes + " minutes to choose a new password:\n\n"
      + link + "\n\n"
      + "If you did not ask for this, you can ignore this email; your password stays the same.\n");
    try {
      sender.send(message);
    } catch (MailException e) {
      // Don't tell the requester (that would reveal whether the account exists); make it visible to operators.
      log.error("Could not send the password reset email to {}", user.getEmail(), e);
    }
  }
}
