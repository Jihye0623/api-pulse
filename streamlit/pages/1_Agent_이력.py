# streamlit/pages/3_Agent_이력.py

import streamlit as st
import requests
import pandas as pd

API_URL = "http://localhost:8080"

st.set_page_config(page_title="Agent 이력", page_icon="🤖", layout="wide")
st.title("🤖 Agent 실행 이력")

# 새로고침 버튼
if st.button("🔄 새로고침"):
    st.rerun()

# Agent 로그 조회
try:
    res = requests.get(f"{API_URL}/agent/logs", timeout=5)
    logs = res.json()
except Exception as e:
    st.error(f"API 연결 실패: {e}")
    st.stop()

if not logs:
    st.info("Agent 실행 이력이 없습니다.")
    st.stop()

# 요약 지표
col1, col2, col3 = st.columns(3)
with col1:
    st.metric("총 실행 횟수", len(logs))
with col2:
    avg_ms = sum(l["responseTimeMs"] for l in logs) / len(logs)
    st.metric("평균 응답시간", f"{avg_ms:.0f} ms")
with col3:
    trigger_types = set(l["triggerType"] for l in logs)
    st.metric("트리거 유형", len(trigger_types))

st.divider()

# 이력 목록
for log in logs:
    # 트리거 타입별 색상
    trigger = log.get("triggerType", "")
    if trigger == "STATISTICAL_ANOMALY":
        badge = "🔴"
    elif trigger == "CARD_ERROR":
        badge = "🟠"
    elif trigger == "MANUAL":
        badge = "🔵"
    else:
        badge = "⚪"

    with st.expander(
        f"{badge} [{log['triggerType']}] {log['createdAt'][:19]}  |  {log['responseTimeMs']} ms"
    ):
        col1, col2 = st.columns([1, 2])

        with col1:
            st.markdown("**트리거 유형**")
            st.code(log.get("triggerType", "-"))

            st.markdown("**사용 Tool**")
            tools = log.get("toolsUsed", "-")
            for tool in tools.split(","):
                st.markdown(f"- `{tool.strip()}`")

            st.markdown("**응답시간**")
            st.markdown(f"`{log.get('responseTimeMs', 0)} ms`")

        with col2:
            st.markdown("**상황 입력값**")
            st.info(log.get("situation", "-"))

            st.markdown("**Agent 판단 결과**")
            st.markdown(log.get("agentResult", "-"))