package com.gsecp.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class RTest {

    @Test
    void okShouldCarrySuccessCodeAndData() {
        R<String> result = R.ok("pong");
        assertEquals(200, result.getCode());
        assertEquals("pong", result.getData());
    }

    @Test
    void failShouldCarryBusinessCode() {
        R<Void> result = R.fail(ResultCode.FORBIDDEN);
        assertEquals(403, result.getCode());
        assertNull(result.getData());
    }
}
