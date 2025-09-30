package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.time.LocalDate;
import java.util.List;

public class Main {

    public static void help() {
        System.out.println("Nästa gång du startar programmet kan du välja mellan följande alternativ:");
        System.out.println("------");
        System.out.println("1) '--zone'*. Välj mellan 'SE1', 'SE2', 'SE3' eller 'SE4'. Ex: '--zone SE1'.");
        System.out.println("2) '--date'. Skriv ett datum enligt format: 'YYYY-MM-DD'.");
        System.out.println("3) '--sorted'. För att sortera priserna i fallande ordning. Skriv '--sorted-R' för att sortera åt andra hållet.");
        System.out.println("4) '--charging'. För att hitta det bästa laddningsspannet. Välj mellan '2h', '4h' eller '8h'. Ex: '--charging 2h'.");
        System.out.println("5) '--help'. För att visa den här informationen.");
        System.out.println("------");
        System.out.println("* = obligatoriskt.");
        System.out.println("Börja med att skriva '--zone' redan nu, lägg till önskvärda argument, eller tryck på valfri tangent för att avsluta.");
    }

    public static class ArgsResult {
        String zone;
        String date;
        boolean sorted;
        String charging;
    }

    public static ArgsResult parseArgs(String[] args) {
        ArgsResult result = new ArgsResult();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--zone")) {
                result.zone = args[i + 1];
            } else if (args[i].equals("--date")) {
                result.date = args[i + 1];
            } else if (args[i].equals("--sorted")) {
                result.sorted = true;
            } else if (args[i].equals("--charging")) {
                result.charging = args[i + 1];
            }
        }

        return result;
    }

    public static void main(String[] args) {

        ElpriserAPI api = new ElpriserAPI();

        if (args.length == 0 || args[0].equals("--help")) {
            help();
            return;
        }

        ArgsResult parsed = parseArgs(args);

        System.out.println("Zone = " + parsed.zone);
        System.out.println("Date = " + parsed.date);
        System.out.println("Sorted = " + parsed.sorted);
        System.out.println("Charging = " + parsed.charging);

        if (parsed.zone != null && parsed.date != null) {

            String dateString = parsed.date;
            LocalDate datum = LocalDate.parse(dateString);

            String zoneString = parsed.zone;
            Prisklass prisklass = Prisklass.valueOf(zoneString);

            List<Elpris> priser = api.getPriser(datum, prisklass);

            if (parsed.sorted == true) {
                for (int i = 0; i < priser.size(); i++) {
                    for (int j = i + 1; j < priser.size(); j++) {
                        Elpris pris1 = priser.get(i);
                        Elpris pris2 = priser.get(j);
                        if (pris1.sekPerKWh() < pris2.sekPerKWh()) {
                            priser.set(i, pris2);
                            priser.set(j, pris1);
                        }
                    }
                }
            }

            System.out.println("Priserna för zon " + zoneString + " den: " + datum + "\n");

            for (int i = 0; i < priser.size(); i++) {
                Elpris pris = priser.get(i);
                String startTid = pris.timeStart().toLocalTime().toString();
                double prisPerKwh = pris.sekPerKWh();

                System.out.println(startTid + " - " + prisPerKwh + " sek/KWh");
            }
        }
    }
}