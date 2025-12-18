#!/bin/bash

# 세션 생성 스크립트
# 사용법: ./run-session-setup.sh [사용자 수] [생성 속도(명/초)]
#
# 예시:
#   ./run-session-setup.sh 100         # 100명, 기본 속도(100명/초)
#   ./run-session-setup.sh 50000 200   # 50,000명, 200명/초

# ==================== 환경변수 설정 ====================
# 테스트 대상 서버 URL (로컬/VM에 따라 변경)
BASE_URL=${BASE_URL:-http://localhost:8080}
WS_BASE_URL=${WS_BASE_URL:-ws://localhost:8080}

# VM에서 실행 시 아래와 같이 수정:
# export BASE_URL=http://192.168.1.100:8080
# export WS_BASE_URL=ws://192.168.1.100:8080
# ======================================================

TOTAL_USERS=${1:-100}
CREATION_RATE=${2:-100}

echo "========================================="
echo "  세션 생성 시작"
echo "========================================="
echo "총 사용자 수: $TOTAL_USERS"
echo "생성 속도: ${CREATION_RATE}명/초"
echo "========================================="
echo ""

read -p "세션 생성을 시작하시겠습니까? (y/N): " confirm
if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    echo "세션 생성이 취소되었습니다."
    exit 0
fi

echo "세션 생성을 시작합니다..."
cd "$(dirname "$0")/../../../../../../../" || exit 1

./gradlew :performance-test:gatlingRun \
  --simulation=performance.simulation.session.SessionSetupSimulation \
  -DtotalUsers="$TOTAL_USERS" \
  -DcreationRate="$CREATION_RATE" \
  -DbaseUrl="$BASE_URL" \
  -DwsBaseUrl="$WS_BASE_URL"
