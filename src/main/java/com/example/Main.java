package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.time.LocalDate;
import java.util.List;

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

    public static void main(String[] args) {

        ElpriserAPI elpriserAPI = new ElpriserAPI();

        ArgsResult currentArgs = readArguments(args);

        if (currentArgs.getZone() != null && currentArgs.getDate() != null) {

            LocalDate selectedDate = LocalDate.parse(currentArgs.getDate());
            Prisklass selectedZone = Prisklass.valueOf(currentArgs.getZone());

            List<Elpris> prices = elpriserAPI.getPriser(selectedDate, selectedZone);

            System.out.println("\nPriser för zon " + currentArgs.getZone() + " den " + selectedDate + ":\n");
            for (int i = 0; i < prices.size(); i++) {
                Elpris price = prices.get(i);
                String startTime = price.timeStart().toLocalTime().toString();
                double pricePerKWh = price.sekPerKWh();
                System.out.println(startTime + " - " + pricePerKWh + " sek/KWh");
            }
        }
    }
}