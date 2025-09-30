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

    public static void main(String[] args) {

        ElpriserAPI api = new ElpriserAPI();
        LocalDate idag = LocalDate.now();

        if (args.length == 0 || args[0].equals("--help")) {
            help();

        } else if (args.length == 2 && args[0].equals("--zone") && args[1].equals("SE1")) {

            List<Elpris> priser = api.getPriser(idag, Prisklass.SE1);

            System.out.println("Priserna för zon 1:\n");

            for (int i = 0; i < priser.size(); i++) {
                Elpris pris = priser.get(i);
                System.out.println(pris.timeStart().toLocalTime() + " - " + pris.sekPerKWh() + " sek/KWh");
            }

        } else if (args.length == 2 && args[0].equals("--zone") && args[1].equals("SE2")) {

            List<Elpris> priser = api.getPriser(idag, Prisklass.SE2);

            System.out.println("Priserna för zon 2:\n");

            for (int i = 0; i < priser.size(); i++) {
                Elpris pris = priser.get(i);
                System.out.println(pris.timeStart().toLocalTime() + " - " + pris.sekPerKWh() + " sek/KWh");
            }
        } else if (args.length == 2 && args[0].equals("--zone") && args[1].equals("SE3")) {

            List<Elpris> priser = api.getPriser(idag, Prisklass.SE3);

            System.out.println("Priserna för zon 3:\n");

            for (int i = 0; i < priser.size(); i++) {
                Elpris pris = priser.get(i);
                System.out.println(pris.timeStart().toLocalTime() + " - " + pris.sekPerKWh() + " sek/KWh");
            }
        } else if (args.length == 2 && args[0].equals("--zone") && args[1].equals("SE4")) {

            List<Elpris> priser = api.getPriser(idag, Prisklass.SE4);

            System.out.println("Priserna för zon 4:\n");

            for (int i = 0; i < priser.size(); i++) {
                Elpris pris = priser.get(i);
                System.out.println(pris.timeStart().toLocalTime() + " - " + pris.sekPerKWh() + " sek/KWh");
            }
        }
    }
}