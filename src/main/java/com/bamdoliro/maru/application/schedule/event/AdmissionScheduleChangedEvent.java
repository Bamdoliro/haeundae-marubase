package com.bamdoliro.maru.application.schedule.event;

/**
 * 입학 일정이 등록되거나 수정됐을 때 발행한다. 커밋 후 1차 합격자 선발 예약을 다시 잡는 데 쓴다.
 */
public record AdmissionScheduleChangedEvent(Long scheduleId) {
}
