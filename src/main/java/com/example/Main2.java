package com.example;

import java.time.LocalDate;
import java.time.LocalTime;

public class Main2 {

    public static void main(String[] args) {

        if (args.length == 0 || args[0].equals("--help")) {
            showHelp();
            return;
        }

        String zone = null;
        if (args.length >= 2 && args[0].equals("--zone") && isValidZone(args[1])) {
            zone = args[1];
        } else {
            System.out.println("Felaktig zon. Använd --zone SE1|SE2|SE3|SE4.");
            return;
        }

        LocalDate date = null;
        boolean dateSpecified = false;

        for (int i = 2; i < args.length; i++) {
            if (args[i].equals("--date")) {
                if (i + 1 < args.length) {
                    date = LocalDate.parse(args[i + 1]);
                    dateSpecified = true;
                    break;
                } else {
                    System.out.println("Fel: inget datum angivet efter --date");
                    return;
                }
            }
        }

        if (!dateSpecified) {
            date = LocalDate.now();
        }

        LocalDate nextDay = date.plusDays(1);
        boolean fetchNextDay = true;

        if (!dateSpecified && date.equals(LocalDate.now())) {
            LocalTime now = LocalTime.now();
            if (now.isBefore(LocalTime.of(13, 0))) {
                System.out.println("Morgondagens priser är ännu inte tillgängliga. Kör programmet efter 13:00 för att få dem.");
                fetchNextDay = false;
            }
        }

        System.out.println("Vald zon: " + zone);
        System.out.println("Datum att hämta priser för: " + date);
        if (fetchNextDay) {
            System.out.println("Inkluderar även nästa dag: " + nextDay);
        }
    }

    private static boolean isValidZone(String zone) {
        return zone.equals("SE1") || zone.equals("SE2") || zone.equals("SE3") || zone.equals("SE4");
    }

    private static void showHelp() {
        System.out.println("Hjälp:");
        System.out.println("--zone SE1|SE2|SE3|SE4  : Välj priszon (obligatoriskt)");
        System.out.println("--date YYYY-MM-DD        : Ange datum (valfritt, default = idag)");
        System.out.println("--sorted                 : Visa priser sorterade efter kostnad (valfritt)");
        System.out.println("--charging 2h|4h|8h      : Hitta bästa laddningsfönster (valfritt)");
        System.out.println("--help                   : Visa detta hjälpmeddelande");
    }
}
