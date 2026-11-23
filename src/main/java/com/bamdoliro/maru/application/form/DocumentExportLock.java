package com.bamdoliro.maru.application.form;

import org.springframework.stereotype.Component;

import java.util.concurrent.Semaphore;

/**
 * 전체 자기소개서 PDF 생성이 서버 안에서 동시에 하나만 접수되도록 막는다.
 * 작업을 실행기에 넘기기 전에 얻고, 생성이 끝나면(성공/실패 모두) 놓는다.
 * 실행기에 들어가기 전에 막으므로 생성 작업끼리 실행기 스레드를 붙잡고 서로 기다리는 일은 없다.
 * 작업 API로 접수된 경우에는 그 작업 id를 기억해 두어, 다시 호출되면 같은 작업을 돌려줄 수 있게 한다.
 */
@Component
public class DocumentExportLock {

    private final Semaphore permit = new Semaphore(1);
    private volatile Long runningJobId;

    public boolean tryAcquire() {
        return permit.tryAcquire();
    }

    public void holdFor(Long jobId) {
        this.runningJobId = jobId;
    }

    public Long getRunningJobId() {
        return runningJobId;
    }

    public void release() {
        runningJobId = null;
        permit.release();
    }
}
