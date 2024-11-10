package kir.util.net;

import java.net.InetAddress;

public abstract class NetSocket<T> {

    protected int buffer;
    protected InetAddress clientAddress;
    protected int clientPort;

    public abstract InetAddress getClientAddress();
    public abstract InetAddress getLocalAddress();
    public abstract int getClientPort();
    public abstract int getLocalPort();
    public abstract int getBufferSize();
    public abstract boolean isClosed();
    public abstract T getSocket();

}
