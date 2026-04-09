package com.mipt.config;

import com.mipt.otp.MailSenderProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@EnableConfigurationProperties(MailSenderProperties.class)
public class MailSenderConfig {
  @Bean
  public JavaMailSender javaMailSender(MailSenderProperties properties) {
    JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
    mailSender.setHost(properties.getHost());
    mailSender.setPort(properties.getPort());
    mailSender.setUsername(properties.getUsername());
    mailSender.setPassword(properties.getPassword());
    mailSender.setProtocol(properties.getProtocol());

    Properties javaMailProperties = mailSender.getJavaMailProperties();
    javaMailProperties.put("mail.smtp.auth", String.valueOf(properties.isAuth()));
    javaMailProperties.put("mail.smtp.starttls.enable", String.valueOf(properties.isStarttls()));
    javaMailProperties.put("mail.smtp.ssl.enable", String.valueOf(properties.isSsl()));
    javaMailProperties.put("mail.smtp.connectiontimeout", properties.getConnectionTimeoutMs());
    javaMailProperties.put("mail.smtp.timeout", properties.getTimeoutMs());
    javaMailProperties.put("mail.smtp.writetimeout", properties.getWriteTimeoutMs());

    return mailSender;
  }
}
