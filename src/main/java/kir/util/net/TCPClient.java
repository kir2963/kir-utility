package kir.util.net;

import kir.util.CommandParser;
import kir.util.Printer;
import kir.util.StickyFinger;
import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public final class TCPClient {

    private Socket socket;
    private Postman postman;

    private static final File SHARE_DIRECTORY = new File("received/");

    public String getHostName() {
        return socket != null ? socket.getRemoteSocketAddress().toString() : null;
    }

    public boolean connect(String host, int port) {
        if (socket != null) close();
        try {
            socket = new Socket(host, port);
            postman = new Postman(socket);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @SneakyThrows
    public boolean sendCmd(String cmd) {
        if (socket == null) throw new RuntimeException("Not connected to any host");
        var sendfCmdArr = CommandParser.parse(cmd);

        if (sendfCmdArr.getKey().equalsIgnoreCase("up")) {
            if (sendfCmdArr.getValue().stream().map(File::new).anyMatch(f -> !f.exists())) {
                Printer.error("Some files does not exist.");
                return false;
            }
            postman.sendMsg(cmd);
            if (sendfCmdArr.getValue().size() != 1) sendFiles(sendfCmdArr.getValue());
            else sendFile(sendfCmdArr.getValue().get(0));
            Printer.success("File sent.");
            return true;
        }

        if (sendfCmdArr.getKey().equalsIgnoreCase("cp")) {
            postman.sendMsg(cmd);
            recvFile();
            Printer.success("File saved to " + SHARE_DIRECTORY.getAbsolutePath());
            return false;
        }

        postman.sendMsg(cmd);
        return true;
    }

    @SneakyThrows
    private void sendFile(String path) {
        var file = new File(path);
        if (file.isDirectory()) postman.sendDir(file);
        else postman.sendFile(file);
    }

    @SneakyThrows
    private void sendFiles(List<String> files) {
        var src = files.stream().map(File::new).toArray(File[]::new);
        Files.createDirectories(Paths.get("./cache"));
        var tmpName = "./cache/" + UUID.randomUUID() + ".zip";
        postman.sendFile(StickyFinger.zip(tmpName, src), true);
    }

    private void recvFile() {
        if (!SHARE_DIRECTORY.exists()) SHARE_DIRECTORY.mkdirs();
        try {
            postman.recvFile(SHARE_DIRECTORY);
        } catch (IOException e) {
            Printer.error(e.getMessage());
            e.printStackTrace();
        }
    }


    @SneakyThrows
    public String sendw(String cmd) {
        if (sendCmd(cmd)) return recvResponse();
        return "";
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
