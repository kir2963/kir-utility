package kir.util.net;

import kir.util.ConsoleColors;
import kir.util.Printer;

import java.util.Arrays;
import java.util.Scanner;

public final class TCPTerminal {

    private final Scanner scanner = new Scanner(System.in);
    private TCPClient client;

    public void init() {
        while (true) {
            while (!promptConnect()) {}
            Printer.printfc("Connected to %s%n", ConsoleColors.GREEN, client.getHostName());
            while (true) {
                Printer.print("> "); var inp = scanner.nextLine();
                if (inp.isEmpty()) continue;
                if (inp.equalsIgnoreCase("dc")) {
                    client.sendCmd("dc");
                    break;
                }
                var resp = client.sendw(inp);
                Printer.println(resp);
            }
        }
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
            if (!client.sendw(new String(password)).equalsIgnoreCase("OK")) {
                Printer.error("Invalid credentials!");
                scanner.nextLine(); // Ignore empty string
                client.close();
                return false;
            }
            scanner.nextLine(); // Ignore empty string
        }
        return true;
    }

}
