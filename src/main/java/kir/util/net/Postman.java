package kir.util.net;

import kir.util.StickyFinger;

import java.io.*;
import java.net.Socket;

public final class Postman {

    private static final int CHUNK_SIZE = 4096;

    private final Socket client;

    public Postman(Socket client) {
        this.client = client;
    }

    public void sendMsg(String msg) throws IOException {
        var dos = new DataOutputStream(client.getOutputStream());
        dos.writeUTF(msg);
        dos.flush();
    }
    public void sendFile(File file) throws IOException {
        var dos = new DataOutputStream(client.getOutputStream());
        var bif = new BufferedInputStream(new FileInputStream(file));

        var buffer = new byte[CHUNK_SIZE];
        int bytesRead;

        dos.writeUTF(file.getName());
        while ((bytesRead = bif.read(buffer)) != -1) {
            dos.write(buffer, 0, bytesRead);
        }
        dos.flush();

        bif.close();
        dos.close();
    }
    public void sendDir(File dir) throws IOException {
        var target = StickyFinger.zip(dir, "tmp.zip", true);
        sendFile(target);
    }

    public String recvMsg() throws IOException {
        var dis = new DataInputStream(client.getInputStream());
        return dis.readUTF();
    }
    public File recvFile(File outputDir) throws IOException {
        var dis = new DataInputStream(client.getInputStream());

        var buffer = new byte[CHUNK_SIZE];
        int bytesRead;

        var fileName = dis.readUTF();
        var outPath = new File(outputDir, fileName);
        var bos = new BufferedOutputStream(new FileOutputStream(outPath));
        while ((bytesRead = dis.read(buffer)) != -1) {
            bos.write(buffer, 0, bytesRead);
        }

        bos.close();
        dis.close();

        return outPath;
    }
    public void recvDir(File outputDir) throws IOException {
        var zip = recvFile(outputDir);
        StickyFinger.unzip(zip, outputDir);
        zip.delete();
    }

}
