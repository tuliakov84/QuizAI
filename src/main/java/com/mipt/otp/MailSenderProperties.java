package com.mipt.otp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.mail")
public class MailSenderProperties {
  private String host = "smtp.yandex.com";
  private int port = 465;
  private String username = "";
  private String password = "";
  private String protocol = "smtp";
  private boolean auth = true;
  private boolean starttls = false;
  private boolean ssl = true;
  private String from = "quizai@yandex.com";
  private String fromName = "QuizAI";
  private long connectionTimeoutMs = 5000;
  private long timeoutMs = 5000;
  private long writeTimeoutMs = 5000;

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public boolean isAuth() {
    return auth;
  }

  public void setAuth(boolean auth) {
    this.auth = auth;
  }

  public boolean isStarttls() {
    return starttls;
  }

  public void setStarttls(boolean starttls) {
    this.starttls = starttls;
  }

  public boolean isSsl() {
    return ssl;
  }

  public void setSsl(boolean ssl) {
    this.ssl = ssl;
  }

  public String getFrom() {
    return from;
  }

  public void setFrom(String from) {
    this.from = from;
  }

  public String getFromName() {
    return fromName;
  }

  public void setFromName(String fromName) {
    this.fromName = fromName;
  }

  public long getConnectionTimeoutMs() {
    return connectionTimeoutMs;
  }

  public void setConnectionTimeoutMs(long connectionTimeoutMs) {
    this.connectionTimeoutMs = connectionTimeoutMs;
  }

  public long getTimeoutMs() {
    return timeoutMs;
  }

  public void setTimeoutMs(long timeoutMs) {
    this.timeoutMs = timeoutMs;
  }

  public long getWriteTimeoutMs() {
    return writeTimeoutMs;
  }

  public void setWriteTimeoutMs(long writeTimeoutMs) {
    this.writeTimeoutMs = writeTimeoutMs;
  }
}
