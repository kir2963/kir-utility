package kir.util.net;

import kir.util.CommandParser;
import kir.util.Printer;
import kir.util.StickyFinger;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class NetClient implements Closeable {

    private final NetTransceiver transceiver;
    private final Path receivedPath;

    public NetClient(SocketMode mode, String endpoint, int port) throws IOException {
        int bufSize = 2048; // Default buffer size before negotiation
        receivedPath = Path.of("received");
        verifyReceivedDirectory();
        switch (mode) {
            case TCP: {
                var tcpSocket = new TCPSocket(new Socket(endpoint, port), bufSize);
                this.transceiver = new NetTransceiver(tcpSocket, mode, bufSize);
                break;
            }
            case UDP: {
                var socket = UDPSocket.locate(endpoint, port, bufSize);
                this.transceiver = new NetTransceiver(socket, mode, socket.getBufferSize());
                break;
            }
            case MULTICAST:
            default: throw new UnsupportedOperationException("Not implemented");
        }
    }

    public void setBufferSize(int bufSize) {
        this.transceiver.setBufferSize(bufSize);
    }

    public String sendw(String str) throws IOException {
        if (!send(str)) return "";
        return transceiver.receive();
    }

    public boolean send(String str) throws IOException {
        var cmdArr = CommandParser.parse(str);
        var cmd = cmdArr.getKey();

        if (cmd.equalsIgnoreCase("up") || cmd.equalsIgnoreCase("cp")) {
            Printer.printf("Current mode %s does not support file operations.", transceiver.getSocketMode());
            return false;
        }

        if (cmdArr.getKey().equalsIgnoreCase("up")) {
            if (cmdArr.getValue().stream().map(Path::of).anyMatch(p -> !Files.exists(p))) {
                Printer.error("Some files does not exist.");
                return false;
            }
            transceiver.send(cmd);
            sendFiles(cmdArr.getValue().stream().map(Path::of).toArray(Path[]::new));
            Printer.success("File sent.");
            Printer.warning("Waiting for server to decompress...");
            return true;
        }

        if (cmdArr.getKey().equalsIgnoreCase("cp")) {
            transceiver.send(str);
            if (!Files.exists(receivedPath)) Files.createDirectories(receivedPath);
            var readiness = transceiver.receive();
            if (!readiness.equalsIgnoreCase("ok")) {
                Printer.error("Some files do not exist.");
                return false;
            }
            var output = transceiver.receiveFile(receivedPath);
            Printer.success("File saved to " + output.toAbsolutePath());
            return false;
        }

        transceiver.send(str);
        return true;
    }

    public void sendFiles(Path... files) throws IOException {
        if (files.length != 1 || Files.isDirectory(files[0])) {
            Printer.warning("Directory or multiple files detected. Compressing...");
            var zipName = "cache/" + UUID.randomUUID() + ".zip";
            var zipPath = StickyFinger.zip(zipName, files);
            transceiver.sendFile(zipPath.toFile(), true);
            return;
        }
        transceiver.sendFile(files[0].toFile(), false);
    }

    public String receive() throws IOException {
        return transceiver.receive();
    }

    @Override
    public void close() {
        transceiver.close();
    }

    private void verifyReceivedDirectory() throws IOException {
        if (Files.exists(receivedPath)) Files.createDirectories(receivedPath);
    }

}
