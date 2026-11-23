package com.bamdoliro.maru.application.form;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentExportLockTest {

    private final DocumentExportLock lock = new DocumentExportLock();

    @Test
    void 잠금을_얻은_동안에는_다시_얻을_수_없고_놓으면_다시_얻을_수_있다() {
        assertTrue(lock.tryAcquire());
        assertFalse(lock.tryAcquire());

        lock.release();

        assertTrue(lock.tryAcquire());
    }

    @Test
    void 놓으면_기억한_작업_id도_지운다() {
        assertTrue(lock.tryAcquire());
        lock.holdFor(5L);
        assertEquals(5L, lock.getRunningJobId());

        lock.release();

        assertNull(lock.getRunningJobId());
    }
}
