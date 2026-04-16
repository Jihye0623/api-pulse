import streamlit as st
import requests

API = "http://localhost:8080"

st.set_page_config(page_title="챗봇", layout="wide")
st.title("💬 챗봇")
st.caption("자연어로 결제 시스템 상태를 질문하세요")

if "messages" not in st.session_state:
    st.session_state.messages = []

# 대화 이력 표시
for msg in st.session_state.messages:
    with st.chat_message(msg["role"]):
        st.write(msg["content"])

# 입력창
if prompt := st.chat_input("예: 지금 에러율이 얼마야? / 최근 장애 분석해줘"):
    st.session_state.messages.append({"role": "user", "content": prompt})
    with st.chat_message("user"):
        st.write(prompt)

    with st.chat_message("assistant"):
        with st.spinner("분석 중..."):
            try:
                res = requests.post(
                    f"{API}/agent/analyze",
                    json={"situation": prompt},
                    timeout=60
                )
                answer = res.json().get("result", "응답을 받지 못했습니다.")
            except:
                answer = "서버에 연결할 수 없습니다."

        st.write(answer)
        st.session_state.messages.append({"role": "assistant", "content": answer})