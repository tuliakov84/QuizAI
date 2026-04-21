package com.mipt.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
  private final String avatarStorageLocation;

  public StaticResourceConfig(@Value("${app.avatar.storage-dir:uploads/avatars}") String avatarStorageDir) {
    this.avatarStorageLocation = Path.of(avatarStorageDir).toAbsolutePath().normalize().toUri().toString();
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/uploads/avatars/**")
        .addResourceLocations(avatarStorageLocation.endsWith("/") ? avatarStorageLocation : avatarStorageLocation + "/");
  }
}
