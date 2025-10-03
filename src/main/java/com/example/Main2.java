package com.example;

import com.example.api.ElpriserAPI;
import com.example.api.ElpriserAPI.Elpris;
import com.example.api.ElpriserAPI.Prisklass;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class Main2 {

    public static class ArgsResult {
        String zone;
        String date;
    }

    public static ArgsResult readArguments(String[] args) {
        ArgsResult result = new ArgsResult();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--zone") && i + 1 < args.length) {
                result.zone = args[i + 1];
                i++;
            } else if (args[i].equals("--date") && i + 1 < args.length) {
                result.date = args[i + 1];
                i++;
            }
        }
        if (result.date == null) {
            result.date = LocalDate.now().toString();
        }
        return result;
    }

    private static List<Elpris> getTwoDaysPrices(ElpriserAPI api, Prisklass zone,
                                                 LocalDate day1, LocalDate day2) {
        List<Elpris> prices = new ArrayList<>();
        prices.addAll(api.getPriser(day1, zone));
        prices.addAll(api.getPriser(day2, zone));
        return prices;
    }

    private static List<Elpris> convertQuarterToHour(List<Elpris> prices, LocalDate targetDate) {
        List<Elpris> todaysQuarters = new ArrayList<>();
        for (int i = 0; i < prices.size(); i++) {
            if (prices.get(i).timeStart().toLocalDate().equals(targetDate)) {
                todaysQuarters.add(prices.get(i));
            }
        }

        if (todaysQuarters.size() != 96) {
            throw new IllegalArgumentException(
                    "Fel: Antal kvartal för dagen är inte 96. Kan inte omvandla till timmar."
            );
        }

        List<Elpris> hourlyPrices = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            int startIndex = hour * 4;
            int endIndex = startIndex + 4;

            double sum = 0;
            for (int j = startIndex; j < endIndex; j++) {
                sum += todaysQuarters.get(j).sekPerKWh();
            }
            double average = sum / 4;

            ZonedDateTime startTime = todaysQuarters.get(startIndex).timeStart();
            ZonedDateTime endTime = todaysQuarters.get(endIndex - 1).timeEnd();
            hourlyPrices.add(new Elpris(average, 0, 0, startTime, endTime));
        }

        return hourlyPrices;
    }

    public static void main(String[] args) {
        ArgsResult parsed = readArguments(args);

        Prisklass zone = Prisklass.valueOf(parsed.zone.toUpperCase());
        LocalDate date = LocalDate.parse(parsed.date);
        LocalDate nextDay = date.plusDays(1);

        ElpriserAPI api = new ElpriserAPI();

        List<Elpris> allPrices = getTwoDaysPrices(api, zone, date, nextDay);

        List<Elpris> todaysHourlyPrices = convertQuarterToHour(allPrices, date);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < todaysHourlyPrices.size(); i++) {
            Elpris pris = todaysHourlyPrices.get(i);
            String prisDatum = pris.timeStart().toLocalDate().format(dateFormatter);

            System.out.printf("%s %02d-%02d: %.2f öre%n",
                    prisDatum,
                    pris.timeStart().getHour(),
                    pris.timeEnd().getHour(),
                    pris.sekPerKWh() * 100);
        }
    }
}