package com.tigtech.persfinance.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Simple local filesystem storage implementation.
 * Keeps the same interface so the rest of the app doesn't need changes.
 */
@Service
public class LocalStorageService implements StorageService {

    private final Path baseDir;

    public LocalStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) throws IOException {
        this.baseDir = Path.of(uploadDir).toAbsolutePath();
        if (!Files.exists(baseDir)) {
            Files.createDirectories(baseDir);
        }
    }

    @Override
    public String uploadUserPhoto(MultipartFile file, String userId) throws Exception {
        String safeFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path userDir = baseDir.resolve("users").resolve(userId);
        if (!Files.exists(userDir)) Files.createDirectories(userDir);
        Path target = userDir.resolve(safeFileName);
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
        return "/uploads/users/" + userId + "/" + safeFileName;
    }

    @Override
    public void delete(String pathOrUrl) throws Exception {
        if (pathOrUrl == null) return;
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) return;
        Path p = baseDir.resolve(pathOrUrl.replaceFirst("^/", ""));
        File f = p.toFile();
        if (f.exists()) f.delete();
    }
}

