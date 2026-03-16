#!/bin/bash
echo "정상 트래픽 시작..."
for i in {1..100}
do
  curl -s http://localhost:8080/api/health > /dev/null
  curl -s http://localhost:8080/api/users > /dev/null
  curl -s http://localhost:8080/api/orders > /dev/null
  sleep 0.5
done
echo "완료"