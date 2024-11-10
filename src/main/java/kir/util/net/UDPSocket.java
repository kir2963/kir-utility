package kir.util.net;

import kir.util.Printer;
import kir.util.Randomizer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public final class UDPSocket extends NetSocket<DatagramSocket> {

    private final DatagramSocket socket;

    private UDPSocket(DatagramSocket ds, InetAddress address, int port, int bufSize) {
        this.socket = ds;
        this.clientAddress = address;
        this.clientPort = port;
        this.buffer = bufSize;
    }
    private UDPSocket(DatagramSocket socket) {
        this.socket = socket;
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
    public DatagramSocket getSocket() {
        return this.socket;
    }

    public static UDPSocket negotiate(DatagramSocket ds, DatagramPacket locatePkt, int bufSize) throws IOException {
        var locatePktData = new String(locatePkt.getData()).trim();
        var negotiatePkt = new DatagramPacket(new byte[8], 8);

        try {
            if (!locatePktData.equalsIgnoreCase("locate")) return null;
            negotiatePkt.setData(String.valueOf(bufSize).getBytes());
            negotiatePkt.setAddress(locatePkt.getAddress());
            negotiatePkt.setPort(locatePkt.getPort());
            ds.send(negotiatePkt);
        } finally {
            locatePkt.setData(new byte[8]);
        }

        var servPort = Randomizer.randomAvailablePort();
        try {
            negotiatePkt.setData(String.valueOf(servPort).getBytes());
            ds.send(negotiatePkt);
        } finally {
            locatePkt.setData(new byte[8]);
        }

        var socket = new UDPSocket(new DatagramSocket(servPort));
        socket.clientAddress = locatePkt.getAddress();
        socket.clientPort = locatePkt.getPort();
        socket.buffer = bufSize;

        return socket;
    }

    public static UDPSocket locate(String endpoint, int port, int bufSize) throws IOException {
        var inPkt = new DatagramPacket(new byte[8], 8);
        var outPkt = new DatagramPacket(new byte[8], 8, InetAddress.getByName(endpoint), port);

        var ds = new DatagramSocket();

        outPkt.setData("locate".getBytes());
        ds.send(outPkt);
        ds.receive(inPkt);

        var serverBuf = Integer.parseInt(new String(inPkt.getData()).trim());
        if (bufSize > serverBuf || bufSize < serverBuf) {
            Printer.warning("[SYSTEM/UDP] Server is operating with different buffer size. Readjusting...");
            bufSize = serverBuf;
        }

        inPkt.setData(new byte[8]);
        ds.receive(inPkt);
        var serverPort = Integer.parseInt(new String(inPkt.getData()).trim());

        Printer.warning("[SYSTEM/UDP] Negotiated on port " + serverPort);
        return new UDPSocket(ds, inPkt.getAddress(), serverPort, bufSize);
    }

}
