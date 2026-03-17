#!/bin/bash
echo "응답 지연 시나리오 시작..."
for i in {1..10}
do
  curl -s -X POST http://localhost:8080/scenario/slow > /dev/null &
done
echo "완료"