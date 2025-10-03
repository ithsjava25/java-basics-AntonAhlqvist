package com.example;

import com.example.api.ElpriserAPI;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class Main2 {

    public static void main(String[] args) {
        Locale.setDefault(new Locale("sv", "SE"));
        ElpriserAPI api = new ElpriserAPI();

        UserInput input = checkArguments(args);
        if (input == null) return;

        ElpriserAPI.Prisklass prisklass = getPrisklass(input.zone());
        if (prisklass == null) return;

        LocalDate parsedDate = checkDate(input.date());
        if (parsedDate == null) return;

        checkTimeAndPrintPrices(parsedDate, prisklass, input.zone(), api, args);
    }

    public static UserInput checkArguments(String[] args) {
        String zone = null;
        String date = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help":
                    printHelpInfo();
                    return null;
                case "--zone":
                    if (i + 1 < args.length) zone = args[++i].toUpperCase();
                    else {
                        System.out.println("Fel: Du måste ange en zon efter --zone");
                        printHelpInfo();
                        return null;
                    }
                    break;
                case "--date":
                    if (i + 1 < args.length) date = args[++i];
                    else {
                        System.out.println("Fel: Du måste ange ett datum efter --date");
                        printHelpInfo();
                        return null;
                    }
                    break;
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
        System.out.println("Usage: java -cp target/classes com.example.Main3 [options]");
        System.out.println("--zone SE1|SE2|SE3|SE4   (required)");
        System.out.println("--date YYYY-MM-DD        (optional, defaults to current date)");
        System.out.println("--sorted                 (optional, sort descending by price)");
        System.out.println("--help                   (optional, display this help message)");
    }

    public static LocalDate checkDate(String date) {
        try {
            return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            System.out.println("Felaktigt datumformat. Använd: yyyy-MM-dd");
            printHelpInfo();
            return null;
        }
    }

    public static ElpriserAPI.Prisklass getPrisklass(String zone) {
        if (zone.equals("SE1")) return ElpriserAPI.Prisklass.SE1;
        else if (zone.equals("SE2")) return ElpriserAPI.Prisklass.SE2;
        else if (zone.equals("SE3")) return ElpriserAPI.Prisklass.SE3;
        else if (zone.equals("SE4")) return ElpriserAPI.Prisklass.SE4;
        else {
            System.out.println("Ogiltig zon. Välj SE1, SE2, SE3 eller SE4.");
            return null;
        }
    }

    public static void checkTimeAndPrintPrices(LocalDate parsedDate, ElpriserAPI.Prisklass prisklass,
                                               String zone, ElpriserAPI api, String[] args) {
        boolean sorted = Arrays.asList(args).contains("--sorted");
        LocalDate today = LocalDate.now();

        List<ElpriserAPI.Elpris> todaysPrices = api.getPriser(parsedDate.toString(), prisklass);
        List<ElpriserAPI.Elpris> tomorrowsPrices = api.getPriser(parsedDate.plusDays(1).toString(), prisklass);

        if (parsedDate.equals(today) && LocalTime.now().getHour() < 13) {
            System.out.println("Du får vänta tills efter 13 för att få morgondagens priser, skriver endast ut dagens priser:");
            if (todaysPrices.size() == 96) {
                todaysPrices = convertQuarterToHourly(todaysPrices);
                printPrices(todaysPrices, zone, parsedDate.toString(), sorted);
                printStatistics(todaysPrices, "Dagens");
            } else {
                System.out.println("Fel: Ogiltigt antal kvartaler. Beräkning av priser avbryts.");
            }
        } else {

            System.out.println("\nDagens priser:");
            List<ElpriserAPI.Elpris> todaysToPrint = new ArrayList<>(todaysPrices);
            if (todaysToPrint.size() == 96) todaysToPrint = convertQuarterToHourly(todaysToPrint);
            printPrices(todaysToPrint, zone, parsedDate.toString(), sorted);
            printStatistics(todaysToPrint, "Dagens");

            System.out.println("\nMorgondagens priser:");
            List<ElpriserAPI.Elpris> tomorrowsToPrint = new ArrayList<>(tomorrowsPrices);
            if (tomorrowsToPrint.size() == 96) tomorrowsToPrint = convertQuarterToHourly(tomorrowsToPrint);
            printPrices(tomorrowsToPrint, zone, parsedDate.plusDays(1).toString(), sorted);
            printStatistics(tomorrowsToPrint, "Morgondagens");
        }
    }

    public static List<ElpriserAPI.Elpris> convertQuarterToHourly(List<ElpriserAPI.Elpris> quarters) {
        if (quarters.size() != 96) {
            throw new IllegalArgumentException("Fel: Ogiltigt antal kvartaler, måste vara 96");
        }

        List<ElpriserAPI.Elpris> hourly = new ArrayList<>();

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

            for (int i = 0; i < priser.size() - 1; i++) {
                for (int j = 0; j < priser.size() - 1 - i; j++) {
                    if (priser.get(j).sekPerKWh() < priser.get(j + 1).sekPerKWh()) {
                        ElpriserAPI.Elpris temp = priser.get(j);
                        priser.set(j, priser.get(j + 1));
                        priser.set(j + 1, temp);
                    }
                }
            }
        }

        for (int i = 0; i < priser.size(); i++) {
            ElpriserAPI.Elpris elpris = priser.get(i);
            String hourRange = String.format("%02d-%02d",
                    elpris.timeStart().getHour(),
                    elpris.timeEnd().getHour());
            System.out.printf("%s: %.2f öre\n", hourRange, elpris.sekPerKWh() * 100);
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

        System.out.printf("%s statistik:\n", label);
        System.out.printf("Högsta pris: %s %.2f öre\n", maxRange, max.sekPerKWh() * 100);
        System.out.printf("Lägsta pris: %s %.2f öre\n", minRange, min.sekPerKWh() * 100);
        System.out.printf("Medelpris: %.2f öre\n", avg * 100);
    }

    public record UserInput(String zone, String date) {}
}