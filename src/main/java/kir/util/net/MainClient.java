package kir.util.net;

import kir.util.Printer;

public class MainClient {
    public static void main(String[] args) {
        var client = new TCPClient();
        client.connect("localhost", 8386);
        if (client.recvResponse().equals("auth")) {
            client.sendCmd("admin");
        }
        client.sendCmd("1");
        Printer.println(client.recvResponse());
    }
}
