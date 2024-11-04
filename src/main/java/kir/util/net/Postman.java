package kir.util.net;

import kir.util.Printer;
import kir.util.StickyFinger;
import lombok.SneakyThrows;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

public final class Postman {

    private static final int CHUNK_SIZE = 8192;
    private static final String CREATOR = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass().getSimpleName();

    private final DataOutputStream dos;
    private final DataInputStream dis;

    @SneakyThrows
    public Postman(Socket client) {
        dos = new DataOutputStream(client.getOutputStream());
        dis = new DataInputStream(client.getInputStream());
    }

    public void sendMsg(String msg) throws IOException {
        dos.writeUTF(msg);
        dos.flush();
    }

    public void sendFile(File file) throws IOException {
        sendFile(file, false);
    }

    public void sendFile(File file, boolean compressed) throws IOException {
        var buffer = new byte[CHUNK_SIZE];
        var fileLength = file.length();

        if (compressed) dos.writeBoolean(true);
        dos.writeUTF(file.getName());
        dos.writeLong(fileLength);

        int bytes;
        long bytesWritten = 0;
        var fis = new BufferedInputStream(new FileInputStream(file));
        while ((bytes = fis.read(buffer)) != -1) {
            dos.write(buffer, 0, bytes);
            bytesWritten += bytes;
            if (CREATOR.equals("TCPClient")) Printer.progress(bytesWritten, fileLength);
        }
        dos.flush();
        fis.close();
    }

    public void sendDir(File dir) throws IOException {
        var tmpName = UUID.randomUUID() + ".zip";
        var target = StickyFinger.zip("./cache/" + tmpName, dir.listFiles());
        sendFile(target, true);
    }

    public String recvMsg() throws IOException {
        return dis.readUTF();
    }

    public File recvFile(File outputDir) throws IOException {
        var buffer = new byte[CHUNK_SIZE];

        var compressed = dis.readBoolean();
        var fileName = dis.readUTF();
        var fileLength = dis.readLong();
        var outPath = new File(outputDir, fileName);

        var fos = new BufferedOutputStream(new FileOutputStream(outPath));
        int bytes, bytesRead = 0;
        while (bytesRead < fileLength && (bytes = dis.read(buffer, 0, (int) Math.min(buffer.length, fileLength - bytesRead))) != -1) {
            fos.write(buffer, 0, bytes);
            bytesRead += bytes;
            if (CREATOR.equals("TCPClient")) Printer.progress(bytesRead, fileLength);
        }
        fos.flush();
        fos.close();

        if (compressed) {
            StickyFinger.unzip(outPath, outputDir);
            outPath.delete();
            return new File(outPath.getParent());
        }

        return outPath;
    }

//    public void recvDir(File outputDir) throws IOException {
//        var zip = recvFile(outputDir);
//        StickyFinger.unzip(zip, outputDir);
//        zip.delete();
//    }

}
