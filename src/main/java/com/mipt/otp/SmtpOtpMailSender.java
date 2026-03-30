package com.mipt.otp;

import jakarta.mail.internet.InternetAddress;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class SmtpOtpMailSender implements OtpMailSender {
  private static final ZoneId MOSCOW_TIME_ZONE = ZoneId.of("Europe/Moscow");
  private static final DateTimeFormatter EXPIRY_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'MSK'", Locale.forLanguageTag("ru-RU"))
          .withZone(MOSCOW_TIME_ZONE);

  private final JavaMailSender mailSender;
  private final MailSenderProperties properties;

  public SmtpOtpMailSender(JavaMailSender mailSender, MailSenderProperties properties) {
    this.mailSender = mailSender;
    this.properties = properties;
  }

  @Override
  public void sendOtp(String email, OtpPurpose purpose, String code, Instant expiresAt) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(email);
    message.setFrom(buildFromAddress());
    message.setSubject(buildSubject(purpose));
    message.setText(buildBody(purpose, code, expiresAt));

    try {
      mailSender.send(message);
    } catch (MailException exception) {
      throw new OtpException("Failed to send OTP email: " + exception.getMessage(), exception);
    }
  }

  private String buildSubject(OtpPurpose purpose) {
    return switch (purpose) {
      case REGISTRATION -> "Завершите регистрацию в QuizAI";
      case PASSWORD_RESET -> "Сброс пароля QuizAI";
    };
  }

  private String buildBody(OtpPurpose purpose, String code, Instant expiresAt) {
    String action = switch (purpose) {
      case REGISTRATION -> "завершения регистрации";
      case PASSWORD_RESET -> "сброса пароля";
    };

    return "Используйте одноразовый код для " + action + ":\n\n"
        + code
        + "\n\nКод активен до "
        + EXPIRY_FORMATTER.format(expiresAt)
        + ".\nЕсли вы не запрашивали данный код, пожалуйста, проигнорируйте сообщение. "
        + "\n\nСообщение отправлено автоматически, не отвечайте на него.\nКоманда QUIZAI";
  }

  private String buildFromAddress() {
    try {
      return new InternetAddress(properties.getFrom(), properties.getFromName()).toString();
    } catch (UnsupportedEncodingException exception) {
      return properties.getFrom();
    }
  }
}
