#!/bin/bash
echo "에러 시나리오 시작..."
for i in {1..30}
do
  curl -s -X POST http://localhost:8080/scenario/error > /dev/null
  sleep 0.2
done
echo "완료"