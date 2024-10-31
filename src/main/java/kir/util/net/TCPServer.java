package kir.util.net;

import kir.util.ConsoleColors;
import kir.util.Printer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GuardedObject;
import java.util.Arrays;
import java.util.PropertyPermission;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class TCPServer implements AutoCloseable {

    private final int port;
    private final ExecutorService executor;
    private final boolean sharing;
    private final byte[] secret;

    public TCPServer(int port, boolean sharing) {
        this(port, null, sharing);
    }

    public TCPServer(int port, String password, boolean sharing) {
        this.port = port;
        this.executor = Executors.newCachedThreadPool();
        this.sharing = sharing;
        this.secret = password.getBytes();
    }

    public void listen() {
        Printer.printfc("Listening on port %d...%n", ConsoleColors.GREEN, port);
        try (var ss = new ServerSocket(port)) {
            while (true) {
                var client = ss.accept();
                handleClient(client, new CommandHandler(client, sharing));
                //executor.submit(() -> handleClient(client, new CommandHandler(client, sharing)));
            }
        } catch (IOException e) {
            Printer.error("Error starting server.");
            Printer.error(e.getMessage());
            e.printStackTrace();
            close();
        }
    }
    public void listen(CommandHandler commandHandler) {
        Printer.printfc("Listening on port %d...%n", ConsoleColors.GREEN, port);
        try (var ss = new ServerSocket(port)) {
            while (true) {
                var client = ss.accept();
                handleClient(client, commandHandler);
                // executor.submit(() -> handleClient(client, commandHandler));
            }
        } catch (IOException e) {
            Printer.error("Error starting server.");
            Printer.error(e.getMessage());
            e.printStackTrace();
            close();
        }
    }

    private void handleClient(Socket client, CommandHandler commandHandler) {
        Printer.printfc("[%s] Connected.%n", ConsoleColors.CYAN, client.getRemoteSocketAddress().toString());
        var postman = new Postman(client);
        try {
            if (secret != null) {
                postman.sendMsg("auth");
                var resp = postman.recvMsg();
                if (!Arrays.equals(resp.getBytes(), secret)) {
                    postman.sendMsg("Invalid credential.");
                    Printer.printfc("[%s] Disconnected.%n", ConsoleColors.CYAN, client.getRemoteSocketAddress().toString());
                    return;
                } else {
                    postman.sendMsg("OK");
                }
            }
            while (!client.isClosed()) {
                var msg = postman.recvMsg();
                var cmdArr = msg.split(" ");
                var args = cmdArr.length > 1 ? Arrays.copyOfRange(cmdArr, 1, cmdArr.length - 1) : new Object();
                commandHandler.handle(cmdArr[0], (Object) args);
            }
            Printer.printfc("[%s] Disconnected.%n", ConsoleColors.CYAN, client.getRemoteSocketAddress().toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }

}
