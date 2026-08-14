# syntax=docker/dockerfile:1

# ===== 1) BUILD STAGE =====
# 빌드에 필요한 JDK 21 환경을 기반으로 시작합니다.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle 의존성 캐싱 (소스코드 변경 시 매번 라이브러리를 받지 않도록 최적화)
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

# 소스 복사 후 실행 가능한 .jar 파일로 빌드
# COPY . .가 git이 추적 중인 gradlew(실행권한 없을 수 있음)로 다시 덮어쓰므로 여기서도 chmod 필요
COPY . .
RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon -x test

# ===== 2) RUNTIME STAGE =====
# eclipse-temurin:21-jre(태그 미지정)는 Temurin 쪽 사정으로 베이스 OS가 바뀔 수 있어서,
# apt로 python3.11을 설치해야 하는 이 이미지에서만 jammy를 명시 고정한다.
FROM eclipse-temurin:21-jre-jammy
ENV TZ=Asia/Seoul \
    DEBIAN_FRONTEND=noninteractive \
    PYTHONIOENCODING=utf-8 \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Seoul"
WORKDIR /opt/app

# python3.11 + venv + opencv/mediapipe가 필요로 하는 최소 시스템 라이브러리 (opencv-python 비-headless라 필요).
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        python3.11 python3.11-venv python3-pip \
        libgl1 libglib2.0-0 \
        curl \
    && rm -rf /var/lib/apt/lists/*

# AI 레포 전체가 아니라 런타임에 필요한 4개 항목만 이미지에 구워넣는다
# (dataset/, venv/, uploads/, README.md 등은 불필요 - 팀원이 정리해둔 목록 기준).
COPY AI/requirements.txt /opt/app/ai/requirements.txt
RUN python3.11 -m venv /opt/venv \
    && /opt/venv/bin/pip install --no-cache-dir --upgrade pip \
    && /opt/venv/bin/pip install --no-cache-dir -r /opt/app/ai/requirements.txt

COPY AI/src/features /opt/app/ai/src/features
COPY AI/scripts/analyze.py /opt/app/ai/scripts/analyze.py
COPY AI/scripts/check_photo.py /opt/app/ai/scripts/check_photo.py
COPY AI/models_export.json /opt/app/ai/models_export.json

# 1단계(build)에서 완성된 결과물 .jar 파일만 쏙 복사 (이미지 용량 최소화)
COPY --from=build /app/build/libs/*.jar app.jar

ENV DUCKSWELL_AI_PYTHON_PATH=/opt/venv/bin/python \
    DUCKSWELL_AI_SCRIPTS_PATH=/opt/app/ai/scripts

EXPOSE 8080

# actuator는 없지만 permitAll로 열려있는 /api/members/me를 헬스체크 대용으로 사용
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=5 \
    CMD curl -f http://localhost:8080/api/members/me || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
