package kir.util;

import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;

public final class Verifier {

    @SneakyThrows
    public static String genChecksum(File target) throws IOException {
        var data = Files.readAllBytes(target.toPath());
        var hash = MessageDigest.getInstance("MD5").digest(data);
        return new BigInteger(1, hash).toString(16);
    }

    public static boolean verify(File target, String checksum) throws IOException {
        var targetChecksum = genChecksum(target);
        return targetChecksum.equals(checksum);
    }

}
