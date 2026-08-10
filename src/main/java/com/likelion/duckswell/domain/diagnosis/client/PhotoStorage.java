package com.likelion.duckswell.domain.diagnosis.client;

import org.springframework.web.multipart.MultipartFile;

/**
 * 진단용 사진 저장소 계약. 사진은 품질체크 시점에 딱 한 번만 업로드되고
 * 통과하면 그때 발급된 photoId를 최종 제출 요청에 그대로 실어 보내는 방식이라
 * 구현체가 무엇이든 (로컬 디스크, S3 등) photoId ↔ 실제 위치를 이 3개 메서드로만 왕복 변환.
 * 배포 환경이 정해지면 이 인터페이스의 새 구현체(예: S3PhotoStorage)만 추가하면 되고
 * Controller/Service는 건드릴 필요 없다.
 */
public interface PhotoStorage {

    /** 사진을 저장하고 photoId를 반환한다. */
    String save(MultipartFile photo);

    /** photoId로 실제 파일 위치(경로 또는 URL)를 찾는다. 없거나 유효하지 않으면 거부한다. */
    String resolvePath(String photoId);

    /** 품질 체크에서 탈락한 사진 등, 더 이상 필요 없는 파일을 지운다. */
    void delete(String photoId);

    /**
     * resolvePath()가 CV 분석용으로 임시 파일을 만들었다면(S3 등) 사용이 끝난 뒤 정리한다.
     * 로컬처럼 원본 저장 경로를 그대로 반환하는 구현체는 지울 필요가 없어 기본값은 아무 동작도 하지 않는다.
     */
    default void cleanupResolvedPath(String resolvedPath) {
    }
}
