package kir.util.net;

public class MainServer {
    public static void main(String[] args) {
        var server = new TCPServer(8386, "admin", true);
        server.listen();
    }
}
