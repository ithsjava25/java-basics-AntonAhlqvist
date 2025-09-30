package com.example;

public class Main {

    public static class ArgsResult {
        private String zone;
        private String date;
        private boolean sorted;
        private String charging;

        public String getZone() {
            return zone;
        }

        public String getDate() {
            return date;
        }

        public boolean isSorted() {
            return sorted;
        }

        public String getCharging() {
            return charging;
        }

        public void setZone(String zone) {
            this.zone = zone;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public void setSorted(boolean sorted) {
            this.sorted = sorted;
        }

        public void setCharging(String charging) {
            this.charging = charging;
        }

        public void printInfo() {
            System.out.println("Print zone: " + zone);
            System.out.println("Print date: " + date);
            System.out.println("Print sorted: " + sorted);
            System.out.println("Print charging: " + charging);
        }
    }

    private static ArgsResult parseArgs(String[] args) {
        ArgsResult result = new ArgsResult();

        for (int i = 0; i < args.length; i++) {
            if (args[0].equals("--zone")) {
                result.setZone(args[i + 1]);
            } else if (args[0].equals("--date")) {
                result.setDate(args[i + 1]);
            } else if (args[0].equals("--sort")) {
                result.setSorted(true);
            } else if (args[0].equals("--charging")); {
                result.setCharging(args[i + 1]);
            }
        }
            return result;
    }

    public static void main(String[] args) {

        ArgsResult parsed = parseArgs(args);
        parsed.printInfo();
    }
}