@echo off
chcp 65001 >nul
echo 정상 트래픽 시작...
for /L %%i in (1,1,30) do (
  echo %%i번째 요청 중...
  curl -s http://localhost:8080/api/health
  curl -s http://localhost:8080/api/users
  curl -s http://localhost:8080/api/orders
)
echo 완료!