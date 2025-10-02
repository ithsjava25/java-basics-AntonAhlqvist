package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

public class Main {

    public static class ArgsResult {
        private String zone;
        private String date;
        private boolean sorted;
        private String charging;

        public String getZone() { return zone; }
        public String getDate() { return date; }
        public boolean isSorted() { return sorted; }
        public String getCharging() { return charging; }

        public void setZone(String zone) { this.zone = zone; }
        public void setDate(String date) { this.date = date; }
        public void setSorted(boolean sorted) { this.sorted = sorted; }
        public void setCharging(String charging) { this.charging = charging; }
    }

    public static void printHelpInfo() {
        System.out.println("Usage: java -cp target/classes com.example.Main [options]");
        System.out.println("Options:");
        System.out.println("--zone SE1|SE2|SE3|SE4   (required)");
        System.out.println("--date YYYY-MM-DD        (optional, defaults to current date)");
        System.out.println("--sorted                 (optional, display prices in descending order)");
        System.out.println("--charging 2h|4h|8h      (optional, find optimal charging period)");
        System.out.println("--help                   (optional, display this help message)");
    }

    private static ArgsResult readArguments(String[] args) {
        ArgsResult result = new ArgsResult();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--zone") && i + 1 < args.length) {
                result.setZone(args[i + 1]);
            } else if (args[i].equals("--date") && i + 1 < args.length) {
                result.setDate(args[i + 1]);
            } else if (args[i].equals("--sorted")) {
                result.setSorted(true);
            } else if (args[i].equals("--charging") && i + 1 < args.length) {
                result.setCharging(args[i + 1]);
            }
        }

        return result;
    }

    private static String formatPrice(double sekPerKWh) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("sv", "SE"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return nf.format(sekPerKWh * 100) + " öre";
    }

    private static List<Double> quarterToHour(List<Elpris> kvartPriser) {
        List<Double> timPriser = new ArrayList<>();
        int kvartalPerTimme = 4;

        for (int i = 0; i < kvartPriser.size(); i += kvartalPerTimme) {
            double sumForCurrentHour = 0;
            for (int j = 0; j < kvartalPerTimme; j++) {
                sumForCurrentHour += kvartPriser.get(i + j).sekPerKWh();
            }
            double currentHourPrice = sumForCurrentHour / kvartalPerTimme;
            timPriser.add(currentHourPrice);
        }

        return timPriser;
    }

    private static void printHourlyStats(List<Double> timPriser) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("sv", "SE"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);

        double sum = 0;
        double min = timPriser.get(0);
        double max = timPriser.get(0);
        int minHour = 0;
        int maxHour = 0;

        System.out.println("\nTimpriser:\n");
        for (int i = 0; i < timPriser.size(); i++) {
            double price = timPriser.get(i);
            sum += price;

            if (price < min) {
                min = price;
                minHour = i;
            }
            if (price > max) {
                max = price;
                maxHour = i;
            }

            String hourInterval = String.format("%02d-%02d", i, i + 1);
            System.out.println(hourInterval + " " + nf.format(price) + " öre");
        }

        double average = sum / timPriser.size();

        System.out.println("\n--- Statistik ---");
        System.out.println("Medelpris: " + nf.format(average) + " öre");
        System.out.println("Lägsta pris: " + nf.format(min) + " öre (Timme " + (minHour + 1) + ")");
        System.out.println("Högsta pris: " + nf.format(max) + " öre (Timme " + (maxHour + 1) + ")");
    }

    public static void main(String[] args) {

        ElpriserAPI elpriserAPI = new ElpriserAPI();
        ArgsResult currentArgs = readArguments(args);

        if (args.length == 0 || (args.length == 1 && args[0].equals("--help"))) {
            printHelpInfo();
            return;
        }

        if (currentArgs.getZone() == null) {
            printHelpInfo();
            return;
        }

        if (currentArgs.getDate() == null) {
            currentArgs.setDate(LocalDate.now().toString());
        }

        LocalDate selectedDate = LocalDate.parse(currentArgs.getDate());
        Prisklass selectedZone = Prisklass.valueOf(currentArgs.getZone());

        List<Elpris> kvartPriser = elpriserAPI.getPriser(selectedDate, selectedZone);
        List<Double> timPriser = quarterToHour(kvartPriser);

        System.out.println("\nTimpriser för zon " + currentArgs.getZone() + " den " + selectedDate + ":\n");

        printHourlyStats(timPriser);
    }
}
