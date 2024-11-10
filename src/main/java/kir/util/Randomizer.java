package kir.util;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Random;

public final class Randomizer {

    private static final int MIN_PORT = 1;
    private static final int MAX_PORT = 65535;

    public static int randomAvailablePort() {
        var random = new Random();
        var randomPort = random.nextInt(MIN_PORT, MAX_PORT);
        while (!isPortAvailable(randomPort)) randomPort = random.nextInt(MIN_PORT, MAX_PORT);
        return randomPort;
    }

    private static boolean isPortAvailable(int port) {
        ServerSocket serverSocket = null;
        DatagramSocket datagramSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            datagramSocket = new DatagramSocket(port);
            datagramSocket.setReuseAddress(true);
            return true;
        } catch (IOException ignored) {
        } finally {
            if (datagramSocket != null) datagramSocket.close();
            if (serverSocket != null) try {
                serverSocket.close();
            } catch (IOException ignored) {}
        }
        return false;
    }

}
