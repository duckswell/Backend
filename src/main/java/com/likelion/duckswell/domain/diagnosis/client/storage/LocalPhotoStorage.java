package com.likelion.duckswell.domain.diagnosis.client.storage;

import com.likelion.duckswell.domain.diagnosis.exception.DiagnosisErrorCode;
import com.likelion.duckswell.global.exception.CustomException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * PhotoStorage의 로컬 디스크 구현체. local 프로필에서만.
 * 배포 환경은 별도 구현체(S3 등)를 추가해서 그쪽 프로필에 연결.
 */
@Profile("local")
@Component
public class LocalPhotoStorage implements PhotoStorage {

    private final Path uploadDir;

    public LocalPhotoStorage(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String save(MultipartFile photo) {
        try {
            Files.createDirectories(uploadDir);
            String extension = StringUtils.getFilenameExtension(photo.getOriginalFilename());
            String filename = UUID.randomUUID() + (extension != null ? "." + extension : "");
            photo.transferTo(uploadDir.resolve(filename));
            return filename;
        } catch (IOException e) {
            throw new CustomException(DiagnosisErrorCode.PHOTO_NOT_FOUND);
        }
    }

    @Override
    public String resolvePath(String photoId) {
        Path resolved = uploadDir.resolve(photoId).normalize();
        if (!resolved.startsWith(uploadDir) || !Files.isRegularFile(resolved)) {
            throw new CustomException(DiagnosisErrorCode.PHOTO_NOT_FOUND);
        }
        return resolved.toString();
    }

    @Override
    public void delete(String photoId) {
        try {
            Files.deleteIfExists(uploadDir.resolve(photoId).normalize());
        } catch (IOException ignored) {
        }
    }
}
