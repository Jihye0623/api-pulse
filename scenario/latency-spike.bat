@echo off
chcp 65001 >nul
echo 응답 지연 시나리오 시작...
for /L %%i in (1,1,10) do (
  echo %%i번째 지연 요청 중...
  start /B curl -X POST http://localhost:8080/scenario/slow
)
echo 완료!