package kir.util.net;

import kir.util.Printer;

import java.io.Console;
import java.net.Socket;
import java.util.Arrays;
import java.util.Scanner;

public final class TCPTerminal {

    private final Scanner scanner = new Scanner(System.in);
    private TCPClient client;

    public void init() {

    }

    private boolean promptConnect() {
        Printer.print("Host: "); var host = scanner.nextLine();
        Printer.print("Port: "); var port = scanner.nextInt();
        client = new TCPClient();
        client.connect(host, port);
        if (client.recvResponse().equals("auth")) {
            var console = System.console();
            Printer.println("This host require a password.");
            var password = console.readPassword("Password: ");
            if (!client.sendw(Arrays.toString(password)).equalsIgnoreCase("OK")) {
                Printer.error("Invalid credentials!");
                client.close();
                return false;
            }
        }
        return true;
    }

}
