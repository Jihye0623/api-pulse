import streamlit as st
import requests
import pandas as pd

API = "http://localhost:8080"

st.set_page_config(page_title="Agent 이력", layout="wide")
st.title("🤖 Agent 판단 이력")
st.caption("AI 기본법 '설명 방안·문서화' 책무 — 모든 AI 판단 근거 기록")

try:
    logs = requests.get(f"{API}/agent/logs", timeout=3).json()
except:
    logs = []
    st.error("Spring Boot 서버에 연결할 수 없습니다.")

if logs:
    for log in logs:
        level_icon = "🚨" if "ESCALATION" in log.get("triggerType", "") else "🤖"
        with st.expander(
            f"{level_icon} [{log['triggerType']}] {log['createdAt']} "
            f"— {log['responseTimeMs']}ms"
        ):
            st.markdown("**입력 상황**")
            st.info(log["situation"])

            st.markdown("**사용된 Tool**")
            tools = log["toolsUsed"].split(",")
            cols = st.columns(len(tools))
            for i, tool in enumerate(tools):
                cols[i].markdown(f"`{tool.strip()}`")

            st.markdown("**Agent 판단 결과**")
            st.success(log["agentResult"])
else:
    st.info("Agent 실행 이력이 없습니다.")