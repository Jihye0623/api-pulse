import streamlit as st
import requests
import time

API = "http://localhost:8080"

st.set_page_config(
    page_title="FinGuard",
    page_icon="🛡️",
    layout="wide"
)

st.title("🛡️ FinGuard 대시보드")
st.caption("AI 기본법 기반 금융 결제 API 장애 자동 대응 시스템")

st.divider()

# 실시간 알림 배너 (3초마다 폴링)
def check_alerts():
    try:
        res = requests.get(f"{API}/alerts/unread", timeout=3)
        return res.json() if res.status_code == 200 else []
    except:
        return []

alerts = check_alerts()

if alerts:
    for alert in alerts:
        if alert["level"] == "ESCALATION":
            st.error(f"🚨 **[에스컬레이션]** {alert['message']}")
        elif alert["level"] == "WARN":
            st.warning(f"⚠️ **[경고]** {alert['message']}")
        else:
            st.info(f"ℹ️ {alert['message']}")

    col1, col2 = st.columns([1, 5])
    with col1:
        if st.button("✅ 전체 읽음 처리"):
            requests.post(f"{API}/alerts/read-all")
            st.rerun()
else:
    st.success("✅ 현재 장애 알림 없음 — 시스템 정상 운영 중")

st.divider()

# 실시간 지표 요약
col1, col2, col3, col4 = st.columns(4)

try:
    history = requests.get(f"{API}/payment/history", timeout=3).json()
    total = len(history)
    approved = sum(1 for p in history if p["status"] == "APPROVED")
    failed = sum(1 for p in history if p["status"] == "FAILED")
    error_rate = round(failed / total * 100, 1) if total > 0 else 0
except:
    total = approved = failed = 0
    error_rate = 0.0

col1.metric("총 결제 건수", f"{total}건")
col2.metric("승인", f"{approved}건")
col3.metric("실패", f"{failed}건")
col4.metric("실패율", f"{error_rate}%",
    delta=f"{error_rate}%" if error_rate > 0 else None,
    delta_color="inverse"
)

st.divider()

# 장애 시나리오 트리거 버튼
st.subheader("⚡ 장애 시나리오")

col1, col2, col3, col4 = st.columns(4)

with col1:
    if st.button("🔴 카드 오류 ON"):
        requests.post(f"{API}/scenario/card-error/on")
        st.toast("카드 오류 모드 ON")

with col2:
    if st.button("🟢 카드 오류 OFF"):
        requests.post(f"{API}/scenario/card-error/off")
        st.toast("카드 오류 모드 OFF")

with col3:
    if st.button("🤖 Agent 분석 실행"):
        with st.spinner("Agent 분석 중..."):
            res = requests.post(f"{API}/agent/analyze/card-error", timeout=60)
            if res.status_code == 200:
                st.toast("Agent 분석 완료!")
                st.rerun()

with col4:
    if st.button("🔍 모니터링 체크"):
        res = requests.post(f"{API}/monitor/check", timeout=60)
        if res.status_code == 200:
            data = res.json()
            if data.get("detected"):
                st.toast(f"장애 감지! 에러율 {data['errorRate']}%", icon="🚨")
            else:
                st.toast(f"정상. 에러율 {data['errorRate']}%", icon="✅")

# 자동 새로고침
time.sleep(3)
st.rerun()