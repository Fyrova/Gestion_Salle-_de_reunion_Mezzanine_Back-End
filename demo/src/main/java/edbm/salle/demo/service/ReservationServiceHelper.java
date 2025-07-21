package edbm.salle.demo.service;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class ReservationServiceHelper {

    /**
     * Returns the date of the nth weekday of a given month and year.
     * For example, the 1st Tuesday of July 2025.
     * If nth is negative, counts from the end of the month (-1 means last).
     * Returns null if the nth weekday does not exist in the month.
     */
    public static LocalDate getNthWeekdayOfMonth(int year, int month, int nth, DayOfWeek dayOfWeek) {
        if (nth == 0) {
            throw new IllegalArgumentException("nth must not be zero");
        }
        LocalDate date;
        if (nth > 0) {
            // Start from the first day of the month
            date = LocalDate.of(year, month, 1);
            // Find first occurrence of the dayOfWeek in the month
            int dayDiff = dayOfWeek.getValue() - date.getDayOfWeek().getValue();
            if (dayDiff < 0) {
                dayDiff += 7;
            }
            date = date.plusDays(dayDiff);
            // Add (nth-1) weeks
            date = date.plusWeeks(nth - 1);
            if (date.getMonthValue() != month) {
                return null; // nth weekday does not exist in this month
            }
            return date;
        } else {
            // nth < 0, count from end of month
            date = LocalDate.of(year, month, LocalDate.of(year, month, 1).lengthOfMonth());
            int dayDiff = date.getDayOfWeek().getValue() - dayOfWeek.getValue();
            if (dayDiff < 0) {
                dayDiff += 7;
            }
            date = date.minusDays(dayDiff);
            date = date.minusWeeks(-nth - 1);
            if (date.getMonthValue() != month) {
                return null;
            }
            return date;
        }
    }
}
