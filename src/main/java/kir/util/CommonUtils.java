package kir.util;

import java.io.IOException;

public final class CommonUtils {

    public static void clearScreen() throws IOException {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            Runtime.getRuntime().exec("cls");
        } else {
            Runtime.getRuntime().exec("clear");
        }
    }

}
