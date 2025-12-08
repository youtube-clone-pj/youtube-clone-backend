#!/bin/bash

echo "Docker Compose 중지 및 데이터 삭제 중..."
docker compose down -v

echo "초기화 완료 (볼륨 포함 삭제)"