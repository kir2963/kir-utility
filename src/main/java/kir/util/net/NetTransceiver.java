package kir.util.net;

import kir.util.Printer;
import kir.util.StickyFinger;
import lombok.Getter;
import lombok.Setter;

import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public final class NetTransceiver implements Closeable {

    private final String CREATOR = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getSimpleName();

    @Getter
    private final SocketMode mode;
    @Getter @Setter
    private int bufSize;

    // TCP Socket Stream
    private DataInputStream dis;
    private DataOutputStream dos;

    // UDP Socket
    private DatagramSocket ds;
    private DatagramPacket inPkt;
    private DatagramPacket outPkt;

    public NetTransceiver(NetSocket<?> socket, SocketMode mode) throws IOException {
        this(socket, mode, 2048);
    }

    public NetTransceiver(NetSocket<?> socket, SocketMode mode, int bufSize) throws IOException {
        if (bufSize <= 0) throw new IllegalArgumentException("Buffer size must be greater than 0");
        this.mode = mode;
        this.bufSize = bufSize;
        this.init(socket);
    }

    private void init(NetSocket<?> socket) throws IOException {
        if (socket instanceof TCPSocket s && mode == SocketMode.TCP) {
            this.dis = new DataInputStream(s.getSocket().getInputStream());
            this.dos = new DataOutputStream(s.getSocket().getOutputStream());
        } else if (socket instanceof UDPSocket s && mode == SocketMode.UDP) {
            this.ds = s.getSocket();
            this.inPkt = new DatagramPacket(new byte[bufSize], bufSize);
            this.outPkt = new DatagramPacket(new byte[bufSize], bufSize, s.clientAddress, s.clientPort);
        } else throw new IllegalArgumentException("Unsupported socket type or socket and mode mismatch");

    }

    public void send(String str) throws IOException {
        switch (mode) {
            case TCP -> {
                dos.writeUTF(str);
                dos.flush();
            }
            case UDP -> {
                var b = str.getBytes();
                // If message length exceed buffer size then the exceeding part will be removed
                if (b.length > bufSize) b = Arrays.copyOf(b, bufSize);
                outPkt.setData(b);
                ds.send(outPkt);
                outPkt.setData(new byte[bufSize]);
            }
            case MULTICAST -> throw new UnsupportedOperationException("Not implemented");
        }
    }

    public String receive() throws IOException {
        switch (mode) {
            case TCP -> {
                return dis.readUTF();
            }
            case UDP -> {
                ds.receive(inPkt);
                var receivedStr = new String(inPkt.getData()).trim();
                inPkt.setData(new byte[bufSize]);
                return receivedStr;
            }
            case MULTICAST -> throw new UnsupportedOperationException("Not implemented");
        }
        return "";
    }

    public void sendFile(File file) throws IOException {
        sendFile(file, false);
    }

    public void sendFile(File file, boolean compressed) throws IOException {
        if (mode != SocketMode.TCP)
            throw new UnsupportedOperationException("Current mode does not support file transfer");

        var fileLength = file.length();

        dos.writeBoolean(compressed);
        dos.writeUTF(file.getName());
        dos.writeLong(fileLength);

        int data;
        long bytesWritten = 0;
        byte[] buffer = new byte[bufSize];
        var fis = new BufferedInputStream(new FileInputStream(file));
        while ((data = fis.read(buffer)) != -1) {
            dos.write(buffer, 0, data);
            bytesWritten += data;
            if (CREATOR.equals("NetClient")) Printer.progress(bytesWritten, fileLength);
        }

        dos.flush();
        fis.close();
    }

    public Path receiveFile(Path outputPath) throws IOException {
        if (mode != SocketMode.TCP)
            throw new UnsupportedOperationException("Current mode does not support file transfer");

        var compressed = dis.readBoolean();
        var fileName = dis.readUTF();
        var fileLength = dis.readLong();
        var outPath = outputPath.resolve(fileName);

        int data;
        long bytesRead = 0;
        byte[] buffer = new byte[bufSize];
        var fos = new BufferedOutputStream(new FileOutputStream(outPath.toFile()));
        while (bytesRead < fileLength && (data = dis.read(buffer, 0, (int) Math.min(buffer.length, fileLength - bytesRead))) != -1) {
            fos.write(buffer, 0, data);
            bytesRead += data;
            if (CREATOR.equals("NetClient")) Printer.progress(bytesRead, fileLength);
        }
        fos.flush();
        fos.close();

        if (compressed) {
            StickyFinger.unzip(outPath, outputPath);
            Files.delete(outPath);
            return outPath.getParent();
        }

        return outPath;
    }

    @Override
    public void close() {
        // UDP
        if (ds != null) ds.close();

        // TCP
        if (dis != null) try { dis.close(); } catch (IOException ignored) {}
        if (dos != null) try { dos.close(); } catch (IOException ignored) {}
    }

}
