package kir.util.net;

import kir.util.ConsoleColors;
import kir.util.Printer;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.net.Socket;

public final class TCPClient {

    private Socket socket;
    private Postman postman;

    public boolean connect(String host, int port) {
        if (socket != null) close();
        try {
            socket = new Socket(host, port);
            postman = new Postman(socket);
            Printer.printfc("Connected to %s on port %d.%n", ConsoleColors.GREEN, host, port);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @SneakyThrows
    public void sendCmd(String cmd) {
        if (socket == null) throw new RuntimeException("Not connected to any host");
        var sendfCmdArr = cmd.split(" ");
        if (sendfCmdArr[0].equalsIgnoreCase("sendf")) sendFile(sendfCmdArr[1]);
        postman.sendMsg(cmd);
    }

    @SneakyThrows
    private void sendFile(String path) throws IOException {
        var file = new File(path);
        if (file.exists()) {
            if (file.isDirectory()) postman.sendDir(file);
            else postman.sendFile(file);
        }
    }

    @SneakyThrows
    public String sendw(String cmd) {
        sendCmd(cmd);
        return recvResponse();
    }

    @SneakyThrows
    public String recvResponse() {
        if (socket == null) throw new RuntimeException("Not connected to any host");
        return postman.recvMsg();
    }

    @SneakyThrows
    public void close() {
        if (socket == null) throw new RuntimeException("Not connected to any host");
        postman.sendMsg("dc");
        Printer.info("Disconnected from " + socket.getRemoteSocketAddress());
    }

}
