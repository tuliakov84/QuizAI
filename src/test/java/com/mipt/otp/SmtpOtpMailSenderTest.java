package com.mipt.otp;

import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmtpOtpMailSenderTest {

  @Test
  void shouldFormatOtpExpiryTimeInMoscowTimeZone() {
    CapturingMailSender javaMailSender = new CapturingMailSender();
    SmtpOtpMailSender otpMailSender = new SmtpOtpMailSender(javaMailSender, mailProperties());

    otpMailSender.sendOtp(
        "user@example.com",
        OtpPurpose.REGISTRATION,
        "123456",
        Instant.parse("2026-03-30T12:00:00Z")
    );

    SimpleMailMessage message = javaMailSender.lastMessage;
    assertNotNull(message);
    assertEquals("user@example.com", message.getTo()[0]);
    assertTrue(message.getText().contains("2026-03-30 15:00:00 MSK"));
  }

  private static MailSenderProperties mailProperties() {
    MailSenderProperties properties = new MailSenderProperties();
    properties.setFrom("quizai@yandex.com");
    properties.setFromName("QuizAI");
    return properties;
  }

  private static final class CapturingMailSender implements JavaMailSender {
    private SimpleMailMessage lastMessage;

    @Override
    public void send(SimpleMailMessage simpleMessage) {
      this.lastMessage = simpleMessage;
    }

    @Override
    public void send(SimpleMailMessage... simpleMessages) {
      if (simpleMessages.length > 0) {
        this.lastMessage = simpleMessages[simpleMessages.length - 1];
      }
    }

    @Override
    public jakarta.mail.internet.MimeMessage createMimeMessage() {
      throw new UnsupportedOperationException();
    }

    @Override
    public jakarta.mail.internet.MimeMessage createMimeMessage(java.io.InputStream contentStream) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void send(jakarta.mail.internet.MimeMessage mimeMessage) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void send(jakarta.mail.internet.MimeMessage... mimeMessages) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void send(org.springframework.mail.javamail.MimeMessagePreparator mimeMessagePreparator) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void send(org.springframework.mail.javamail.MimeMessagePreparator... mimeMessagePreparators) {
      throw new UnsupportedOperationException();
    }
  }
}
