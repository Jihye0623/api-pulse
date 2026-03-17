package com.apipulse.api_pulse_app.agent.tools;

import com.apipulse.api_pulse_app.payment.PaymentService;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestartServiceTool {

    private final PaymentService paymentService;

    @Tool("카드 오류 모드를 비활성화하여 결제 서비스를 정상 상태로 복구합니다.")
    public String restoreCardErrorMode() {
        log.warn("[Tool] RestartService - 카드 오류 모드 해제");
        paymentService.setCardErrorMode(false);
        return "카드 오류 모드 해제 완료 - 결제 서비스 정상화됨";
    }

    @Tool("타임아웃 모드를 비활성화하여 결제 처리 지연을 해소합니다.")
    public String restoreTimeoutMode() {
        log.warn("[Tool] RestartService - 타임아웃 모드 해제");
        paymentService.setTimeoutMode(false);
        return "타임아웃 모드 해제 완료 - 결제 응답시간 정상화됨";
    }
}