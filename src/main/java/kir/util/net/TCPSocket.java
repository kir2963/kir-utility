package kir.util.net;

import java.net.InetAddress;
import java.net.Socket;

public final class TCPSocket extends NetSocket<Socket> {

    private final Socket socket;

    public TCPSocket(Socket socket, int buffer) {
        if (socket == null || buffer < 0) {
            throw new IllegalArgumentException("Invalid socket or buffer size");
        }
        this.socket = socket;
        this.buffer = buffer;
        this.clientAddress = socket.getInetAddress();
        this.clientPort = socket.getPort();
    }

    @Override
    public InetAddress getClientAddress() {
        return this.clientAddress;
    }

    @Override
    public InetAddress getLocalAddress() {
        return this.socket.getLocalAddress();
    }

    @Override
    public int getClientPort() {
        return this.clientPort;
    }

    @Override
    public int getLocalPort() {
        return this.socket.getLocalPort();
    }

    @Override
    public int getBufferSize() {
        return this.buffer;
    }

    @Override
    public boolean isClosed() {
        return this.socket.isClosed();
    }

    @Override
    public Socket getSocket() {
        return this.socket;
    }

}
