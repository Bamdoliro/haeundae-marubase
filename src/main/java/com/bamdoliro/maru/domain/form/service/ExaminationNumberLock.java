package com.bamdoliro.maru.domain.form.service;

/**
 * 수험번호 부여와 원서 저장을 트랜잭션 단위로 직렬화하기 위한 락.
 * 호출한 트랜잭션이 끝날 때(커밋/롤백) 자동으로 해제되어야 한다.
 */
public interface ExaminationNumberLock {

    void lock();
}
