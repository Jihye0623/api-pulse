import streamlit as st
import requests
from streamlit_autorefresh import st_autorefresh

API = "http://localhost:8080"

st.set_page_config(
    page_title="FinGuard",
    page_icon="🛡️",
    layout="wide"
)

st_autorefresh(interval=3000, key="finguard_refresh")

# -----------------------------
# session state
# -----------------------------
if "shown_alert_ids" not in st.session_state:
    st.session_state.shown_alert_ids = set()

if "just_read_all" not in st.session_state:
    st.session_state.just_read_all = False

# -----------------------------
# API helpers
# -----------------------------
def fetch_json(url, method="GET", timeout=3):
    try:
        if method == "GET":
            res = requests.get(url, timeout=timeout)
        else:
            res = requests.post(url, timeout=timeout)

        if res.status_code == 200:
            return res.json()
        return None
    except Exception:
        return None

def check_alerts():
    data = fetch_json(f"{API}/alerts/unread")
    return data if isinstance(data, list) else []

def get_history():
    data = fetch_json(f"{API}/payment/history")
    return data if isinstance(data, list) else []

def get_agent_logs():
    data = fetch_json(f"{API}/agent/logs")
    return data if isinstance(data, list) else []

def get_monitor_summary():
    data = fetch_json(f"{API}/monitor/summary")
    return data if isinstance(data, dict) else {}

# -----------------------------
# data load
# -----------------------------
alerts = check_alerts()
summary = get_monitor_summary()
history = get_history()   # 최근 결제 내역/타임라인 때문에 일단 유지
agent_logs = get_agent_logs()

if st.session_state.just_read_all:
    alerts = []
    st.session_state.just_read_all = False

# -----------------------------
# derived metrics
# -----------------------------
total = summary.get("totalCount", 0)
approved = summary.get("approvedCount", 0)
failed = summary.get("failedCount", 0)
error_rate = round(summary.get("errorRate", 0.0), 1)
latest_reason = summary.get("topFailReason", "-")
current_status = summary.get("status", "NORMAL")
summary_message = summary.get("message", "")

latest_log = agent_logs[0] if agent_logs else None

# -----------------------------
# page header
# -----------------------------
st.title("🛡️ 대시보드")
st.caption("AI 기본법 기반 금융 결제 API 장애 자동 대응 시스템")

st.divider()

# -----------------------------
# 1. 상태 배너
# -----------------------------
if current_status == "CRITICAL":
    st.error(
        f"🚨 **현재 상태: CRITICAL**  \n"
        f"- 최근 실패율: **{error_rate}%**  \n"
        f"- 주요 실패 사유: **{latest_reason}**  \n"
        f"- {summary_message}"
    )
elif current_status == "WARNING":
    st.warning(
        f"⚠️ **현재 상태: WARNING**  \n"
        f"- 최근 실패율: **{error_rate}%**  \n"
        f"- 주요 실패 사유: **{latest_reason}**  \n"
        f"- {summary_message}"
    )
else:
    st.success(
        f"✅ **현재 상태: NORMAL**  \n"
        f"- 최근 실패율: **{error_rate}%**  \n"
        f"- {summary_message}"
    )

st.divider()

# -----------------------------
# 2. 핵심 KPI
# -----------------------------
st.subheader("📊 핵심 운영 지표")

k1, k2, k3, k4 = st.columns(4)

k1.metric("총 결제 건수", f"{total}건")
k2.metric("승인", f"{approved}건")
k3.metric("실패", f"{failed}건")
k4.metric(
    "실패율",
    f"{error_rate}%",
    delta=f"{error_rate}%" if error_rate > 0 else None,
    delta_color="inverse"
)

st.divider()

# -----------------------------
# 3. 실시간 알림
# -----------------------------
st.subheader("🔔 실시간 알림")

new_alerts = []
for alert in alerts:
    alert_id = alert.get("id") or f"{alert.get('level', 'INFO')}::{alert.get('message', '')}"
    if alert_id not in st.session_state.shown_alert_ids:
        new_alerts.append((alert_id, alert))
        st.session_state.shown_alert_ids.add(alert_id)

for _, alert in new_alerts:
    level = alert.get("level", "INFO")
    message = alert.get("message", "")

    if level == "ESCALATION":
        st.toast(f"🚨 새 에스컬레이션: {message}", icon="🚨")
    elif level == "WARN":
        st.toast(f"⚠️ 새 경고: {message}", icon="⚠️")
    else:
        st.toast(f"ℹ️ 새 알림: {message}", icon="ℹ️")

if alerts:
    for alert in alerts[:3]:
        level = alert.get("level", "INFO")
        message = alert.get("message", "")

        if level == "ESCALATION":
            st.error(f"🚨 **[에스컬레이션]** {message}")
        elif level == "WARN":
            st.warning(f"⚠️ **[경고]** {message}")
        else:
            st.info(f"ℹ️ {message}")

    c1, c2 = st.columns([1, 6])
    with c1:
        if st.button("✅ 전체 읽음 처리"):
            res = fetch_json(f"{API}/alerts/read-all", method="POST")
            if res is not None:
                st.session_state.shown_alert_ids = set()
                st.session_state.just_read_all = True
                st.rerun()
            else:
                st.error("전체 읽음 처리 실패")
else:
    st.info("현재 미확인 알림이 없습니다.")

st.divider()

# -----------------------------
# 4. Agent 분석 결과
# -----------------------------
st.subheader("🤖 Agent 분석 결과")

if latest_log:
    a1, a2, a3 = st.columns(3)

    a1.metric("최근 트리거", latest_log.get("triggerType", "-"))
    a2.metric("응답 시간", f"{latest_log.get('responseTimeMs', 0)} ms")
    a3.metric("실행 시각", str(latest_log.get("createdAt", "-"))[:19])

    with st.container(border=True):
        st.markdown("### 📝 최근 분석 요약")
        st.write(latest_log.get("situation", "-"))

        st.markdown("### 🛠️ 사용 Tool")
        st.code(latest_log.get("toolsUsed", "-"))

        st.markdown("### 📄 Agent 응답")
        st.write(latest_log.get("agentResult", "-"))
else:
    st.info("아직 Agent 실행 이력이 없습니다.")

st.divider()

# -----------------------------
# 5. 장애 타임라인
# -----------------------------
st.subheader("⏱️ 장애 타임라인")

timeline_items = []

if total > 0:
    if failed > 0:
        timeline_items.append(f"🔴 장애 감지: 최근 결제 {total}건 중 실패 {failed}건")
        timeline_items.append(f"📌 주요 실패 사유: {latest_reason}")
    else:
        timeline_items.append("🟢 최근 결제 흐름 정상")

if latest_log:
    timeline_items.append(f"🤖 Agent 실행: {latest_log.get('triggerType', '-')}")
    timeline_items.append(f"🧠 분석 요청: {latest_log.get('situation', '-')}")
    timeline_items.append("📝 판단 이력 저장 완료")

if error_rate < 50 and total > 0:
    timeline_items.append("✅ 현재 정상 범위로 복귀")

if timeline_items:
    for item in timeline_items:
        st.write(item)
else:
    st.write("표시할 타임라인 데이터가 없습니다.")

st.divider()

# -----------------------------
# 6. 액션 버튼
# -----------------------------
st.subheader("⚡ 장애 시나리오 및 제어")

b1, b2, b3, b4 = st.columns(4)

with b1:
    if st.button("🔴 카드 오류 ON", use_container_width=True):
        res = fetch_json(f"{API}/scenario/card-error/on", method="POST")
        if res is not None:
            st.toast("카드 오류 모드 ON", icon="🔴")
            st.rerun()
        else:
            st.error("카드 오류 모드 ON 요청 실패")

with b2:
    if st.button("🟢 카드 오류 OFF", use_container_width=True):
        res = fetch_json(f"{API}/scenario/card-error/off", method="POST")
        if res is not None:
            st.toast("카드 오류 모드 OFF", icon="🟢")
            st.rerun()
        else:
            st.error("카드 오류 모드 OFF 요청 실패")

with b3:
    if st.button("🤖 Agent 분석 실행", use_container_width=True):
        with st.spinner("Agent 분석 중..."):
            res = requests.post(f"{API}/agent/analyze/card-error", timeout=60)
            if res.status_code == 200:
                st.toast("Agent 분석 완료!", icon="🤖")
                st.rerun()
            else:
                st.error("Agent 분석 실패")

with b4:
    if st.button("🔍 모니터링 체크", use_container_width=True):
        data = fetch_json(f"{API}/monitor/check", method="POST", timeout=60)
        if data is not None:
            if data.get("detected"):
                st.toast(f"장애 의심 상태 감지: 실패율 {data.get('errorRate', 0)}%", icon="🚨")
            else:
                st.toast(f"정상 상태: 실패율 {data.get('errorRate', 0)}%", icon="✅")
            st.rerun()
        else:
            st.error("모니터링 체크 요청 실패")