package kir.util.net;

import at.favre.lib.crypto.bcrypt.BCrypt;
import kir.util.Printer;

import java.io.Closeable;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.InputMismatchException;
import java.util.Scanner;

public class ClientTerminal implements Closeable {
    private final Scanner sc;
    private final SocketMode mode;
    private NetClient client;

    public ClientTerminal(SocketMode mode) {
        sc = new Scanner(System.in);
        this.mode = mode;
    }

    public void init() throws IOException {
        while (true) {
            while (!connect());
            Printer.success("Connected");
            while (true) {
                Printer.print("> "); var input = sc.nextLine();
                if (input.isEmpty()) continue;
                if (input.equalsIgnoreCase("dc")) {
                    client.send("dc");
                    break;
                }
                var resp = client.sendw(input.trim());
                if (!resp.isEmpty()) Printer.println(resp);
            }
        }
    }

    private boolean connect() throws IOException {
        Printer.print("Endpoint: "); var endpoint = sc.nextLine();
        Printer.print("Port: "); int port;
        try { port = sc.nextInt(); }
        catch (InputMismatchException e) { Printer.error("Invalid port number"); return false; }
        sc.nextLine();
        NetClient client;
        try {
            client = authenticate(endpoint, port);
        } catch (UnknownHostException e) {
            Printer.error("Unknown host: " + endpoint);
            return false;
        }

        if (client == null) {
            Printer.println("Invalid credential");
            return false;
        }
        this.client = client;
        return true;
    }

    private NetClient authenticate(String endpoint, int port) throws IOException {
        var client = new NetClient(mode, endpoint, port);
        if (mode == SocketMode.TCP) {
            var resp = client.receive();

            if (resp.equalsIgnoreCase("auth")) {
                Printer.warning("This endpoint require a password!");
                Printer.print("Password: "); var pwd = sc.nextLine();
                var hash = BCrypt.withDefaults().hashToString(12, pwd.trim().toCharArray());
                resp = client.sendw(hash);
            }

            if (!resp.equalsIgnoreCase("authenticated")) {
                return null;
            }

            var serverBufSize = client.receive().trim();
            Printer.println(serverBufSize);
            client.setBufferSize(Integer.parseInt(serverBufSize));
        }
        return client;
    }

    @Override
    public void close() {
        client.close();
        sc.close();
    }

}
