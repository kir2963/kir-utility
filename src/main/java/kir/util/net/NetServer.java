package kir.util.net;

import at.favre.lib.crypto.bcrypt.BCrypt;
import kir.util.ConsoleColors;
import kir.util.Printer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NetServer implements AutoCloseable {

    private final SocketMode mode;
    private final int port;
    private final int bufSize;
    private final ExecutorService executor;
    private char[] pwh;

    public NetServer(SocketMode mode, int port, int bufSize) {
        this.mode = mode;
        this.port = port;
        this.bufSize = bufSize;
        this.executor = Executors.newCachedThreadPool();
        this.pwh = null;
    }

    public void setCredential(String password) {
        if (mode != SocketMode.TCP) {
            Printer.warning("Password server only support TCP socket. Action ignored");
            return;
        }
        this.pwh = password.trim().toCharArray();
    }

    public void listen() {
        try {
            switch (mode) {
                case TCP: listenTcp(CommandHandler.class); break;
                case UDP: listenUdp(CommandHandler.class); break;
                case MULTICAST: throw new UnsupportedOperationException("Not implemented");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error starting server", e);
        }
    }

    public void listen(Class<? extends CommandHandler> cmdHandler) {
        try {
            switch (mode) {
                case TCP: listenTcp(cmdHandler); break;
                case UDP: listenUdp(cmdHandler); break;
                case MULTICAST: throw new UnsupportedOperationException("Not implemented");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error starting server", e);
        }
    }

    private void listenTcp(Class<? extends CommandHandler> handler) throws IOException {
        try (var ss = new ServerSocket(port)) {
            Printer.printfc("[SYSTEM/TCP] Listening on port %d...%n", ConsoleColors.GREEN, port);
            while (true) {
                var client = ss.accept();
                executor.submit(() -> handleClient(new TCPSocket(client, bufSize), handler));
            }
        }
    }

    private void listenUdp(Class<? extends CommandHandler> handler) throws IOException {
        try (var ds = new DatagramSocket(port)) {
            Printer.printfc("[SYSTEM/UDP] Listening on port %d...%n", ConsoleColors.GREEN, port);
            var locatePkt = new DatagramPacket(new byte[8], 8);   // Server will listen for locate packet// Then send the current buffer size for client to readjust before continue
            while (true) {
                ds.receive(locatePkt);
                var socket = UDPSocket.negotiate(ds, locatePkt, bufSize);
                if (socket == null) continue;
                handleClient(socket, handler);
                executor.submit(() -> handleClient(socket, handler));
            }
        }
    }

//    private <T> void handleClient(NetSocket<T> socket) {
//        try {
//            var transceiver = new NetTransceiver(socket, mode, bufSize);
//            var handler = new CommandHandler(transceiver);
//            handleClient(socket, transceiver, handler);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//    }

    private <T> void handleClient(NetSocket<T> socket, Class<? extends CommandHandler> clazz) {
        try {
            var transceiver = new NetTransceiver(socket, mode, bufSize);
            var handler = getHandler(clazz, transceiver);
            handleClient(socket, transceiver, handler);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> void handleClient(NetSocket<T> socket, NetTransceiver transceiver, CommandHandler handler) throws IOException {
        var clientAddr = socket.getClientAddress();
        if (!authenticate(transceiver)) return;
        if (mode == SocketMode.TCP) transceiver.send(String.valueOf(bufSize));
        Printer.printfc("[SYSTEM/%s] (%s) Connected%n", ConsoleColors.CYAN, mode.toString(), clientAddr);
        try {
            while (!socket.isClosed()) {
                var cmd = transceiver.receive();
                handler.handle(cmd);
            }
        } catch (IOException ignored) {
        } finally {
            Printer.printfc("[SYSTEM/%s] (%s) Disconnected%n", ConsoleColors.CYAN, mode.toString(), clientAddr);
        }
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    private CommandHandler getHandler(Class<? extends CommandHandler> clazz, NetTransceiver transceiver) {
        if (clazz.getSuperclass() != CommandHandler.class) return new CommandHandler(transceiver);
        try {
            var handler = clazz.getDeclaredConstructor().newInstance();
            var f = handler.getClass().getSuperclass().getDeclaredField("transceiver");
            f.setAccessible(true);
            f.set(handler, transceiver);
            return handler;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("No parameterless public constructor found");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean authenticate(NetTransceiver transceiver) throws IOException {
        if (this.pwh != null) {
            transceiver.send("auth");
            var ch = transceiver.receive().trim();
            if (!BCrypt.verifyer().verify(this.pwh, ch).verified) {
                transceiver.send("Invalid credential");
                transceiver.close();
                return false;
            }
        }
        if (mode == SocketMode.TCP) transceiver.send("authenticated");
        return true;
    }

}
