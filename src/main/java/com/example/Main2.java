package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.time.LocalDate;
import java.util.List;

public class Main2 {

    public static class ArgsResult {
        String zone;
        String date;
        boolean sorted;
        String charging;
    }

    public static ArgsResult readArguments(String[] args) {
        ArgsResult result = new ArgsResult();

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--zone")) {
                result.zone = args[i + 1];
                i++;
            } else if (args[i].equals("--date")) {
                result.date = args[i + 1];
                i++;
            } else if (args[i].equals("--sorted")) {
                result.sorted = true;
            } else if (args[i].equals("--charging")) {
                result.charging = args[i + 1];
                i++;
            }
        }

        if (result.date == null) {
            result.date = LocalDate.now().toString();
        }

        return result;
    }

    public static void main(String[] args) {
        ArgsResult parsed = readArguments(args);

        ElpriserAPI api = new ElpriserAPI();

        Prisklass zone = Prisklass.valueOf(parsed.zone.toUpperCase());

        LocalDate date = LocalDate.parse(parsed.date);

        List<Elpris> priser = api.getPriser(date, zone);

        for (Elpris pris : priser) {
            System.out.println(pris.timeStart() + " - " + pris.sekPerKWh() + " öre");
        }
    }
}