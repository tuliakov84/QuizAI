package com.mipt.service;

import com.mipt.dbAPI.DatabaseAccessException;
import com.mipt.dbAPI.DbService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AvatarStorageService {
  private static final long MAX_AVATAR_SIZE_BYTES = 5L * 1024L * 1024L;
  private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

  private final DbService dbService;
  private final Path storageRoot;

  public AvatarStorageService(
      DbService dbService,
      @Value("${app.avatar.storage-dir:uploads/avatars}") String storageDir
  ) {
    this.dbService = dbService;
    this.storageRoot = Path.of(storageDir).toAbsolutePath().normalize();
  }

  public String storeAvatar(String session, MultipartFile avatarFile)
      throws IOException, SQLException, DatabaseAccessException {
    if (avatarFile == null || avatarFile.isEmpty()) {
      throw new IllegalArgumentException("Avatar file is required");
    }

    if (avatarFile.getSize() > MAX_AVATAR_SIZE_BYTES) {
      throw new IllegalArgumentException("Avatar file is too large");
    }

    String extension = resolveExtension(avatarFile.getOriginalFilename(), avatarFile.getContentType());
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new IllegalArgumentException("Unsupported avatar format");
    }

    Integer userId = dbService.getUserId(session);
    if (userId == null) {
      throw new DatabaseAccessException("User session not found");
    }

    Files.createDirectories(storageRoot);

    String filename = "user-" + userId + "-" + UUID.randomUUID() + "." + extension;
    Path targetPath = storageRoot.resolve(filename).normalize();
    Files.copy(avatarFile.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

    String avatarUrl = "/uploads/avatars/" + filename;
    dbService.changeCustomAvatarPath(session, avatarUrl);
    return avatarUrl;
  }

  private static String resolveExtension(String originalFilename, String contentType) {
    String extension = StringUtils.getFilenameExtension(originalFilename);
    if (extension != null && !extension.isBlank()) {
      return extension.toLowerCase(Locale.ROOT);
    }

    if (contentType == null) {
      return "";
    }

    return switch (contentType.toLowerCase(Locale.ROOT)) {
      case "image/jpeg" -> "jpg";
      case "image/png" -> "png";
      case "image/webp" -> "webp";
      default -> "";
    };
  }
}
