package com.jobpulse.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class CronExpressionUtilTest {

    @Test
    void validatesCronExpressions() {
        assertTrue(CronExpressionUtil.isValidCronExpression("0 0/5 * ? * *"));
        assertFalse(CronExpressionUtil.isValidCronExpression(""));
        assertFalse(CronExpressionUtil.isValidCronExpression(null));
        assertFalse(CronExpressionUtil.isValidCronExpression("invalid cron"));
    }

    @Test
    void getNextRunTimeReturnsFutureDate() {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime next = CronExpressionUtil.getNextRunTime("0 0/5 * ? * *", from);

        assertNotNull(next);
        assertTrue(next.isAfter(from));
    }

    @Test
    void getNextRunTimeThrowsForInvalidCron() {
        assertThrows(IllegalArgumentException.class,
                () -> CronExpressionUtil.getNextRunTime("invalid", LocalDateTime.now()));
    }

    @Test
    void generatesCronByFrequency() {
        assertEquals("0 */15 * * * *", CronExpressionUtil.generateCronExpression("MINUTES", 15));
        assertEquals("0 0 */2 * * *", CronExpressionUtil.generateCronExpression("HOURS", 2));
        assertEquals("0 0 0 */1 * *", CronExpressionUtil.generateCronExpression("DAYS", 1));
    }

    @Test
    void generateCronThrowsForUnsupportedFrequency() {
        assertThrows(IllegalArgumentException.class,
                () -> CronExpressionUtil.generateCronExpression("YEARS", 1));
    }
}
