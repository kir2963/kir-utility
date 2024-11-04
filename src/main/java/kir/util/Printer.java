package kir.util;

public final class Printer {

    public static String format(String fStr, Object... args) {
        return String.format(fStr, args);
    }
    public static String formatc(String fStr, String colorCode, Object... args) {
        return String.format("%s%s%s", colorCode, String.format(fStr, args), ConsoleColors.RESET);
    }

    public static void print(String str) {
        System.out.print(str);
    }
    public static void println(String str) {
        System.out.println(str);
    }
    public static void printf(String fStr, Object... args) {
        System.out.printf(fStr, args);
    }
    public static void printfc(String fStr, String colorCode, Object... args) {
        System.out.printf("%s%s%s", colorCode, String.format(fStr, args), ConsoleColors.RESET);
    }

    public static void info(String str) {
        System.out.printf("%s%s%s%n", ConsoleColors.CYAN, str, ConsoleColors.RESET);
    }
    public static void info(String str, boolean newline) {
        System.out.printf("%s%s%s" + (newline ? "%n" : ""),
                ConsoleColors.CYAN, str, ConsoleColors.RESET);
    }

    public static void success(String str) {
        System.out.printf("%s%s%s%n", ConsoleColors.GREEN, str, ConsoleColors.RESET);
    }
    public static void success(String str, boolean newline) {
        System.out.printf("%s%s%s" + (newline ? "%n" : ""),
                ConsoleColors.GREEN, str, ConsoleColors.RESET);
    }

    public static void warning(String str) {
        System.out.printf("%s%s%s%n", ConsoleColors.YELLOW, str, ConsoleColors.RESET);
    }
    public static void warning(String str, boolean newline) {
        System.out.printf("%s%s%s" + (newline ? "%n" : ""),
                ConsoleColors.YELLOW, str, ConsoleColors.RESET);
    }

    public static void error(String str) {
        System.out.printf("%s%s%s%n", ConsoleColors.RED, str, ConsoleColors.RESET);
    }
    public static void error(String str, boolean newline) {
        System.out.printf("%s%s%s" + (newline ? "%n" : ""),
                ConsoleColors.RED, str, ConsoleColors.RESET);
    }

    public static void progress(double current, double max) {
        if (current == max) {
            printf("\r[DONE]%-50s%n", "");
            return;
        }
        var currentPercentage = Math.floor(current / max * 50);
        var strBuilder = new StringBuilder();
        for (var i = 0; i <= currentPercentage; i++) {
            if (i == currentPercentage) {
                strBuilder.append(">");
                continue;
            }
            strBuilder.append("=");
        }
        printfc("\r[%-50s]", ConsoleColors.BLUE, strBuilder.toString());
    }

}
