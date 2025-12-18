@echo off
REM 라이브 스트리밍 부하 테스트 실행 스크립트 (Windows)
REM
REM 사용법:
REM   run-load-test.bat [단계] [옵션]
REM
REM 단계:
REM   1 - 소규모 테스트 (100명, spike)
REM   2 - 중규모 테스트 (1,000명, ramp 5분)
REM   3 - 대규모 테스트 (10,000명, ramp 10분)
REM   4 - 목표 테스트 (50,000명, realistic)
REM   5 - 스트레스 테스트 (100,000명, realistic)
REM
REM 예시:
REM   run-load-test.bat 1
REM   run-load-test.bat 4

REM ==================== 환경변수 설정 ====================
REM 테스트 대상 서버 URL (로컬/VM에 따라 변경)
SET BASE_URL=http://localhost:8080
SET WS_BASE_URL=ws://localhost:8080

REM VM에서 실행 시 아래와 같이 수정:
REM SET BASE_URL=http://192.168.1.100:8080
REM SET WS_BASE_URL=ws://192.168.1.100:8080
REM ======================================================

setlocal enabledelayedexpansion

if "%1"=="" (
    call :show_usage
    exit /b 1
)

set STAGE=%1

if "%STAGE%"=="1" goto stage1
if "%STAGE%"=="2" goto stage2
if "%STAGE%"=="3" goto stage3
if "%STAGE%"=="4" goto stage4
if "%STAGE%"=="5" goto stage5

echo 에러: 알 수 없는 단계 '%STAGE%'
echo.
call :show_usage
exit /b 1

:stage1
REM 1단계: 소규모 테스트 (100명, spike)
echo ========================================
echo   단계 1: 라이브 스트리밍 부하 테스트
echo ========================================
echo 총 사용자 수: 100
echo 부하 주입 패턴: spike
echo 세션 지속 시간: 360~420초
echo ========================================
echo.

set /p confirm="테스트를 시작하시겠습니까? (Y/N): "
if /i not "%confirm%"=="Y" (
    echo 테스트가 취소되었습니다.
    exit /b 0
)

echo 테스트를 시작합니다...
cd /d "%~dp0\..\..\..\..\..\..\..\..\"
call gradlew :performance-test:gatlingRun --simulation=performance.simulation.livestreaming.LiveStreamingLoadTestSimulation -DtotalUsers=100 -Dpattern=spike -DminDuration=360 -DmaxDuration=420 -DbaseUrl=%BASE_URL% -DwsBaseUrl=%WS_BASE_URL%

goto end

:stage2
REM 2단계: 중규모 테스트 (1,000명, ramp 5분)
echo ========================================
echo   단계 2: 라이브 스트리밍 부하 테스트
echo ========================================
echo 총 사용자 수: 1,000
echo 부하 주입 패턴: ramp
echo 램프 지속 시간: 300초
echo 세션 지속 시간: 600~900초
echo ========================================
echo.

set /p confirm="테스트를 시작하시겠습니까? (Y/N): "
if /i not "%confirm%"=="Y" (
    echo 테스트가 취소되었습니다.
    exit /b 0
)

echo 테스트를 시작합니다...
cd /d "%~dp0\..\..\..\..\..\..\..\..\"
call gradlew :performance-test:gatlingRun --simulation=performance.simulation.livestreaming.LiveStreamingLoadTestSimulation -DtotalUsers=1000 -Dpattern=ramp -DrampDuration=300 -DminDuration=600 -DmaxDuration=900 -DbaseUrl=%BASE_URL% -DwsBaseUrl=%WS_BASE_URL%

goto end

:stage3
REM 3단계: 대규모 테스트 (10,000명, ramp 10분)
echo ========================================
echo   단계 3: 라이브 스트리밍 부하 테스트
echo ========================================
echo 총 사용자 수: 10,000
echo 부하 주입 패턴: ramp
echo 램프 지속 시간: 600초
echo 세션 지속 시간: 900~1200초
echo ========================================
echo.

set /p confirm="테스트를 시작하시겠습니까? (Y/N): "
if /i not "%confirm%"=="Y" (
    echo 테스트가 취소되었습니다.
    exit /b 0
)

echo 테스트를 시작합니다...
cd /d "%~dp0\..\..\..\..\..\..\..\..\"
call gradlew :performance-test:gatlingRun --simulation=performance.simulation.livestreaming.LiveStreamingLoadTestSimulation -DtotalUsers=10000 -Dpattern=ramp -DrampDuration=600 -DminDuration=900 -DmaxDuration=1200 -DbaseUrl=%BASE_URL% -DwsBaseUrl=%WS_BASE_URL%

goto end

:stage4
REM 4단계: 목표 테스트 (50,000명, realistic)
echo ========================================
echo   단계 4: 라이브 스트리밍 부하 테스트
echo ========================================
echo 총 사용자 수: 50,000
echo 부하 주입 패턴: realistic
echo 세션 지속 시간: 1200~1800초
echo ========================================
echo.

set /p confirm="테스트를 시작하시겠습니까? (Y/N): "
if /i not "%confirm%"=="Y" (
    echo 테스트가 취소되었습니다.
    exit /b 0
)

echo 테스트를 시작합니다...
cd /d "%~dp0\..\..\..\..\..\..\..\..\"
call gradlew :performance-test:gatlingRun --simulation=performance.simulation.livestreaming.LiveStreamingLoadTestSimulation -DtotalUsers=50000 -Dpattern=realistic -DminDuration=1200 -DmaxDuration=1800 -DbaseUrl=%BASE_URL% -DwsBaseUrl=%WS_BASE_URL%

goto end

:stage5
REM 5단계: 스트레스 테스트 (100,000명, realistic)
echo ========================================
echo   경고: 스트레스 테스트
echo ========================================
echo 이 테스트는 시스템에 매우 높은 부하를 가합니다.
echo 테스트 환경이 충분한지 확인하세요.
echo ========================================
echo.
echo 총 사용자 수: 100,000
echo 부하 주입 패턴: realistic
echo 세션 지속 시간: 1200~1800초
echo.

set /p confirm="테스트를 시작하시겠습니까? (Y/N): "
if /i not "%confirm%"=="Y" (
    echo 테스트가 취소되었습니다.
    exit /b 0
)

echo 테스트를 시작합니다...
cd /d "%~dp0\..\..\..\..\..\..\..\..\"
call gradlew :performance-test:gatlingRun --simulation=performance.simulation.livestreaming.LiveStreamingLoadTestSimulation -DtotalUsers=100000 -Dpattern=realistic -DminDuration=1200 -DmaxDuration=1800 -DbaseUrl=%BASE_URL% -DwsBaseUrl=%WS_BASE_URL%

goto end

:show_usage
echo 사용법: %0 [단계]
echo.
echo 단계:
echo   1 - 소규모 테스트 (100명, spike)
echo   2 - 중규모 테스트 (1,000명, ramp 5분)
echo   3 - 대규모 테스트 (10,000명, ramp 10분)
echo   4 - 목표 테스트 (50,000명, realistic)
echo   5 - 스트레스 테스트 (100,000명, realistic)
echo.
echo 예시:
echo   %0 1
echo   %0 4
exit /b 0

:end
echo.
echo ========================================
echo   테스트가 완료되었습니다!
echo ========================================
echo 리포트 위치: backend\performance-test\build\reports\gatling\
echo.
exit /b 0
