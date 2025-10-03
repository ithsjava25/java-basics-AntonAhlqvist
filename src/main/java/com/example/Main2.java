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
        ElpriserAPI elpriserAPI = new ElpriserAPI();

        UserInput input = checkArguments(args);
        if (input == null) return;

        ElpriserAPI.Prisklass prisklass = getPrisklass(input.zone());
        if (prisklass == null) return;

        LocalDate parsedDate = checkDate(input.date());
        if (parsedDate == null) return;

        checkTimeAndPrintPrices(parsedDate, prisklass, input.zone(), elpriserAPI);
    }

    public static UserInput checkArguments(String[] args) {
        String zone = null;
        String date = null;

        if (args.length == 0) {
            System.out.println("Du måste skriva --zone SE1-4");
            printHelpInfo();
            return null;
        }

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--help":
                    printHelpInfo();
                    return null;
                case "--zone":
                    if (i + 1 < args.length) {
                        zone = args[++i].toUpperCase();
                    } else {
                        System.out.println("Fel: Du måste ange en zon efter --zone");
                        printHelpInfo();
                        return null;
                    }
                    break;
                case "--date":
                    if (i + 1 < args.length) {
                        date = args[++i];
                    } else {
                        System.out.println("Fel: Du måste ange ett datum efter --date");
                        printHelpInfo();
                        return null;
                    }
                    break;
                default:
                    System.out.println("Ogiltigt argument: " + args[i]);
                    printHelpInfo();
                    return null;
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

        return new UserInput(zone, date);
    }

    public static void printHelpInfo() {
        System.out.println("Usage: java -cp target/classes com.example.Main3 [options]");
        System.out.println("Options:");
        System.out.println("--zone SE1|SE2|SE3|SE4   (required)");
        System.out.println("--date YYYY-MM-DD        (optional, defaults to current date)");
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
        switch (zone) {
            case "SE1": return ElpriserAPI.Prisklass.SE1;
            case "SE2": return ElpriserAPI.Prisklass.SE2;
            case "SE3": return ElpriserAPI.Prisklass.SE3;
            case "SE4": return ElpriserAPI.Prisklass.SE4;
            default:
                System.out.println("Ogiltig zon. Välj SE1, SE2, SE3 eller SE4.");
                return null;
        }
    }

    public static void checkTimeAndPrintPrices(LocalDate parsedDate, ElpriserAPI.Prisklass prisklass, String zone, ElpriserAPI api) {
        LocalDate today = LocalDate.now();

        if (parsedDate.equals(today)) {
            int hourNow = LocalTime.now().getHour();

            if (hourNow < 13) {
                System.out.println("Du får vänta tills efter 13 för att få morgondagens priser, skriver endast ut dagens priser:");
                List<ElpriserAPI.Elpris> todaysPrices = api.getPriser(parsedDate.toString(), prisklass);
                printPriser(todaysPrices, zone, parsedDate.toString());
            } else {
                System.out.println("Efter 13: Hämtar dagens och morgondagens priser:");
                List<ElpriserAPI.Elpris> allPrices = new ArrayList<>();
                List<ElpriserAPI.Elpris> todaysPrices = api.getPriser(parsedDate.toString(), prisklass);
                List<ElpriserAPI.Elpris> tomorrowsPrices = api.getPriser(parsedDate.plusDays(1).toString(), prisklass);
                allPrices.addAll(todaysPrices);
                allPrices.addAll(tomorrowsPrices);

                System.out.println("\nDagens priser:");
                printPriser(todaysPrices, zone, parsedDate.toString());

                System.out.println("\nMorgondagens priser:");
                printPriser(tomorrowsPrices, zone, parsedDate.plusDays(1).toString());
            }
        } else {
            List<ElpriserAPI.Elpris> priser = api.getPriser(parsedDate.toString(), prisklass);
            printPriser(priser, zone, parsedDate.toString());
        }
    }

    public static void printPriser(List<ElpriserAPI.Elpris> priser, String zone, String date) {
        if (priser == null || priser.isEmpty()) {
            System.out.println("Inga priser hittades för " + date + " i zon " + zone);
            return;
        }

        System.out.println("\nTimpriser för zon " + zone + " den " + date + ":");

        if (priser.size() == 96) { // kvartalshantering
            for (int i = 0; i < 24; i++) {
                int startIndex = i * 4;
                int endIndex = startIndex + 4;
                double sum = 0.0;

                for (int j = startIndex; j < endIndex; j++) {
                    sum += priser.get(j).sekPerKWh();
                }

                double avg = sum / 4.0;
                String hourRange = String.format("%02d-%02d", i, i + 1);
                System.out.printf("%s: %.2f öre\n", hourRange, avg * 100);
            }
        } else {
            for (ElpriserAPI.Elpris elpris : priser) {
                String hourRange = String.format("%02d-%02d",
                        elpris.timeStart().getHour(),
                        elpris.timeEnd().getHour());
                double orePris = elpris.sekPerKWh() * 100;
                System.out.printf("%s: %.2f öre\n", hourRange, orePris);
            }
        }
    }

    public record UserInput(String zone, String date) {}
}