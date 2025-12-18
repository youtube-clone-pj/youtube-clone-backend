#!/bin/bash

# 라이브 스트리밍 부하 테스트 실행 스크립트
#
# 사용법:
#   ./run-load-test.sh [단계] [옵션]
#
# 단계:
#   1 - 소규모 테스트 (100명, spike)
#   2 - 중규모 테스트 (1,000명, ramp 5분)
#   3 - 대규모 테스트 (10,000명, ramp 10분)
#   4 - 목표 테스트 (50,000명, realistic)
#   5 - 스트레스 테스트 (100,000명, realistic)
#   custom - 사용자 정의 설정
#
# 예시:
#   ./run-load-test.sh 1
#   ./run-load-test.sh 4
#   ./run-load-test.sh custom -DtotalUsers=5000 -Dpattern=ramp -DrampDuration=180

set -e

# ==================== 환경변수 설정 ====================
# 테스트 대상 서버 URL (로컬/VM에 따라 변경)
BASE_URL=${BASE_URL:-http://localhost:8080}
WS_BASE_URL=${WS_BASE_URL:-ws://localhost:8080}

# VM에서 실행 시 아래와 같이 수정:
# export BASE_URL=http://192.168.1.100:8080
# export WS_BASE_URL=ws://192.168.1.100:8080
# ======================================================

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 함수: 테스트 실행
run_test() {
    local stage=$1
    local total_users=$2
    local pattern=$3
    local ramp_duration=$4
    local min_duration=$5
    local max_duration=$6

    echo -e "${BLUE}========================================${NC}"
    echo -e "${GREEN}  단계 ${stage}: 라이브 스트리밍 부하 테스트${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo -e "총 사용자 수: ${YELLOW}${total_users}${NC}"
    echo -e "부하 주입 패턴: ${YELLOW}${pattern}${NC}"
    if [ "$pattern" == "ramp" ]; then
        echo -e "램프 지속 시간: ${YELLOW}${ramp_duration}초${NC}"
    fi
    echo -e "세션 지속 시간: ${YELLOW}${min_duration}~${max_duration}초${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""

    # Gradle 명령 구성
    local cmd="./gradlew :performance-test:gatlingRun"
    cmd="$cmd --simulation=performance.simulation.livestreaming.LiveStreamingLoadTestSimulation"
    cmd="$cmd -DtotalUsers=$total_users"
    cmd="$cmd -Dpattern=$pattern"

    if [ "$pattern" == "ramp" ]; then
        cmd="$cmd -DrampDuration=$ramp_duration"
    fi

    cmd="$cmd -DminDuration=$min_duration"
    cmd="$cmd -DmaxDuration=$max_duration"
    cmd="$cmd -DbaseUrl=$BASE_URL"
    cmd="$cmd -DwsBaseUrl=$WS_BASE_URL"

    echo -e "${YELLOW}실행 명령:${NC}"
    echo "$cmd"
    echo ""

    # 확인 메시지
    read -p "테스트를 시작하시겠습니까? (y/N): " confirm
    if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
        echo -e "${RED}테스트가 취소되었습니다.${NC}"
        exit 0
    fi

    # 테스트 실행
    echo -e "${GREEN}테스트를 시작합니다...${NC}"
    cd "$(dirname "$0")/../../../../../../../" || exit 1
    eval $cmd

    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}  테스트가 완료되었습니다!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo -e "리포트 위치: ${YELLOW}backend/performance-test/build/reports/gatling/${NC}"
}

# 함수: 사용법 출력
show_usage() {
    echo "사용법: $0 [단계] [옵션]"
    echo ""
    echo "단계:"
    echo "  1      - 소규모 테스트 (100명, spike)"
    echo "  2      - 중규모 테스트 (1,000명, ramp 5분)"
    echo "  3      - 대규모 테스트 (10,000명, ramp 10분)"
    echo "  4      - 목표 테스트 (50,000명, realistic)"
    echo "  5      - 스트레스 테스트 (100,000명, realistic)"
    echo "  custom - 사용자 정의 설정"
    echo ""
    echo "예시:"
    echo "  $0 1"
    echo "  $0 4"
    echo "  $0 custom -DtotalUsers=5000 -Dpattern=ramp -DrampDuration=180"
}

# 메인 로직
if [ $# -lt 1 ]; then
    show_usage
    exit 1
fi

STAGE=$1
shift # 첫 번째 인자 제거

case $STAGE in
    1)
        # 1단계: 소규모 테스트 (100명, spike)
        # 목적: 시나리오 동작 확인
        run_test "1" 100 "spike" 0 360 420
        ;;
    2)
        # 2단계: 중규모 테스트 (1,000명, ramp 5분)
        # 목적: 병목 지점 초기 파악
        run_test "2" 1000 "ramp" 300 600 900
        ;;
    3)
        # 3단계: 대규모 테스트 (10,000명, ramp 10분)
        # 목적: 시스템 한계 탐색
        run_test "3" 10000 "ramp" 600 900 1200
        ;;
    4)
        # 4단계: 목표 테스트 (50,000명, realistic)
        # 목적: 실제 시나리오 시뮬레이션
        run_test "4" 50000 "realistic" 300 1200 1800
        ;;
    5)
        # 5단계: 스트레스 테스트 (100,000명, realistic)
        # 목적: 시스템 한계점 확인
        echo -e "${RED}========================================${NC}"
        echo -e "${RED}  경고: 스트레스 테스트${NC}"
        echo -e "${RED}========================================${NC}"
        echo -e "${YELLOW}이 테스트는 시스템에 매우 높은 부하를 가합니다.${NC}"
        echo -e "${YELLOW}테스트 환경이 충분한지 확인하세요.${NC}"
        echo -e "${RED}========================================${NC}"
        echo ""

        run_test "5" 100000 "realistic" 300 1200 1800
        ;;
    custom)
        # 사용자 정의 설정
        if [ $# -lt 3 ]; then
            echo -e "${RED}에러: custom 모드는 최소 3개의 파라미터가 필요합니다.${NC}"
            echo "예시: $0 custom -DtotalUsers=5000 -Dpattern=ramp -DrampDuration=180"
            exit 1
        fi

        echo -e "${BLUE}========================================${NC}"
        echo -e "${GREEN}  사용자 정의 부하 테스트${NC}"
        echo -e "${BLUE}========================================${NC}"

        # Gradle 명령 구성
        local cmd="./gradlew :performance-test:gatlingRun --simulation=performance.simulation.livestreaming.LiveStreamingLoadTestSimulation"
        for arg in "$@"; do
            cmd="$cmd $arg"
        done
        cmd="$cmd -DbaseUrl=$BASE_URL"
        cmd="$cmd -DwsBaseUrl=$WS_BASE_URL"

        echo -e "${YELLOW}실행 명령:${NC}"
        echo "$cmd"
        echo ""

        read -p "테스트를 시작하시겠습니까? (y/N): " confirm
        if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
            echo -e "${RED}테스트가 취소되었습니다.${NC}"
            exit 0
        fi

        echo -e "${GREEN}테스트를 시작합니다...${NC}"
        cd "$(dirname "$0")/../../../../../../../" || exit 1
        eval $cmd

        echo ""
        echo -e "${GREEN}========================================${NC}"
        echo -e "${GREEN}  테스트가 완료되었습니다!${NC}"
        echo -e "${GREEN}========================================${NC}"
        echo -e "리포트 위치: ${YELLOW}backend/performance-test/build/reports/gatling/${NC}"
        ;;
    *)
        echo -e "${RED}에러: 알 수 없는 단계 '$STAGE'${NC}"
        echo ""
        show_usage
        exit 1
        ;;
esac
