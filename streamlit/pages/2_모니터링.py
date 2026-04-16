import streamlit as st
import requests
import pandas as pd
import plotly.express as px

API = "http://localhost:8080"

st.set_page_config(page_title="모니터링", layout="wide")
st.title("📊 실시간 결제 모니터링")

try:
    history = requests.get(f"{API}/payment/history", timeout=3).json()
except:
    history = []
    st.error("Spring Boot 서버에 연결할 수 없습니다.")

if history:
    df = pd.DataFrame(history)
    df["createdAt"] = pd.to_datetime(df["createdAt"])

    # 상태별 분포 차트
    col1, col2 = st.columns(2)

    with col1:
        st.subheader("결제 상태 분포")
        status_count = df["status"].value_counts().reset_index()
        status_count.columns = ["status", "count"]
        color_map = {"APPROVED": "#1D9E75", "FAILED": "#E24B4A", "CANCELLED": "#BA7517"}
        fig = px.pie(status_count, names="status", values="count",
                     color="status", color_discrete_map=color_map)
        st.plotly_chart(fig, use_container_width=True)

    with col2:
        st.subheader("실패 사유")
        failed_df = df[df["status"] == "FAILED"]
        if not failed_df.empty:
            reason_count = failed_df["failReason"].value_counts().reset_index()
            reason_count.columns = ["reason", "count"]
            fig2 = px.bar(reason_count, x="reason", y="count",
                          color_discrete_sequence=["#E24B4A"])
            st.plotly_chart(fig2, use_container_width=True)
        else:
            st.info("실패 건수 없음")

    # 최근 결제 내역 테이블
    st.subheader("최근 결제 내역")
    display_df = df[["id", "cardNumber", "amount", "merchantName",
                     "status", "failReason", "createdAt"]].copy()
    display_df.columns = ["ID", "카드번호", "금액", "가맹점", "상태", "실패사유", "일시"]
    st.dataframe(display_df, use_container_width=True)

else:
    st.info("결제 데이터가 없습니다.")