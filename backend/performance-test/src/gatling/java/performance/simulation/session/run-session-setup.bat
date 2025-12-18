@echo off
REM 세션 생성 스크립트 (Windows)
REM 사용법: run-session-setup.bat [사용자 수] [생성 속도(명/초)]
REM
REM 예시:
REM   run-session-setup.bat 100         - 100명, 기본 속도(100명/초)
REM   run-session-setup.bat 50000 200   - 50,000명, 200명/초

REM ==================== 환경변수 설정 ====================
REM 테스트 대상 서버 URL (로컬/VM에 따라 변경)
SET BASE_URL=http://localhost:8080
SET WS_BASE_URL=ws://localhost:8080

REM VM에서 실행 시 아래와 같이 수정:
REM SET BASE_URL=http://192.168.1.100:8080
REM SET WS_BASE_URL=ws://192.168.1.100:8080
REM ======================================================

SET TOTAL_USERS=%1
SET CREATION_RATE=%2

IF "%TOTAL_USERS%"=="" SET TOTAL_USERS=100
IF "%CREATION_RATE%"=="" SET CREATION_RATE=100

echo =========================================
echo   세션 생성 시작
echo =========================================
echo 총 사용자 수: %TOTAL_USERS%
echo 생성 속도: %CREATION_RATE%명/초
echo =========================================
echo.

set /p confirm="세션 생성을 시작하시겠습니까? (Y/N): "
if /i not "%confirm%"=="Y" (
    echo 세션 생성이 취소되었습니다.
    exit /b 0
)

echo 세션 생성을 시작합니다...
cd /d "%~dp0\..\..\..\..\..\..\..\..\"

gradlew.bat :performance-test:gatlingRun ^
  --simulation=performance.simulation.session.SessionSetupSimulation ^
  -DtotalUsers=%TOTAL_USERS% ^
  -DcreationRate=%CREATION_RATE% ^
  -DbaseUrl=%BASE_URL% ^
  -DwsBaseUrl=%WS_BASE_URL%
