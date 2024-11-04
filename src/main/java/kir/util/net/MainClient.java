package kir.util.net;

import kir.util.Printer;
import kir.util.StickyFinger;

import java.io.File;
import java.time.Duration;
import java.time.Instant;

public class MainClient {
    public static void main(String[] args) {
        var term = new TCPTerminal();
        term.init();
    }
}

// TODO: Handle wrong hostname/password crash