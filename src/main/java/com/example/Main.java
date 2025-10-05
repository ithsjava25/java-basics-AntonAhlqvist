package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        // Programmet använder svensk locale för att alltid skriva priser i rätt format (t.ex. "12,34 öre").
        Locale.setDefault(new Locale("sv", "SE"));
        ElpriserAPI api = new ElpriserAPI();

        UserInput input = parseUserInput(args);
        if (input == null) {
            return;
        }

        ElpriserAPI.Prisklass priceClass = getPriceClass(input.getZone());
        if (priceClass == null) {
            return;
        }

        LocalDate parsedDate = parseDate(input.getDate());
        if (parsedDate == null) {
            return;
        }

        int hours = input.getChargingHours();
        boolean chargingEnabled = hours > 0;

        processAndPrintPrices(parsedDate, priceClass, input.getZone(), api, input.isSorted(), chargingEnabled, hours);
    }

    public static UserInput parseUserInput(String[] args) {
        String zone = null;
        String date = null;
        boolean sorted = false;
        int chargingHours = 0;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help" -> {
                    printHelpInfo();
                    return null;
                }
                case "--zone" -> {
                    if (i + 1 < args.length) {
                        zone = args[i + 1].toUpperCase();
                        i = i + 1;
                    } else {
                        System.out.println("Du måste ange en zon efter --zone (välj SE1-SE4)");
                        printHelpInfo();
                        return null;
                    }
                }
                case "--date" -> {
                    if (i + 1 < args.length) {
                        date = args[i + 1];
                        i = i + 1;
                    } else {
                        System.out.println();
                        System.out.println("Du måste ange ett datum efter --date (skriv enligt format YYYY-MM-DD)");
                        System.out.println();
                        printHelpInfo();
                        return null;
                    }
                }
                case "--sorted" -> sorted = true;
                case "--charging" -> {
                    if (i + 1 < args.length) {
                        String ch = args[i + 1].toLowerCase();
                        i = i + 1;
                        if (ch.equals("2h")) {
                            chargingHours = 2;
                        } else if (ch.equals("4h")) {
                            chargingHours = 4;
                        } else if (ch.equals("8h")) {
                            chargingHours = 8;
                        } else {
                            System.out.println("Ogiltig laddningstid (välj 2h, 4h eller 8h)");
                            return null;
                        }
                    } else {
                        System.out.println("Du måste ange laddningstid efter --charging (välj 2h, 4h eller 8h)");
                        return null;
                    }
                }
            }
        }

        if (zone == null) {
            System.out.println("Du måste skriva --zone (välj SE1-SE4)");
            printHelpInfo();
            return null;
        }

        if (date == null) {
            date = LocalDate.now().toString();
        }

        return new UserInput(zone, date, sorted, chargingHours);
    }

    public static void printHelpInfo() {
        System.out.println("Usage: java -cp target/classes com.example.Main [options]");
        System.out.println();
        System.out.println("=== Hjälp ===");
        System.out.println();
        System.out.println("--zone SE1|SE2|SE3|SE4   (obligatoriskt)");
        System.out.println("--date YYYY-MM-DD        (valfritt, använder dagens datum om inget annat anges)");
        System.out.println("--sorted                 (valfritt, sorterar efter pris)");
        System.out.println("--charging 2h|4h|8h      (valfritt, används för att hitta det optimala laddningsfönstret)");
        System.out.println("--help                   (visar denna hjälptext)");
    }

    public static LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            System.out.println("Ogiltigt datum (skriv enligt format YYYY-MM-DD)");
            printHelpInfo();
            return null;
        }
    }

    public static ElpriserAPI.Prisklass getPriceClass(String zone) {
        switch (zone) {
            case "SE1" -> {
                return ElpriserAPI.Prisklass.SE1;
            }
            case "SE2" -> {
                return ElpriserAPI.Prisklass.SE2;
            }
            case "SE3" -> {
                return ElpriserAPI.Prisklass.SE3;
            }
            case "SE4" -> {
                return ElpriserAPI.Prisklass.SE4;
            }
            default -> {
                System.out.println("Ogiltig zon (välj SE1-SE4)");
                return null;
            }
        }
    }

    public static void processAndPrintPrices(LocalDate parsedDate, ElpriserAPI.Prisklass priceClass,
                                             String zone, ElpriserAPI api,
                                             boolean sorted, boolean chargingEnabled, int chargingHours) {

        List<ElpriserAPI.Elpris> todaysPrices = fetchTodaysPrices(parsedDate, priceClass, api);
        List<ElpriserAPI.Elpris> tomorrowsPrices = fetchTomorrowsPrices(parsedDate, priceClass, api);

        printPricesAndStatistics(todaysPrices, zone, parsedDate.toString(), sorted, "Dagens");

        if (!sorted && !tomorrowsPrices.isEmpty()) {
            printPricesAndStatistics(tomorrowsPrices, zone, parsedDate.plusDays(1).toString(), false, "Morgondagens");
        }

        if (chargingEnabled && chargingHours > 0) {
            List<ElpriserAPI.Elpris> combined = new ArrayList<ElpriserAPI.Elpris>(todaysPrices);
            combined.addAll(tomorrowsPrices);
            calculateChargingWindow(combined, chargingHours);
        }
    }

    public static List<ElpriserAPI.Elpris> fetchTodaysPrices(LocalDate parsedDate, ElpriserAPI.Prisklass priceClass, ElpriserAPI api) {
        LocalDate today = LocalDate.now();
        List<ElpriserAPI.Elpris> todaysPrices = api.getPriser(parsedDate.toString(), priceClass);

        if (parsedDate.equals(today) && LocalTime.now().getHour() < 13) {
            System.out.println("Du får vänta tills efter kl 13 för att få morgondagens priser, skriver endast ut dagens priser:");
            if (todaysPrices.size() == 96) {
                todaysPrices = convertQuarterlyToHourlyPrices(todaysPrices);
            }
            return todaysPrices;
        }

        if (todaysPrices.size() == 96) {
            todaysPrices = convertQuarterlyToHourlyPrices(todaysPrices);
        }
        return todaysPrices;
    }

    public static List<ElpriserAPI.Elpris> fetchTomorrowsPrices(LocalDate parsedDate, ElpriserAPI.Prisklass priceClass, ElpriserAPI api) {
        List<ElpriserAPI.Elpris> tomorrowsPrices = api.getPriser(parsedDate.plusDays(1).toString(), priceClass);

        if (tomorrowsPrices.size() == 96) {
            tomorrowsPrices = convertQuarterlyToHourlyPrices(tomorrowsPrices);
        }
        return tomorrowsPrices;
    }

    public static void printPricesAndStatistics(List<ElpriserAPI.Elpris> prices, String zone, String date, boolean sorted, String label) {
        if (prices == null || prices.isEmpty()) {
            System.out.println("Inga priser (" + label.toLowerCase() + ") för " + date + " i zon " + zone + " – ingen data.");
            return;
        }

        System.out.println();
        System.out.println("=== " + label + " priser (" + date + ", zon " + zone + ") ===");

        List<ElpriserAPI.Elpris> toPrint = new ArrayList<>(prices);

        if (sorted) {
            sortPricesAscending(toPrint);
        }

        printPriceList(toPrint, zone, date, sorted);
        System.out.println(); // luft före statistik
        printPriceStatistics(prices, label);
    }

    public static void calculateChargingWindow(List<ElpriserAPI.Elpris> prices, int hours) {
        calculateOptimalChargingWindow(prices, hours);
    }

    public static void sortPricesAscending(List<ElpriserAPI.Elpris> prices) {
        if (prices == null || prices.size() < 2) {
            return;
        }
        prices.sort(Comparator.comparing(ElpriserAPI.Elpris::sekPerKWh));
    }

    public static List<ElpriserAPI.Elpris> convertQuarterlyToHourlyPrices(List<ElpriserAPI.Elpris> quarters) {
        if (quarters.size() != 96) {
            throw new IllegalArgumentException("Ogiltigt antal kvartar (måste vara 96)");
        }

        List<ElpriserAPI.Elpris> hourly = new ArrayList<ElpriserAPI.Elpris>();
        for (int i = 0; i < 24; i++) {
            int startIndex = i * 4;
            int endIndex = startIndex + 4;
            double sum = 0.0;
            ZonedDateTime startTime = quarters.get(startIndex).timeStart();
            ZonedDateTime endTime = quarters.get(endIndex - 1).timeEnd();

            for (int j = startIndex; j < endIndex; j++) {
                sum = sum + quarters.get(j).sekPerKWh();
            }

            double avg = sum / 4.0;
            hourly.add(new ElpriserAPI.Elpris(avg, 0.0, 0.0, startTime, endTime));
        }

        return hourly;
    }

    public static void printPriceList(List<ElpriserAPI.Elpris> prices, String zone, String date, boolean sorted) {
        if (prices == null || prices.isEmpty()) {
            System.out.println("Inga priser hittades för " + date + " i zon " + zone);
            return;
        }

        if (sorted) {
            System.out.println("(Priserna är sorterade från det lägsta till det högsta)");
            System.out.println();
        } else {
            System.out.println("(Priserna visas i tidsordning)");
            System.out.println();
        }

        Locale sv = new Locale("sv", "SE");
        for (int i = 0; i < prices.size(); i++) {
            ElpriserAPI.Elpris elpris = prices.get(i);
            int startHour = elpris.timeStart().getHour();
            int endHour = elpris.timeEnd().getHour();

            String hourRange = "";
            if (startHour < 10) {
                hourRange = "0" + startHour + "-";
            } else {
                hourRange = startHour + "-";
            }
            if (endHour < 10) {
                hourRange = hourRange + "0" + endHour;
            } else {
                hourRange = hourRange + endHour;
            }

            double ore = elpris.sekPerKWh() * 100;
            System.out.printf("%s %s öre%n", hourRange, String.format(sv, "%.2f", ore));
        }
    }

    public static void printPriceStatistics(List<ElpriserAPI.Elpris> prices, String label) {
        if (prices == null || prices.isEmpty()) {
            return;
        }

        double sum = 0.0;
        ElpriserAPI.Elpris min = prices.getFirst();
        ElpriserAPI.Elpris max = prices.getFirst();

        for (int i = 0; i < prices.size(); i++) {
            ElpriserAPI.Elpris p = prices.get(i);
            double ore = p.sekPerKWh() * 100;
            sum = sum + ore;

            if (p.sekPerKWh() < min.sekPerKWh()) {
                min = p;
            } else if (p.sekPerKWh() > max.sekPerKWh()) {
                max = p;
            }
        }

        double avg = sum / prices.size();

        int minStart = min.timeStart().getHour();
        int minEnd = min.timeEnd().getHour();
        int maxStart = max.timeStart().getHour();
        int maxEnd = max.timeEnd().getHour();

        String minRange = "";
        if (minStart < 10) {
            minRange = "0" + minStart + "-";
        } else {
            minRange = minStart + "-";
        }
        if (minEnd < 10) {
            minRange = minRange + "0" + minEnd;
        } else {
            minRange = minRange + minEnd;
        }

        String maxRange = "";
        if (maxStart < 10) {
            maxRange = "0" + maxStart + "-";
        } else {
            maxRange = maxStart + "-";
        }
        if (maxEnd < 10) {
            maxRange = maxRange + "0" + maxEnd;
        } else {
            maxRange = maxRange + maxEnd;
        }

        Locale sv = new Locale("sv", "SE");
        System.out.println("=== " + label + " Statistik ===");
        System.out.println();
        System.out.println("Högsta pris: " + maxRange + " " + String.format(sv, "%.2f", max.sekPerKWh() * 100) + " öre");
        System.out.println("Lägsta pris: " + minRange + " " + String.format(sv, "%.2f", min.sekPerKWh() * 100) + " öre");
        System.out.println("Medelpris: " + String.format(sv, "%.2f", avg) + " öre");
    }

    public static void calculateOptimalChargingWindow(List<ElpriserAPI.Elpris> prices, int hours) {
        if (prices == null || prices.isEmpty()) {
            return;
        }
        if (prices.size() < hours) {
            System.out.println("Påbörja laddning kunde inte beräknas – inte tillräckligt med priser.");
            return;
        }

        double minSum = Double.MAX_VALUE;
        int minIndex = 0;

        for (int i = 0; i <= prices.size() - hours; i++) {
            double sum = 0;
            for (int j = 0; j < hours; j++) {
                sum = sum + prices.get(i + j).sekPerKWh();
            }
            if (sum < minSum) {
                minSum = sum;
                minIndex = i;
            }
        }

        System.out.println();
        System.out.println("=== Optimalt laddningsfönster ===");
        System.out.println("(Påbörja laddning för " + hours + " timmar)");
        System.out.println();

        Locale sv = new Locale("sv", "SE");
        double totalPrice = 0;

        for (int i = minIndex; i < minIndex + hours; i++) {
            ElpriserAPI.Elpris p = prices.get(i);

            String date = p.timeStart().toLocalDate().toString();
            int startHour = p.timeStart().getHour();
            int startMinute = p.timeStart().getMinute();
            int endHour = p.timeEnd().getHour();
            int endMinute = p.timeEnd().getMinute();

            String start = "";
            if (startHour < 10) {
                start = "0" + startHour + ":";
            } else {
                start = startHour + ":";
            }
            if (startMinute < 10) {
                start = start + "0" + startMinute;
            } else {
                start = start + startMinute;
            }

            String end = "";
            if (endHour < 10) {
                end = "0" + endHour + ":";
            } else {
                end = endHour + ":";
            }
            if (endMinute < 10) {
                end = end + "0" + endMinute;
            } else {
                end = end + endMinute;
            }

            double ore = p.sekPerKWh() * 100;
            totalPrice = totalPrice + ore;

            System.out.println(date + " kl " + start + "-" + end + ": " + String.format(sv, "%.2f", ore) + " öre");
        }

        System.out.println("Totalt pris för fönstret: " + String.format(sv, "%.2f", totalPrice) + " öre");
        System.out.println("Medelpris för fönster: " + String.format(sv, "%.2f", totalPrice / hours) + " öre");
    }

    public static class UserInput {
        private String zone;
        private String date;
        private boolean sorted;
        private int chargingHours;

        public UserInput(String zone, String date, boolean sorted, int chargingHours) {
            this.zone = zone;
            this.date = date;
            this.sorted = sorted;
            this.chargingHours = chargingHours;
        }

        public String getZone() {
            return this.zone;
        }

        public String getDate() {
            return this.date;
        }

        public boolean isSorted() {
            return this.sorted;
        }

        public int getChargingHours() {
            return this.chargingHours;
        }
    }
}