#!/bin/bash

set -e

echo "Spring Boot 빌드 시작"

cd ../backend
./gradlew :api:clean :api:bootJar --no-build-cache

echo "JAR 파일 복사"

cp api/build/libs/api-0.0.1-SNAPSHOT.jar ../vm-load-test/app.jar
echo "app.jar 복사 완료"

echo "Docker Compose 실행"

cd ../vm-load-test
docker compose up -d

echo "실행 완료"
echo "로그 확인: docker compose logs -f app"
echo "중지: docker compose down"