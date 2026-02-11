package com.jobpulse.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;
import org.quartz.CronExpression;

public class CronExpressionUtil {

    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();

 
    public static boolean isValidCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            return false;
        }
        try {
            CronExpression.isValidExpression(cronExpression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public static void validateCronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.isBlank()) {
            throw new IllegalArgumentException("Cron expression cannot be null or empty");
        }
        if (!CronExpression.isValidExpression(cronExpression)) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression);
        }
    }

    public static LocalDateTime getNextRunTime(String cronExpression, LocalDateTime fromDateTime) {
        validateCronExpression(cronExpression);
        
        try {
            CronExpression cron = new CronExpression(cronExpression);
            cron.setTimeZone(DEFAULT_TIMEZONE);
            
            java.util.Date fromDate = java.util.Date.from(
                fromDateTime.atZone(ZoneId.systemDefault()).toInstant()
            );
            java.util.Date nextDate = cron.getNextValidTimeAfter(fromDate);
            
            if (nextDate == null) {
                throw new IllegalArgumentException("Unable to calculate next run time for cron expression: " + cronExpression);
            }
            
            return LocalDateTime.ofInstant(
                nextDate.toInstant(),
                ZoneId.systemDefault()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing cron expression: " + e.getMessage(), e);
        }
    }


    public static LocalDateTime getNextRunTime(String cronExpression, LocalDateTime fromDateTime, TimeZone timeZone) {
        validateCronExpression(cronExpression);
        
        try {
            CronExpression cron = new CronExpression(cronExpression);
            cron.setTimeZone(timeZone != null ? timeZone : DEFAULT_TIMEZONE);
            
            java.util.Date fromDate = java.util.Date.from(
                fromDateTime.atZone(ZoneId.systemDefault()).toInstant()
            );
            java.util.Date nextDate = cron.getNextValidTimeAfter(fromDate);
            
            if (nextDate == null) {
                throw new IllegalArgumentException("Unable to calculate next run time for cron expression: " + cronExpression);
            }
            
            return LocalDateTime.ofInstant(
                nextDate.toInstant(),
                ZoneId.systemDefault()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parsing cron expression: " + e.getMessage(), e);
        }
    }


    public static String generateCronExpression(String frequency, Integer interval) {
        if (frequency == null || interval == null || interval <= 0) {
            throw new IllegalArgumentException("Frequency and interval must be valid");
        }

        return switch (frequency.toUpperCase()) {
            case "MINUTES" -> "0 */" + interval + " * * * *";        // Every X minutes
            case "HOURS" -> "0 0 */" + interval + " * * *";           // Every X hours
            case "DAYS" -> "0 0 0 */" + interval + " * *";            // Every X days
            case "WEEKS" -> "0 0 0 ? * MON";                          // Every week (Monday)
            case "MONTHS" -> "0 0 0 1 * *";                           // Every month (1st day)
            default -> throw new IllegalArgumentException("Unsupported frequency: " + frequency);
        };
    }
}
