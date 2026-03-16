@echo off
chcp 65001 >nul
echo 에러 시나리오 시작...
for /L %%i in (1,1,30) do (
  echo %%i번째 에러 요청 중...
  curl -X POST http://localhost:8080/scenario/error
)
echo 완료!