package com.leak.service;

import java.nio.file.Path;

/** 从 multipart 表单提取的上传照片信息。 */
public record UploadedPhoto(Path path, String originalName, String contentType) {
}
