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
        if (input == null) return;

        ElpriserAPI.Prisklass prisklass = getPrisklass(input.zone());
        if (prisklass == null) return;

        LocalDate parsedDate = checkDate(input.date());
        if (parsedDate == null) return;

        boolean sorted = false;
        boolean chargingEnabled = false;
        int chargingHours = 0;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--sorted")) sorted = true;
            if (args[i].equals("--charging")) chargingEnabled = true;
        }
        chargingHours = getChargingHours(args);

        checkTimeAndPrintPrices(parsedDate, prisklass, input.zone(), api, sorted, chargingEnabled, chargingHours);
    }

    public static UserInput checkArguments(String[] args) {
        String zone = null;
        String date = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--help")) {
                printHelpInfo();
                return null;
            } else if (args[i].equals("--zone")) {
                if (i + 1 < args.length) zone = args[++i].toUpperCase();
                else {
                    System.out.println("Fel: Du måste ange en zon efter --zone");
                    printHelpInfo();
                    return null;
                }
            } else if (args[i].equals("--date")) {
                if (i + 1 < args.length) date = args[++i];
                else {
                    System.out.println("Fel: Du måste ange ett datum efter --date");
                    printHelpInfo();
                    return null;
                }
            }
        }

        if (zone == null) {
            System.out.println("Du måste skriva --zone SE1-4");
            printHelpInfo();
            return null;
        }

        if (date == null) date = LocalDate.now().toString();

        return new UserInput(zone, date);
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
        if (zone.equals("SE1")) return ElpriserAPI.Prisklass.SE1;
        if (zone.equals("SE2")) return ElpriserAPI.Prisklass.SE2;
        if (zone.equals("SE3")) return ElpriserAPI.Prisklass.SE3;
        if (zone.equals("SE4")) return ElpriserAPI.Prisklass.SE4;

        System.out.println("Ogiltig zon. Välj SE1, SE2, SE3 eller SE4.");
        return null;
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

            List<ElpriserAPI.Elpris> todaysToPrint = new ArrayList<ElpriserAPI.Elpris>(todaysPrices);

            if (sorted) {
                for (int i = 0; i < todaysToPrint.size() - 1; i++) {
                    for (int j = 0; j < todaysToPrint.size() - i - 1; j++) {
                        if (todaysToPrint.get(j).sekPerKWh() < todaysToPrint.get(j + 1).sekPerKWh()) {
                            ElpriserAPI.Elpris temp = todaysToPrint.get(j);
                            todaysToPrint.set(j, todaysToPrint.get(j + 1));
                            todaysToPrint.set(j + 1, temp);
                        }
                    }
                }
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
        List<ElpriserAPI.Elpris> todaysToPrint = new ArrayList<ElpriserAPI.Elpris>(todaysPrices);
        if (sorted) {
            for (int i = 0; i < todaysToPrint.size() - 1; i++) {
                for (int j = 0; j < todaysToPrint.size() - i - 1; j++) {
                    if (todaysToPrint.get(j).sekPerKWh() < todaysToPrint.get(j + 1).sekPerKWh()) {
                        ElpriserAPI.Elpris temp = todaysToPrint.get(j);
                        todaysToPrint.set(j, todaysToPrint.get(j + 1));
                        todaysToPrint.set(j + 1, temp);
                    }
                }
            }
        }
        printPrices(todaysToPrint, zone, parsedDate.toString(), false);
        printStatistics(todaysPrices, "Dagens");

        System.out.println("\nMorgondagens priser:");
        if (tomorrowsPrices.size() == 96) {
            tomorrowsPrices = convertQuarterToHourly(tomorrowsPrices);
        }
        List<ElpriserAPI.Elpris> tomorrowsToPrint = new ArrayList<ElpriserAPI.Elpris>(tomorrowsPrices);
        if (sorted) {
            for (int i = 0; i < tomorrowsToPrint.size() - 1; i++) {
                for (int j = 0; j < tomorrowsToPrint.size() - i - 1; j++) {
                    if (tomorrowsToPrint.get(j).sekPerKWh() < tomorrowsToPrint.get(j + 1).sekPerKWh()) {
                        ElpriserAPI.Elpris temp = tomorrowsToPrint.get(j);
                        tomorrowsToPrint.set(j, tomorrowsToPrint.get(j + 1));
                        tomorrowsToPrint.set(j + 1, temp);
                    }
                }
            }
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
                sum += quarters.get(j).sekPerKWh();
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

        if (sorted) {

            Collections.sort(priser, Comparator.comparingDouble(ElpriserAPI.Elpris::sekPerKWh).reversed());
        }

        Locale sv = new Locale("sv", "SE");
        for (int i = 0; i < priser.size(); i++) {
            ElpriserAPI.Elpris elpris = priser.get(i);
            String hourRange = String.format("%02d-%02d",
                    elpris.timeStart().getHour(),
                    elpris.timeEnd().getHour());
            double ore = elpris.sekPerKWh() * 100;
            System.out.println(hourRange + " " + String.format(sv, "%.2f", ore) + " öre");
        }
    }

    public static void printStatistics(List<ElpriserAPI.Elpris> priser, String label) {
        if (priser == null || priser.isEmpty()) return;

        double sum = 0.0;
        ElpriserAPI.Elpris min = priser.get(0);
        ElpriserAPI.Elpris max = priser.get(0);

        for (int i = 0; i < priser.size(); i++) {
            ElpriserAPI.Elpris p = priser.get(i);
            sum += p.sekPerKWh();
            if (p.sekPerKWh() < min.sekPerKWh()) min = p;
            if (p.sekPerKWh() > max.sekPerKWh()) max = p;
        }

        double avg = sum / priser.size();
        String minRange = String.format("%02d-%02d", min.timeStart().getHour(), min.timeEnd().getHour());
        String maxRange = String.format("%02d-%02d", max.timeStart().getHour(), max.timeEnd().getHour());

        Locale sv = new Locale("sv", "SE");
        System.out.printf("%s statistik:\n", label);
        System.out.println("Högsta pris: " + maxRange + " " + String.format(sv, "%.2f", max.sekPerKWh() * 100) + " öre");
        System.out.println("Lägsta pris: " + minRange + " " + String.format(sv, "%.2f", min.sekPerKWh() * 100) + " öre");
        System.out.println("Medelpris: " + String.format(sv, "%.2f", avg * 100) + " öre");
    }

    public static void calculateOptimalCharging(List<ElpriserAPI.Elpris> priser, int hours) {
        if (priser == null || priser.isEmpty()) return;
        if (priser.size() < hours) {
            System.out.println("Påbörja laddning kunde inte beräknas – inte tillräckligt med priser.");
            return;
        }

        double minSum = Double.MAX_VALUE;
        int minIndex = 0;

        for (int i = 0; i <= priser.size() - hours; i++) {
            double sum = 0;
            for (int j = 0; j < hours; j++) {
                sum += priser.get(i + j).sekPerKWh();
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
            String start = String.format("%02d:%02d", p.timeStart().getHour(), p.timeStart().getMinute());
            String end = String.format("%02d:%02d", p.timeEnd().getHour(), p.timeEnd().getMinute());
            double ore = p.sekPerKWh() * 100;
            totalPrice += ore;
            System.out.println("kl " + start + "-" + end + ": " + String.format(sv, "%.2f", ore) + " öre");
        }
        System.out.println("Totalt pris för fönstret: " + String.format(sv, "%.2f", totalPrice) + " öre");
        System.out.println("Medelpris för fönster: " + String.format(sv, "%.2f", totalPrice / hours) + " öre");
    }

    public static int getChargingHours(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--charging") && i + 1 < args.length) {
                String ch = args[i + 1].toLowerCase();
                if (ch.equals("2h")) return 2;
                else if (ch.equals("4h")) return 4;
                else if (ch.equals("8h")) return 8;
                else {
                    System.out.println("Påbörja laddning kunde inte beräknas – ogiltig laddningstid, använd 2h, 4h eller 8h.");
                    return 0;
                }
            }
        }
        return 0;
    }

    public record UserInput(String zone, String date) {}
}
