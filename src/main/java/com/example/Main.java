package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main {

    public static void main(String[] args) {
        Locale.setDefault(new Locale("sv", "SE"));
        ElpriserAPI api = new ElpriserAPI();

        UserInput input = checkArguments(args);
        if (input == null) {
            return;
        }

        ElpriserAPI.Prisklass prisklass = getPrisklass(input.zone());
        if (prisklass == null) {
            return;
        }

        LocalDate parsedDate = checkDate(input.date());
        if (parsedDate == null) {
            return;
        }

        int hours = input.chargingHours();
        boolean chargingEnabled = false;
        if (hours > 0) {
            chargingEnabled = true;
        }

        checkTimeAndPrintPrices(parsedDate, prisklass, input.zone(), api, input.sorted(), chargingEnabled, hours);
    }

    public static UserInput checkArguments(String[] args) {
        String zone = null;
        String date = null;
        boolean sorted = false;
        int chargingHours = 0;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--help")) {
                printHelpInfo();
                return null;
            } else if (args[i].equals("--zone")) {
                if (i + 1 < args.length) {
                    zone = args[i + 1].toUpperCase();
                    i = i + 1;
                } else {
                    System.out.println("Fel: Du måste ange en zon efter --zone");
                    printHelpInfo();
                    return null;
                }
            } else if (args[i].equals("--date")) {
                if (i + 1 < args.length) {
                    date = args[i + 1];
                    i = i + 1;
                } else {
                    System.out.println("Fel: Du måste ange ett datum efter --date");
                    printHelpInfo();
                    return null;
                }
            } else if (args[i].equals("--sorted")) {
                sorted = true;
            } else if (args[i].equals("--charging")) {
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
                        System.out.println("Ogiltig laddningstid, använd 2h, 4h eller 8h.");
                        return null;
                    }
                } else {
                    System.out.println("Du måste ange en tid efter --charging (2h,4h,8h)");
                    return null;
                }
            }
        }

        if (zone == null) {
            System.out.println("Du måste skriva --zone SE1-4");
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
        System.out.println("--zone SE1|SE2|SE3|SE4   (required)");
        System.out.println("--date YYYY-MM-DD        (optional, defaults to current date)");
        System.out.println("--sorted                 (optional, sort descending by price)");
        System.out.println("--charging 2h|4h|8h      (optional, find optimal charging window)");
        System.out.println("--help                   (optional, display this help message)");
    }

    public static LocalDate checkDate(String date) {
        try {
            return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            System.out.println("Fel datum. Använd: yyyy-MM-dd");
            printHelpInfo();
            return null;
        }
    }

    public static ElpriserAPI.Prisklass getPrisklass(String zone) {
        if (zone.equals("SE1")) {
            return ElpriserAPI.Prisklass.SE1;
        } else if (zone.equals("SE2")) {
            return ElpriserAPI.Prisklass.SE2;
        } else if (zone.equals("SE3")) {
            return ElpriserAPI.Prisklass.SE3;
        } else if (zone.equals("SE4")) {
            return ElpriserAPI.Prisklass.SE4;
        } else {
            System.out.println("Ogiltig zon. Välj SE1, SE2, SE3 eller SE4.");
            return null;
        }
    }

    public static void checkTimeAndPrintPrices(LocalDate parsedDate, ElpriserAPI.Prisklass prisklass,
                                               String zone, ElpriserAPI api,
                                               boolean sorted, boolean chargingEnabled, int chargingHours) {
        LocalDate today = LocalDate.now();

        List<ElpriserAPI.Elpris> todaysPrices = api.getPriser(parsedDate.toString(), prisklass);
        List<ElpriserAPI.Elpris> tomorrowsPrices = api.getPriser(parsedDate.plusDays(1).toString(), prisklass);

        if (parsedDate.equals(today) && LocalTime.now().getHour() < 13) {
            System.out.println("Du får vänta tills efter 13 för att få morgondagens priser, skriver endast ut dagens priser:");

            if (todaysPrices.size() == 96) {
                todaysPrices = convertQuarterToHourly(todaysPrices);
            }

            List<ElpriserAPI.Elpris> todaysToPrint = new ArrayList<ElpriserAPI.Elpris>();
            for (int i = 0; i < todaysPrices.size(); i++) {
                todaysToPrint.add(todaysPrices.get(i));
            }

            if (sorted) {
                sortPricesDescending(todaysToPrint);
            }

            printPrices(todaysToPrint, zone, parsedDate.toString(), false);
            printStatistics(todaysPrices, "Dagens");

            if (chargingEnabled && chargingHours > 0) {
                calculateOptimalCharging(todaysPrices, chargingHours);
            }
            return;
        }

        System.out.println("\nDagens priser:");
        if (todaysPrices.size() == 96) {
            todaysPrices = convertQuarterToHourly(todaysPrices);
        }
        List<ElpriserAPI.Elpris> todaysToPrint = new ArrayList<ElpriserAPI.Elpris>();
        for (int i = 0; i < todaysPrices.size(); i++) {
            todaysToPrint.add(todaysPrices.get(i));
        }
        if (sorted) {
            sortPricesDescending(todaysToPrint);
        }
        printPrices(todaysToPrint, zone, parsedDate.toString(), false);
        printStatistics(todaysPrices, "Dagens");

        System.out.println("\nMorgondagens priser:");
        if (tomorrowsPrices.size() == 96) {
            tomorrowsPrices = convertQuarterToHourly(tomorrowsPrices);
        }
        List<ElpriserAPI.Elpris> tomorrowsToPrint = new ArrayList<ElpriserAPI.Elpris>();
        for (int i = 0; i < tomorrowsPrices.size(); i++) {
            tomorrowsToPrint.add(tomorrowsPrices.get(i));
        }
        if (sorted) {
            sortPricesDescending(tomorrowsToPrint);
        }
        printPrices(tomorrowsToPrint, zone, parsedDate.plusDays(1).toString(), false);
        printStatistics(tomorrowsPrices, "Morgondagens");

        if (chargingEnabled && chargingHours > 0) {
            List<ElpriserAPI.Elpris> combined = new ArrayList<ElpriserAPI.Elpris>();
            for (int i = 0; i < todaysPrices.size(); i++) {
                combined.add(todaysPrices.get(i));
            }
            for (int i = 0; i < tomorrowsPrices.size(); i++) {
                combined.add(tomorrowsPrices.get(i));
            }
            calculateOptimalCharging(combined, chargingHours);
        }
    }

    public static void sortPricesDescending(List<ElpriserAPI.Elpris> priser) {
        if (priser == null || priser.size() < 2) {
            return;
        }

        for (int i = 0; i < priser.size() - 1; i++) {
            for (int j = 0; j < priser.size() - i - 1; j++) {
                if (priser.get(j).sekPerKWh() < priser.get(j + 1).sekPerKWh()) {
                    ElpriserAPI.Elpris temp = priser.get(j);
                    priser.set(j, priser.get(j + 1));
                    priser.set(j + 1, temp);
                }
            }
        }
    }

    public static List<ElpriserAPI.Elpris> convertQuarterToHourly(List<ElpriserAPI.Elpris> quarters) {
        if (quarters.size() != 96) {
            throw new IllegalArgumentException("Fel: Ogiltigt antal kvartar, måste vara 96");
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

    public static void printPrices(List<ElpriserAPI.Elpris> priser, String zone, String date, boolean sorted) {
        if (priser == null || priser.isEmpty()) {
            System.out.println("Inga priser hittades för " + date + " i zon " + zone);
            return;
        }

        Locale sv = new Locale("sv", "SE");
        for (int i = 0; i < priser.size(); i++) {
            ElpriserAPI.Elpris elpris = priser.get(i);
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
            System.out.println(hourRange + " " + String.format(sv, "%.2f", ore) + " öre");
        }
    }

    public static void printStatistics(List<ElpriserAPI.Elpris> priser, String label) {
        if (priser == null || priser.isEmpty()) {
            return;
        }

        double sum = 0.0;
        ElpriserAPI.Elpris min = priser.get(0);
        ElpriserAPI.Elpris max = priser.get(0);

        for (int i = 0; i < priser.size(); i++) {
            ElpriserAPI.Elpris p = priser.get(i);
            double ore = p.sekPerKWh() * 100;
            sum = sum + ore;

            if (p.sekPerKWh() < min.sekPerKWh()) {
                min = p;
            } else {
                if (p.sekPerKWh() > max.sekPerKWh()) {
                    max = p;
                }
            }
        }

        double avg = sum / priser.size();

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
        System.out.println(label + " statistik:");
        System.out.println("Högsta pris: " + maxRange + " " + String.format(sv, "%.2f", max.sekPerKWh() * 100) + " öre");
        System.out.println("Lägsta pris: " + minRange + " " + String.format(sv, "%.2f", min.sekPerKWh() * 100) + " öre");
        System.out.println("Medelpris: " + String.format(sv, "%.2f", avg) + " öre");
    }

    public static void calculateOptimalCharging(List<ElpriserAPI.Elpris> priser, int hours) {
        if (priser == null || priser.isEmpty()) {
            return;
        }
        if (priser.size() < hours) {
            System.out.println("Påbörja laddning kunde inte beräknas – inte tillräckligt med priser.");
            return;
        }

        double minSum = Double.MAX_VALUE;
        int minIndex = 0;

        for (int i = 0; i <= priser.size() - hours; i++) {
            double sum = 0;
            for (int j = 0; j < hours; j++) {
                sum = sum + priser.get(i + j).sekPerKWh();
            }
            if (sum < minSum) {
                minSum = sum;
                minIndex = i;
            }
        }

        System.out.println("Beräknar optimalt laddningsfönster:");
        System.out.println("Påbörja laddning för " + hours + " timmar:");

        Locale sv = new Locale("sv", "SE");
        double totalPrice = 0;

        for (int i = minIndex; i < minIndex + hours; i++) {
            ElpriserAPI.Elpris p = priser.get(i);

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
        public String zone;
        public String date;
        public boolean sorted;
        public int chargingHours;

        public UserInput(String zone, String date, boolean sorted, int chargingHours) {
            this.zone = zone;
            this.date = date;
            this.sorted = sorted;
            this.chargingHours = chargingHours;
        }

        public String zone() {
            return this.zone;
        }

        public String date() {
            return this.date;
        }

        public boolean sorted() {
            return this.sorted;
        }

        public int chargingHours() {
            return this.chargingHours;
        }
    }
}