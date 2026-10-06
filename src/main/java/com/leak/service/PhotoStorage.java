package com.leak.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/** 照片本地存储：上传文件保存到 leak.storage.photo-dir，数据库只存文件名。 */
@ApplicationScoped
public class PhotoStorage {

    private static final Set<String> ALLOWED_EXT = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".svg");

    @ConfigProperty(name = "leak.storage.photo-dir")
    String photoDir;

    public Path dir() {
        Path p = Path.of(photoDir);
        try {
            Files.createDirectories(p);
        } catch (IOException e) {
            throw new IllegalStateException("无法创建照片目录: " + p, e);
        }
        return p;
    }

    /** 保存上传临时文件，返回磁盘文件名。 */
    public String store(Path uploaded, String originalName, String contentType) {
        String ext = extOf(originalName, contentType);
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Files.copy(uploaded, dir().resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("照片保存失败: " + fileName, e);
        }
        return fileName;
    }

    /** 种子数据直接写入一段字节（SVG 占位图）。 */
    public String storeBytes(byte[] bytes, String ext) {
        String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
        try {
            Files.write(dir().resolve(fileName), bytes);
        } catch (IOException e) {
            throw new IllegalStateException("照片保存失败: " + fileName, e);
        }
        return fileName;
    }

    public Path resolve(String fileName) {
        return dir().resolve(fileName);
    }

    private String extOf(String name, String contentType) {
        if (name != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0) {
                String ext = name.substring(dot).toLowerCase();
                if (ALLOWED_EXT.contains(ext)) {
                    return ext;
                }
            }
        }
        if (contentType != null) {
            return switch (contentType.toLowerCase()) {
                case "image/png" -> ".png";
                case "image/gif" -> ".gif";
                case "image/webp" -> ".webp";
                case "image/svg+xml" -> ".svg";
                default -> ".jpg";
            };
        }
        return ".jpg";
    }
}
