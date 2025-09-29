package com.example;

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

        if (args.length == 0 || args[0].equals("--help")) {
            help();
        } else if (args.length > 0 && args[0].equals("--zone")) {
            System.out.println("Du valde '--zone'");
        }
    }
}