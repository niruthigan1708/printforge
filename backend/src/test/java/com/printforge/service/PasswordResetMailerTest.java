package com.printforge.service;

import com.printforge.entity.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PasswordResetMailerTest {
  private static User user() {
    User user = new User();
    user.setName("Mail Tester");
    user.setEmail("mail-tester@example.com");
    return user;
  }

  @SuppressWarnings("unchecked")
  private static ObjectProvider<JavaMailSender> provider(JavaMailSender sender) {
    ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(sender);
    return provider;
  }

  @Test void sendsTheLinkOverSmtpWhenConfigured() {
    JavaMailSender sender = mock(JavaMailSender.class);
    new PasswordResetMailer(provider(sender), "PrintForge <no-reply@shop.example>", "smtp.shop.example").send(user(), "https://shop.example/reset-password?token=abc", 30);

    ArgumentCaptor<SimpleMailMessage> message = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(sender).send(message.capture());
    assertArrayEquals(new String[] { "mail-tester@example.com" }, message.getValue().getTo());
    assertEquals("PrintForge <no-reply@shop.example>", message.getValue().getFrom());
    assertTrue(message.getValue().getText().contains("https://shop.example/reset-password?token=abc"));
    assertTrue(message.getValue().getText().contains("30 minutes"));
  }

  @Test void withoutSmtpItDoesNotFail() {
    assertDoesNotThrow(() -> new PasswordResetMailer(provider(null), "x@y.z", "").send(user(), "http://localhost:3000/reset-password?token=abc", 30));
  }

  @Test void anEmptyHostMeansLogTheLinkEvenIfAMailSenderBeanExists() {
    // Spring Boot creates a mail sender whenever spring.mail.host is defined, even as "";
    // that sender must not be used, or the link is lost instead of logged.
    JavaMailSender sender = mock(JavaMailSender.class);
    new PasswordResetMailer(provider(sender), "x@y.z", " ").send(user(), "http://localhost:3000/reset-password?token=abc", 30);
    verifyNoInteractions(sender);
  }

  @Test void anSmtpFailureIsNotReportedToTheRequester() {
    JavaMailSender sender = mock(JavaMailSender.class);
    doThrow(new MailSendException("smtp down")).when(sender).send(any(SimpleMailMessage.class));
    // Surfacing the failure would reveal that the account exists; it is logged for operators instead.
    assertDoesNotThrow(() -> new PasswordResetMailer(provider(sender), "x@y.z", "smtp.shop.example").send(user(), "link", 30));
  }
}
