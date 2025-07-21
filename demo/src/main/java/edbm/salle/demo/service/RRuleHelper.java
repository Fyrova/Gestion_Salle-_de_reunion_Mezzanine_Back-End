package edbm.salle.demo.service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

public class RRuleHelper {
    private static final Map<String, String> FREQ_TRANSLATIONS = Map.of(
        "DAILY", "Quotidienne",
        "WEEKLY", "Hebdomadaire",
        "MONTHLY", "Mensuelle",
        "YEARLY", "Annuelle"
    );
    
    private static final Map<String, String> DAY_TRANSLATIONS = Map.of(
        "MO", "Lundi", "TU", "Mardi", "WE", "Mercredi",
        "TH", "Jeudi", "FR", "Vendredi", "SA", "Samedi", "SU", "Dimanche"
    );
    
    private static final Map<String, String> POSITION_TRANSLATIONS = Map.of(
        "1", "Premier", "2", "Deuxième", "3", "Troisième",
        "4", "Quatrième", "-1", "Dernier"
    );

    public static Map<String, String> parseRRule(String rrule) {
        Map<String, String> ruleParts = new HashMap<>();
        if (rrule == null || rrule.isEmpty()) return ruleParts;
        
        Arrays.stream(rrule.split(";"))
              .map(part -> part.split("="))
              .filter(keyValue -> keyValue.length == 2)
              .forEach(keyValue -> ruleParts.put(keyValue[0], keyValue[1]));
        
        return ruleParts;
    }

    public static int calculateOccurrenceCount(LocalDate startDate, Map<String, String> ruleParts) {
        if (ruleParts.containsKey("COUNT")) {
            return Integer.parseInt(ruleParts.get("COUNT"));
        }
        
        if (ruleParts.containsKey("UNTIL")) {
            String untilStr = ruleParts.get("UNTIL").split("T")[0];
            LocalDate untilDate = parseUntilDate(untilStr);
            
            String freq = ruleParts.getOrDefault("FREQ", "DAILY");
            int interval = Integer.parseInt(ruleParts.getOrDefault("INTERVAL", "1"));
            
            switch (freq) {
                case "DAILY": return (int) ChronoUnit.DAYS.between(startDate, untilDate) / interval + 1;
                case "WEEKLY": return (int) ChronoUnit.WEEKS.between(startDate, untilDate) / interval + 1;
                case "MONTHLY": return (int) ChronoUnit.MONTHS.between(startDate, untilDate) / interval + 1;
                case "YEARLY": return (int) ChronoUnit.YEARS.between(startDate, untilDate) / interval + 1;
                default: return 1;
            }
        }
        
        return 12; // Default to 12 occurrences if COUNT or UNTIL not specified
    }

    public static LocalDate calculateNextDate(LocalDate currentDate, String freq, int interval, Map<String, String> ruleParts) {
        switch (freq) {
            case "DAILY":
                return currentDate.plusDays(interval);
                
            case "WEEKLY":
                if (ruleParts.containsKey("BYDAY")) {
                    return calculateNextWeeklyDate(currentDate, interval, ruleParts.get("BYDAY"));
                }
                return currentDate.plusWeeks(interval);
                
            case "MONTHLY":
                if (ruleParts.containsKey("BYDAY") && ruleParts.containsKey("BYSETPOS")) {
                    return calculateMonthlyByPosition(currentDate, interval, ruleParts);
                }
                return currentDate.plusMonths(interval);
                
            case "YEARLY":
                return currentDate.plusYears(interval);
                
            default:
                return currentDate.plusDays(1);
        }
    }

    private static LocalDate calculateNextWeeklyDate(LocalDate currentDate, int interval, String byDay) {
        String[] days = byDay.split(",");
        List<DayOfWeek> targetDays = Arrays.stream(days)
                                          .map(RRuleHelper::parseDayOfWeek)
                                          .collect(Collectors.toList());
        
        LocalDate nextDate = currentDate.plusDays(1);
        int weeksAdded = 0;
        
        while (weeksAdded < interval) {
            if (targetDays.contains(nextDate.getDayOfWeek())) {
                return nextDate;
            }
            
            nextDate = nextDate.plusDays(1);
            
            if (nextDate.getDayOfWeek() == DayOfWeek.MONDAY) {
                weeksAdded++;
            }
        }
        
        return currentDate.plusWeeks(interval);
    }

    private static LocalDate calculateMonthlyByPosition(LocalDate currentDate, int interval, Map<String, String> ruleParts) {
        int setPos = Integer.parseInt(ruleParts.get("BYSETPOS"));
        DayOfWeek dayOfWeek = parseDayOfWeek(ruleParts.get("BYDAY"));
        
        LocalDate nextMonth = currentDate.plusMonths(interval);
        return ReservationServiceHelper.getNthWeekdayOfMonth(
            nextMonth.getYear(),
            nextMonth.getMonthValue(),
            setPos,
            dayOfWeek
        );
    }

    public static String buildRecurrenceDetails(String rrule) {
        Map<String, String> ruleParts = parseRRule(rrule);
        StringBuilder details = new StringBuilder("\n=== Détails de récurrence ===\n");
        
        // Type and interval
        String freq = ruleParts.getOrDefault("FREQ", "DAILY");
        details.append("Type: ").append(translateFrequency(freq)).append("\n");
        details.append("Intervalle: ").append(ruleParts.getOrDefault("INTERVAL", "1")).append("\n");
        
        // Specific days for weekly recurrence
        if (ruleParts.containsKey("BYDAY")) {
            details.append("Jours: ").append(translateDays(ruleParts.get("BYDAY"))).append("\n");
        }
        
        // Position for monthly recurrence
        if (ruleParts.containsKey("BYSETPOS")) {
            details.append("Position: ").append(translatePosition(ruleParts.get("BYSETPOS"))).append("\n");
        }
        
        // End date or count
        if (ruleParts.containsKey("UNTIL")) {
            details.append("Jusqu'au: ").append(formatUntilDate(ruleParts.get("UNTIL"))).append("\n");
        } else if (ruleParts.containsKey("COUNT")) {
            details.append("Nombre d'occurrences: ").append(ruleParts.get("COUNT")).append("\n");
        }
        
        return details.toString();
    }

    public static DayOfWeek parseDayOfWeek(String dayStr) {
        switch (dayStr) {
            case "MO": return DayOfWeek.MONDAY;
            case "TU": return DayOfWeek.TUESDAY;
            case "WE": return DayOfWeek.WEDNESDAY;
            case "TH": return DayOfWeek.THURSDAY;
            case "FR": return DayOfWeek.FRIDAY;
            case "SA": return DayOfWeek.SATURDAY;
            case "SU": return DayOfWeek.SUNDAY;
            default: return DayOfWeek.MONDAY;
        }
    }

    private static LocalDate parseUntilDate(String untilStr) {
        String datePart = untilStr.split("T")[0];
        return LocalDate.parse(datePart, DateTimeFormatter.BASIC_ISO_DATE);
    }

    private static String translateFrequency(String freq) {
        return FREQ_TRANSLATIONS.getOrDefault(freq, freq);
    }

    private static String translateDays(String days) {
        return Arrays.stream(days.split(","))
                     .map(day -> DAY_TRANSLATIONS.getOrDefault(day, day))
                     .collect(Collectors.joining(", "));
    }

    private static String translatePosition(String pos) {
        return POSITION_TRANSLATIONS.getOrDefault(pos, pos);
    }

    private static String formatUntilDate(String until) {
        try {
            String datePart = until.split("T")[0];
            LocalDate date = LocalDate.parse(datePart, DateTimeFormatter.BASIC_ISO_DATE);
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return until;
        }
    }

    public static Map<String, String> getDaysOfWeek() {
        return new LinkedHashMap<>(DAY_TRANSLATIONS);
    }
}
