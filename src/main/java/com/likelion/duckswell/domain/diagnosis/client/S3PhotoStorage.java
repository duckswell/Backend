package com.likelion.duckswell.domain.diagnosis.client;

import com.likelion.duckswell.domain.diagnosis.exception.DiagnosisErrorCode;
import com.likelion.duckswell.global.exception.CustomException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * PhotoStorage의 S3 구현체. prod 프로필에서만 뜬다.
 * 인증은 IAM Role 기반(DefaultCredentialsProvider) access key/secret을 코드나 설정에 직접 넣지 않는다.
 * 배포 인스턴스(EC2 등)에 S3 접근 권한이 있는 IAM Role이 붙어있어야 동작한다.
 *
 * analyze.py/check_photo.py는 로컬 파일 경로만 읽을 수 있어서, resolvePath()는 S3 객체를
 * 로컬 임시 파일로 내려받은 뒤 그 경로를 돌려준다.
 * CV 서브프로세스 호출 전 필요한 다운로드 단계.
 */
@Slf4j
@Profile("prod")
@Component
public class S3PhotoStorage implements PhotoStorage {

    private final S3Client s3Client;
    private final String bucket;

    public S3PhotoStorage(
            @Value("${aws.region}") String region,
            @Value("${aws.s3.bucket}") String bucket
    ) {
        this.bucket = bucket;
        this.s3Client = S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    @Override
    public String save(MultipartFile photo) {
        String extension = StringUtils.getFilenameExtension(photo.getOriginalFilename());
        String key = UUID.randomUUID() + (extension != null ? "." + extension : "");

        try {
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucket).key(key).contentType(photo.getContentType()).build(),
                    RequestBody.fromInputStream(photo.getInputStream(), photo.getSize())
            );
            return key;
        } catch (IOException e) {
            throw new CustomException(DiagnosisErrorCode.PHOTO_NOT_FOUND);
        }
    }

    @Override
    public String resolvePath(String photoId) {
        // 미리 빈 파일을 만들어두면 SDK가 "이미 존재하는 파일"로 보고 실패할 수 있어서,
        // 경로만 만들고 실제 파일 생성은 getObject()가 하게 둔다.
        // 다운로드된 임시 파일은 사용이 끝나면 cleanupResolvedPath()로 지운다(여기서 deleteOnExit로
        // 미루면 장기 실행 서버에서 요청마다 임시 파일이 계속 쌓인다).
        Path tempFile = Path.of(System.getProperty("java.io.tmpdir"), "diagnosis-photo-" + UUID.randomUUID() + suffixOf(photoId));

        try {
            s3Client.getObject(GetObjectRequest.builder().bucket(bucket).key(photoId).build(), tempFile);
            return tempFile.toString();
        } catch (NoSuchKeyException e) {
            throw new CustomException(DiagnosisErrorCode.PHOTO_NOT_FOUND);
        }
    }

    @Override
    public void delete(String photoId) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(photoId).build());
    }

    @Override
    public void cleanupResolvedPath(String resolvedPath) {
        try {
            Files.deleteIfExists(Path.of(resolvedPath));
        } catch (IOException e) {
            log.warn("임시 파일 삭제 실패: {}", resolvedPath, e);
        }
    }

    private String suffixOf(String photoId) {
        String extension = StringUtils.getFilenameExtension(photoId);
        return extension != null ? "." + extension : "";
    }
}
